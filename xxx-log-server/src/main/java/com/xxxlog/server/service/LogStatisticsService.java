package com.xxxlog.server.service;

import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import com.xxxlog.common.model.LogRecord;
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

    private Query buildErrorTimeQuery(long startTime, long endTime) {
        return Query.of(q -> q.bool(b -> b
                .must(m -> m.term(t -> t.field("level").value("ERROR")))
                .must(m -> m.range(r -> r.field("timestamp")
                        .gte(JsonData.of(startTime))
                        .lte(JsonData.of(endTime))))));
    }
}
