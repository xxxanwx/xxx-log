package com.xxxlog.server.dingtalk;

import com.xxxlog.common.enums.ErrorAlertStrategy;
import com.xxxlog.common.model.LogRecord;
import com.xxxlog.server.config.DingTalkProperties;
import com.xxxlog.server.config.ServerProperties;
import com.xxxlog.server.redis.RedisCooldownStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@ConditionalOnProperty(prefix = "xxx-log.dingtalk", name = "enabled", havingValue = "true")
public class DingTalkAlertService {

    private static final Logger log = LoggerFactory.getLogger(DingTalkAlertService.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final DingTalkProperties properties;
    private final DingTalkClient dingTalkClient;
    private final RedisCooldownStore cooldownStore;
    private final ServerProperties serverProperties;

    public DingTalkAlertService(DingTalkProperties properties,
                                DingTalkClient dingTalkClient,
                                RedisCooldownStore cooldownStore,
                                ServerProperties serverProperties) {
        this.properties = properties;
        this.dingTalkClient = dingTalkClient;
        this.cooldownStore = cooldownStore;
        this.serverProperties = serverProperties;
    }

    @Async
    public void onLogIngested(LogRecord record) {
        DingTalkProperties.ErrorAlert alert = properties.getErrorAlert();
        if (!alert.isEnabled() || record == null) {
            return;
        }
        if (!"ERROR".equalsIgnoreCase(record.getLevel())) {
            return;
        }
        if (!matchAppFilter(record.getAppName(), alert.getAppNames())) {
            return;
        }

        String searchText = buildSearchText(record);
        if (matchesAny(searchText, alert.getBlacklistKeywords())) {
            log.debug("DingTalk alert skipped: blacklist matched, app={}, traceId={}",
                    record.getAppName(), record.getTraceId());
            return;
        }

        String matchedKeyword = resolveMatchedKeyword(searchText, alert);
        if (matchedKeyword == null) {
            log.debug("DingTalk alert skipped: keyword not matched, app={}, traceId={}",
                    record.getAppName(), record.getTraceId());
            return;
        }
        if (!passCooldown(record)) {
            log.debug("DingTalk alert skipped: cooldown, app={}, traceId={}",
                    record.getAppName(), record.getTraceId());
            return;
        }

        dingTalkClient.sendMarkdown("ERROR 告警", buildErrorMarkdown(record, matchedKeyword, alert));
        log.info("DingTalk ERROR alert sent, app={}, traceId={}, keyword={}",
                record.getAppName(), record.getTraceId(), matchedKeyword);
    }

    public void sendDailySummary(String dateLabel, long total, Map<String, Long> byApp) {
        StringBuilder md = new StringBuilder();
        md.append("### xxx-log 日志日总结\n\n");
        md.append("- **日期**：").append(dateLabel).append("\n");
        md.append("- **ERROR 总数**：**").append(total).append("**\n\n");
        if (!byApp.isEmpty()) {
            md.append("| 应用 | ERROR 数 |\n");
            md.append("| --- | ---: |\n");
            byApp.forEach((app, count) -> md.append("| ").append(app).append(" | ").append(count).append(" |\n"));
        } else {
            md.append("> 昨日无 ERROR 日志\n");
        }
        dingTalkClient.sendMarkdown("日志日总结", md.toString());
        log.info("DingTalk daily summary pushed, date={}, total={}", dateLabel, total);
    }

    private boolean matchAppFilter(String appName, List<String> allowedApps) {
        if (allowedApps == null || allowedApps.isEmpty()) {
            return true;
        }
        if (!StringUtils.hasText(appName)) {
            return false;
        }
        for (String allowed : allowedApps) {
            if (appName.equalsIgnoreCase(allowed.trim())) {
                return true;
            }
        }
        return false;
    }

    private String resolveMatchedKeyword(String searchText, DingTalkProperties.ErrorAlert alert) {
        ErrorAlertStrategy strategy = alert.getStrategy() != null
                ? alert.getStrategy()
                : ErrorAlertStrategy.KEYWORD;
        return switch (strategy) {
            case ALL -> "ALL";
            case KEYWORD -> findFirstKeyword(searchText, alert.getKeywords());
            case BLACKLIST -> "BLACKLIST";
        };
    }

    private String findFirstKeyword(String text, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return null;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (StringUtils.hasText(keyword) && lower.contains(keyword.trim().toLowerCase(Locale.ROOT))) {
                return keyword.trim();
            }
        }
        return null;
    }

    private boolean matchesAny(String text, List<String> keywords) {
        return findFirstKeyword(text, keywords) != null;
    }

    private String buildSearchText(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(record.getMessage())) {
            sb.append(record.getMessage());
        }
        if (StringUtils.hasText(record.getStackTrace())) {
            sb.append('\n').append(record.getStackTrace());
        }
        if (StringUtils.hasText(record.getClassName())) {
            sb.append('\n').append(record.getClassName());
        }
        if (StringUtils.hasText(record.getLoggerName())) {
            sb.append('\n').append(record.getLoggerName());
        }
        return sb.toString();
    }

    private boolean passCooldown(LogRecord record) {
        int seconds = Math.max(properties.getErrorAlert().getMinIntervalSeconds(), 0);
        if (seconds == 0) {
            return true;
        }
        String cooldownKey = serverProperties.getLock().lockKey("dingtalk:cooldown:"
                + (record.getAppName() != null ? record.getAppName() : "")
                + "|" + truncate(record.getMessage(), 120));
        return !cooldownStore.inCooldown(cooldownKey, seconds);
    }

    private String buildErrorMarkdown(LogRecord record, String matchedKeyword,
                                      DingTalkProperties.ErrorAlert alert) {
        StringBuilder md = new StringBuilder();
        md.append("### 【ERROR】").append(nullToDash(record.getAppName())).append("\n\n");
        md.append("- **时间**：").append(formatTime(record.getTimestamp())).append("\n");
        md.append("- **TraceId**：").append(nullToDash(record.getTraceId())).append("\n");
        md.append("- **位置**：").append(nullToDash(record.getClassName()))
                .append(".").append(nullToDash(record.getMethodName())).append("\n");
        md.append("- **IP**：").append(nullToDash(record.getServerIp())).append("\n");
        md.append("- **内容**：").append(truncate(record.getMessage(), alert.getMaxMessageLength())).append("\n");
        if (alert.isIncludeStackTrace() && StringUtils.hasText(record.getStackTrace())) {
            md.append("\n```\n")
                    .append(truncate(record.getStackTrace(), alert.getMaxMessageLength()))
                    .append("\n```\n");
        }
        if (!"ALL".equals(matchedKeyword) && !"BLACKLIST".equals(matchedKeyword)) {
            md.append("\n> 命中关键词：**").append(matchedKeyword).append("**");
        }
        return md.toString();
    }

    private String formatTime(Long ts) {
        if (ts == null) {
            return "-";
        }
        return TIME_FMT.format(Instant.ofEpochMilli(ts));
    }

    private String nullToDash(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

    private String truncate(String text, int maxLen) {
        if (text == null) {
            return "-";
        }
        if (maxLen <= 0 || text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...";
    }
}
