package com.xxxlog.server.es;

import com.xxxlog.common.enums.IndexSplitType;
import com.xxxlog.common.model.LogRecord;
import com.xxxlog.server.config.ServerProperties;
import org.springframework.stereotype.Component;

import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class IndexNameResolver {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ServerProperties properties;

    public IndexNameResolver(ServerProperties properties) {
        this.properties = properties;
    }

    public String resolve(LogRecord record) {
        String appName = sanitizeAppName(record.getAppName());
        String timeSuffix = formatTime(record.getTimestamp(), properties.getIndexSplitType());
        return buildIndexName(appName, timeSuffix);
    }

    public String buildIndexName(String appName, String timeSuffix) {
        return properties.getIndexPrefix() + "-" + appName + "-" + timeSuffix;
    }

    public String sanitizeAppName(String name) {
        if (name == null || name.isBlank()) {
            return "unknown";
        }
        return name.toLowerCase().replaceAll("[^a-z0-9_-]", "-");
    }

    /**
     * 按天（或按月）生成索引模式，HOUR 拆分时使用日级前缀 + *。
     */
    public List<String> enumerateSearchIndexPatterns(String appName, long startTime, long endTime) {
        IndexSplitType splitType = properties.getIndexSplitType();
        ZoneId zone = ZoneId.systemDefault();
        List<String> patterns = new ArrayList<>();

        if (splitType == IndexSplitType.MONTH) {
            ZonedDateTime cursor = truncateToBucket(Instant.ofEpochMilli(startTime).atZone(zone), splitType);
            ZonedDateTime end = Instant.ofEpochMilli(endTime).atZone(zone);
            while (!cursor.isAfter(end)) {
                patterns.add(buildSearchIndexPattern(appName, cursor, splitType));
                cursor = advanceBucket(cursor, splitType);
            }
            return patterns;
        }

        ZonedDateTime cursor = Instant.ofEpochMilli(startTime).atZone(zone).toLocalDate().atStartOfDay(zone);
        ZonedDateTime end = Instant.ofEpochMilli(endTime).atZone(zone).toLocalDate().atStartOfDay(zone);
        while (!cursor.isAfter(end)) {
            patterns.add(buildSearchIndexPattern(appName, cursor, splitType));
            cursor = cursor.plusDays(1);
        }
        return patterns;
    }

    public String buildSearchIndexPattern(String appName, ZonedDateTime time, IndexSplitType splitType) {
        String appSegment = StringUtils.hasText(appName) ? sanitizeAppName(appName) : "*";
        String prefix = properties.getIndexPrefix() + "-" + appSegment + "-";
        return switch (splitType) {
            case HOUR -> prefix + DAY_FORMAT.format(time) + "*";
            case DAY -> prefix + DAY_FORMAT.format(time);
            case MONTH -> prefix + MONTH_FORMAT.format(time) + "*";
        };
    }

    /**
     * 按配置的 index-split-type，枚举 [startTime, endTime] 覆盖的所有时间后缀。
     */
    public List<String> enumerateTimeSuffixes(long startTime, long endTime) {
        IndexSplitType splitType = properties.getIndexSplitType();
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime cursor = truncateToBucket(Instant.ofEpochMilli(startTime).atZone(zone), splitType);
        ZonedDateTime end = Instant.ofEpochMilli(endTime).atZone(zone);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(splitType.getPattern()).withZone(zone);

        List<String> suffixes = new ArrayList<>();
        while (!cursor.isAfter(end)) {
            suffixes.add(formatter.format(cursor.toInstant()));
            cursor = advanceBucket(cursor, splitType);
        }
        return suffixes;
    }

    /**
     * 从索引名解析应用名，兼容 HOUR / DAY / MONTH 三种后缀格式。
     */
    public String extractAppName(String indexName) {
        String prefix = properties.getIndexPrefix() + "-";
        if (indexName == null || !indexName.startsWith(prefix)) {
            return null;
        }
        String body = indexName.substring(prefix.length());
        if (body.matches(".+-\\d{4}-\\d{2}-\\d{2}-\\d{2}$")) {
            return body.replaceFirst("-\\d{4}-\\d{2}-\\d{2}-\\d{2}$", "");
        }
        if (body.matches(".+-\\d{4}-\\d{2}-\\d{2}$")) {
            return body.replaceFirst("-\\d{4}-\\d{2}-\\d{2}$", "");
        }
        if (body.matches(".+-\\d{4}-\\d{2}$")) {
            return body.replaceFirst("-\\d{4}-\\d{2}$", "");
        }
        return null;
    }

    /**
     * 从索引名解析时间桶起始时刻，用于过期判断。
     */
    public Optional<Instant> parseIndexStartInstant(String indexName) {
        if (indexName == null || !belongsToProject(indexName)) {
            return Optional.empty();
        }
        String prefixPattern = Pattern.quote(properties.getIndexPrefix() + "-");
        ZoneId zone = ZoneId.systemDefault();

        Matcher hourMatcher = Pattern.compile(prefixPattern + ".+-(\\d{4}-\\d{2}-\\d{2})-(\\d{2})$")
                .matcher(indexName);
        if (hourMatcher.matches()) {
            LocalDate date = LocalDate.parse(hourMatcher.group(1));
            int hour = Integer.parseInt(hourMatcher.group(2));
            return Optional.of(date.atTime(hour, 0).atZone(zone).toInstant());
        }
        Matcher dayMatcher = Pattern.compile(prefixPattern + ".+-(\\d{4}-\\d{2}-\\d{2})$").matcher(indexName);
        if (dayMatcher.matches()) {
            return Optional.of(LocalDate.parse(dayMatcher.group(1)).atStartOfDay(zone).toInstant());
        }
        Matcher monthMatcher = Pattern.compile(prefixPattern + ".+-(\\d{4}-\\d{2})$").matcher(indexName);
        if (monthMatcher.matches()) {
            YearMonth month = YearMonth.parse(monthMatcher.group(1));
            return Optional.of(month.atDay(1).atStartOfDay(zone).toInstant());
        }
        return Optional.empty();
    }

    public String extractIndexTimeSuffix(String indexName) {
        if (indexName == null || !belongsToProject(indexName)) {
            return null;
        }
        String prefixPattern = Pattern.quote(properties.getIndexPrefix() + "-");
        Matcher hourMatcher = Pattern.compile(prefixPattern + ".+-(\\d{4}-\\d{2}-\\d{2})-(\\d{2})$")
                .matcher(indexName);
        if (hourMatcher.matches()) {
            return hourMatcher.group(1) + "-" + hourMatcher.group(2);
        }
        Matcher dayMatcher = Pattern.compile(prefixPattern + ".+-(\\d{4}-\\d{2}-\\d{2})$").matcher(indexName);
        if (dayMatcher.matches()) {
            return dayMatcher.group(1);
        }
        Matcher monthMatcher = Pattern.compile(prefixPattern + ".+-(\\d{4}-\\d{2})$").matcher(indexName);
        if (monthMatcher.matches()) {
            return monthMatcher.group(1);
        }
        return null;
    }

    public boolean belongsToProject(String indexName) {
        String prefix = properties.getIndexPrefix() + "-";
        return indexName != null && indexName.startsWith(prefix);
    }

    private String formatTime(Long timestamp, IndexSplitType splitType) {
        long ts = timestamp != null ? timestamp : System.currentTimeMillis();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(splitType.getPattern())
                .withZone(ZoneId.systemDefault());
        return formatter.format(Instant.ofEpochMilli(ts));
    }

    private ZonedDateTime truncateToBucket(ZonedDateTime time, IndexSplitType splitType) {
        return switch (splitType) {
            case HOUR -> time.withMinute(0).withSecond(0).withNano(0);
            case DAY -> time.toLocalDate().atStartOfDay(time.getZone());
            case MONTH -> time.withDayOfMonth(1).toLocalDate().atStartOfDay(time.getZone());
        };
    }

    private ZonedDateTime advanceBucket(ZonedDateTime time, IndexSplitType splitType) {
        return switch (splitType) {
            case HOUR -> time.plusHours(1);
            case DAY -> time.plusDays(1);
            case MONTH -> time.plusMonths(1);
        };
    }
}
