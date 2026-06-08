package com.xxxlog.server.service;

import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.TopHitsAggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.xxxlog.common.model.LogRecord;
import com.xxxlog.server.dto.DashboardOverviewDto;
import com.xxxlog.server.dto.TopErrorDto;
import com.xxxlog.server.dto.TrendPointDto;
import com.xxxlog.server.es.EsBatchIndexSearcher;
import com.xxxlog.server.es.IndexQueryPlanner;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LogStatisticsService {

    private final IndexQueryPlanner indexQueryPlanner;
    private final EsBatchIndexSearcher esBatchIndexSearcher;

    public LogStatisticsService(IndexQueryPlanner indexQueryPlanner,
                                EsBatchIndexSearcher esBatchIndexSearcher) {
        this.indexQueryPlanner = indexQueryPlanner;
        this.esBatchIndexSearcher = esBatchIndexSearcher;
    }

    public long countErrors(long startTime, long endTime) {
        Map<String, Long> byApp = countErrorsByApp(startTime, endTime);
        return byApp.values().stream().mapToLong(Long::longValue).sum();
    }

    public Map<String, Long> countErrorsByApp(long startTime, long endTime) {
        indexQueryPlanner.validateTimeRange(startTime, endTime);
        List<String> indices = indexQueryPlanner.planSearchIndices(null, startTime, endTime);
        if (indices.isEmpty()) {
            return Map.of();
        }
        Query query = buildErrorTimeQuery(startTime, endTime);

        try {
            SearchResponse<LogRecord> response = esBatchIndexSearcher.search(indices, s -> s
                    .size(0)
                    .trackTotalHits(t -> t.enabled(true))
                    .query(query)
                    .aggregations("by_app", a -> a.terms(t -> t.field("appName").size(200))));

            Map<String, Long> result = new LinkedHashMap<>();
            long total = esBatchIndexSearcher.totalHits(response);
            var agg = response.aggregations().get("by_app");
            if (agg != null && agg.sterms() != null) {
                for (StringTermsBucket bucket : agg.sterms().buckets().array()) {
                    result.put(bucket.key().stringValue(), bucket.docCount());
                }
            }
            if (result.isEmpty() && total > 0) {
                result.put("全部", total);
            }
            List<Map.Entry<String, Long>> sorted = new ArrayList<>(result.entrySet());
            sorted.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
            Map<String, Long> ordered = new LinkedHashMap<>();
            for (Map.Entry<String, Long> entry : sorted) {
                ordered.put(entry.getKey(), entry.getValue());
            }
            return ordered;
        } catch (Exception e) {
            throw new IllegalStateException("Count errors failed: " + e.getMessage(), e);
        }
    }

    public DashboardOverviewDto overview(long startTime, long endTime) {
        indexQueryPlanner.validateTimeRange(startTime, endTime);
        List<String> indices = indexQueryPlanner.planSearchIndices(null, startTime, endTime);
        DashboardOverviewDto dto = new DashboardOverviewDto();
        if (indices.isEmpty()) {
            dto.setTotalLogs(0);
            dto.setErrorCount(0);
            dto.setWarnCount(0);
            dto.setErrorsByApp(Map.of());
            return dto;
        }
        try {
            SearchResponse<LogRecord> response = esBatchIndexSearcher.search(indices, s -> s
                    .size(0)
                    .trackTotalHits(t -> t.enabled(true))
                    .query(buildTimeQuery(startTime, endTime))
                    .aggregations("by_level", a -> a.terms(t -> t.field("level").size(10)))
                    .aggregations("errors_by_app", a -> a.terms(t -> t.field("appName").size(200))));

            dto.setTotalLogs(esBatchIndexSearcher.totalHits(response));

            long errorCount = 0;
            long warnCount = 0;
            var levelAgg = response.aggregations().get("by_level");
            if (levelAgg != null && levelAgg.sterms() != null) {
                for (StringTermsBucket bucket : levelAgg.sterms().buckets().array()) {
                    String level = bucket.key().stringValue();
                    if ("ERROR".equalsIgnoreCase(level)) {
                        errorCount = bucket.docCount();
                    } else if ("WARN".equalsIgnoreCase(level)) {
                        warnCount = bucket.docCount();
                    }
                }
            }
            dto.setErrorCount(errorCount);
            dto.setWarnCount(warnCount);

            Map<String, Long> errorsByApp = new LinkedHashMap<>();
            SearchResponse<LogRecord> errorResponse = esBatchIndexSearcher.search(indices, s -> s
                    .size(0)
                    .query(buildErrorTimeQuery(startTime, endTime))
                    .aggregations("by_app", a -> a.terms(t -> t.field("appName").size(200))));
            var appAgg = errorResponse.aggregations().get("by_app");
            if (appAgg != null && appAgg.sterms() != null) {
                for (StringTermsBucket bucket : appAgg.sterms().buckets().array()) {
                    errorsByApp.put(bucket.key().stringValue(), bucket.docCount());
                }
            }
            dto.setErrorsByApp(errorsByApp);
            return dto;
        } catch (Exception e) {
            throw new IllegalStateException("Dashboard overview failed: " + e.getMessage(), e);
        }
    }

    public List<TrendPointDto> errorTrend(long startTime, long endTime, String interval) {
        return levelTrend(startTime, endTime, interval, "ERROR");
    }

    public List<TrendPointDto> ingestTrend(long startTime, long endTime, String interval) {
        indexQueryPlanner.validateTimeRange(startTime, endTime);
        List<String> indices = indexQueryPlanner.planSearchIndices(null, startTime, endTime);
        if (indices.isEmpty()) {
            return List.of();
        }
        CalendarInterval calInterval = resolveInterval(interval);
        try {
            SearchResponse<LogRecord> response = esBatchIndexSearcher.search(indices, s -> s
                    .size(0)
                    .query(buildTimeQuery(startTime, endTime))
                    .aggregations("trend", a -> a.dateHistogram(d -> d
                            .field("timestamp")
                            .calendarInterval(calInterval)
                            .format("epoch_millis")
                            .minDocCount(0))));

            return extractTrendPoints(response, "trend");
        } catch (Exception e) {
            throw new IllegalStateException("Ingest trend failed: " + e.getMessage(), e);
        }
    }

    public List<TopErrorDto> topErrors(long startTime, long endTime, int limit) {
        indexQueryPlanner.validateTimeRange(startTime, endTime);
        List<String> indices = indexQueryPlanner.planSearchIndices(null, startTime, endTime);
        if (indices.isEmpty()) {
            return List.of();
        }
        int size = Math.min(Math.max(limit, 1), 50);
        try {
            List<TopErrorDto> byClass = topErrorsByField(indices, startTime, endTime, size, "className");
            if (!byClass.isEmpty()) {
                return byClass;
            }
            return topErrorsByField(indices, startTime, endTime, size, "message.keyword");
        } catch (Exception e) {
            throw new IllegalStateException("Top errors failed: " + e.getMessage(), e);
        }
    }

    private List<TopErrorDto> topErrorsByField(
            List<String> indices, long startTime, long endTime, int size, String field) throws Exception {
        SearchResponse<LogRecord> response = esBatchIndexSearcher.search(indices, s -> s
                .size(0)
                .query(buildErrorTimeQuery(startTime, endTime))
                .aggregations("top_errors", a -> a
                        .terms(t -> t.field(field).size(size))
                        .aggregations("sample_msg", sa -> sa.topHits(th -> th
                                .size(1)
                                .source(src -> src.filter(f -> f.includes("message", "className")))))));

        List<TopErrorDto> result = new ArrayList<>();
        var agg = response.aggregations().get("top_errors");
        if (agg == null || agg.sterms() == null) {
            return result;
        }
        for (StringTermsBucket bucket : agg.sterms().buckets().array()) {
            String label = bucket.key().stringValue();
            String message = extractSampleMessage(bucket);
            if (message != null && !message.isBlank()) {
                label = message;
            } else if (label != null && label.length() > 200) {
                label = label.substring(0, 200) + "...";
            }
            result.add(new TopErrorDto(label, null, bucket.docCount()));
        }
        return result;
    }

    private String extractSampleMessage(StringTermsBucket bucket) {
        if (bucket.aggregations() == null) {
            return null;
        }
        var sampleAgg = bucket.aggregations().get("sample_msg");
        if (sampleAgg == null || sampleAgg.topHits() == null) {
            return null;
        }
        TopHitsAggregate topHits = sampleAgg.topHits();
        if (topHits.hits().hits().isEmpty()) {
            return null;
        }
        Hit<LogRecord> hit = topHits.hits().hits().get(0);
        LogRecord record = hit.source();
        if (record == null || record.getMessage() == null) {
            return null;
        }
        String message = record.getMessage().trim();
        if (message.length() > 200) {
            return message.substring(0, 200) + "...";
        }
        return message;
    }

    private List<TrendPointDto> levelTrend(long startTime, long endTime, String interval, String level) {
        indexQueryPlanner.validateTimeRange(startTime, endTime);
        List<String> indices = indexQueryPlanner.planSearchIndices(null, startTime, endTime);
        if (indices.isEmpty()) {
            return List.of();
        }
        CalendarInterval calInterval = resolveInterval(interval);
        Query query = Query.of(q -> q.bool(b -> b
                .must(m -> m.term(t -> t.field("level").value(level)))
                .must(m -> m.range(r -> r.field("timestamp")
                        .gte(JsonData.of(startTime))
                        .lte(JsonData.of(endTime))))));

        try {
            SearchResponse<LogRecord> response = esBatchIndexSearcher.search(indices, s -> s
                    .size(0)
                    .query(query)
                    .aggregations("trend", a -> a.dateHistogram(d -> d
                            .field("timestamp")
                            .calendarInterval(calInterval)
                            .format("epoch_millis")
                            .minDocCount(0))));

            return extractTrendPoints(response, "trend");
        } catch (Exception e) {
            throw new IllegalStateException("Error trend failed: " + e.getMessage(), e);
        }
    }

    private List<TrendPointDto> extractTrendPoints(SearchResponse<LogRecord> response, String aggName) {
        List<TrendPointDto> points = new ArrayList<>();
        var agg = response.aggregations().get(aggName);
        if (agg != null && agg.dateHistogram() != null) {
            for (DateHistogramBucket bucket : agg.dateHistogram().buckets().array()) {
                points.add(new TrendPointDto(bucket.key(), bucket.docCount()));
            }
        }
        return points;
    }

    private CalendarInterval resolveInterval(String interval) {
        if ("hour".equalsIgnoreCase(interval)) {
            return CalendarInterval.Hour;
        }
        if ("day".equalsIgnoreCase(interval)) {
            return CalendarInterval.Day;
        }
        return CalendarInterval.Hour;
    }

    private Query buildTimeQuery(long startTime, long endTime) {
        return Query.of(q -> q.range(r -> r.field("timestamp")
                .gte(JsonData.of(startTime))
                .lte(JsonData.of(endTime))));
    }

    private Query buildErrorTimeQuery(long startTime, long endTime) {
        return Query.of(q -> q.bool(b -> b
                .must(m -> m.term(t -> t.field("level").value("ERROR")))
                .must(m -> m.range(r -> r.field("timestamp")
                        .gte(JsonData.of(startTime))
                        .lte(JsonData.of(endTime))))));
    }
}
