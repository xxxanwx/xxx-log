package com.xxxlog.server.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import com.xxxlog.common.enums.KeywordOperator;
import com.xxxlog.common.model.LogRecord;
import com.xxxlog.server.dto.LogQueryRequest;
import com.xxxlog.server.dto.PageResult;
import com.xxxlog.server.es.EsBatchIndexSearcher;
import com.xxxlog.server.es.IndexQueryPlanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LogQueryService {

    private static final Logger log = LoggerFactory.getLogger(LogQueryService.class);
    /** 链路查询未传时间时默认回溯 24 小时 */
    private static final long TRACE_DEFAULT_LOOKBACK_MS = 24L * 60 * 60 * 1000;
    /** from + size 深度分页上限 */
    private static final int MAX_FROM_SIZE = 10000;
    /** listEnvs 默认回溯 7 天 */
    private static final long ENV_DISCOVERY_LOOKBACK_MS = 7L * 24 * 60 * 60 * 1000;

    private final IndexQueryPlanner indexQueryPlanner;
    private final EsBatchIndexSearcher esBatchIndexSearcher;

    public LogQueryService(IndexQueryPlanner indexQueryPlanner,
                           EsBatchIndexSearcher esBatchIndexSearcher) {
        this.indexQueryPlanner = indexQueryPlanner;
        this.esBatchIndexSearcher = esBatchIndexSearcher;
    }

    public PageResult<LogRecord> search(LogQueryRequest request) {
        indexQueryPlanner.validateTimeRange(request.getStartTime(), request.getEndTime());
        List<String> indices = indexQueryPlanner.planSearchIndices(
                request.getAppName(), request.getStartTime(), request.getEndTime());
        log.debug("Log search indices ({}): {}", indices.size(), indices);
        int size = Math.min(Math.max(request.getSize(), 1), 500);
        if (indices.isEmpty()) {
            return emptyResult(request.getPage(), size);
        }

        try {
            int from = Math.max(0, (request.getPage() - 1) * size);
            List<Object> searchAfter = request.getSearchAfter();
            boolean useSearchAfter = searchAfter != null && !searchAfter.isEmpty();

            if (!useSearchAfter && from + size > MAX_FROM_SIZE) {
                throw new IllegalArgumentException(
                        "分页深度超过 " + MAX_FROM_SIZE + " 条限制，请使用游标分页（searchAfter）");
            }

            SortOrder sortOrder = resolveSortOrder(request.getSortOrder());
            Query query = buildQuery(request);

            SearchResponse<LogRecord> response = esBatchIndexSearcher.search(indices, s -> {
                s.size(size)
                        .trackTotalHits(t -> t.enabled(true))
                        .query(query)
                        .sort(sort -> sort.field(f -> f.field("timestamp").order(sortOrder)))
                        .sort(sort -> sort.field(f -> f.field("_id").order(sortOrder)));
                if (useSearchAfter) {
                    s.searchAfter(toFieldValues(searchAfter));
                } else {
                    s.from(from);
                }
            });

            long total = esBatchIndexSearcher.totalHits(response);
            List<LogRecord> records = esBatchIndexSearcher.collectHits(response);

            PageResult<LogRecord> result = new PageResult<>(total, request.getPage(), size, records);
            result.setNextSearchAfter(esBatchIndexSearcher.extractNextSearchAfter(response));
            result.setDeepPagination(useSearchAfter || from + size >= MAX_FROM_SIZE);
            return result;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Log search failed: " + e.getMessage(), e);
        }
    }

    public List<LogRecord> searchByTraceId(String traceId, String appName, Long startTime, Long endTime) {
        if (!StringUtils.hasText(traceId)) {
            return List.of();
        }
        long end = endTime != null && endTime > 0 ? endTime : System.currentTimeMillis();
        long start = startTime != null && startTime > 0 ? startTime : end - TRACE_DEFAULT_LOOKBACK_MS;
        indexQueryPlanner.validateTimeRange(start, end);

        List<String> indices = indexQueryPlanner.planSearchIndices(appName, start, end);
        log.debug("Trace search indices ({}): {}", indices.size(), indices);
        if (indices.isEmpty()) {
            return List.of();
        }
        Query query = buildTraceIdQuery(traceId.trim());

        try {
            SearchResponse<LogRecord> response = esBatchIndexSearcher.search(indices, s -> s
                    .size(500)
                    .query(query)
                    .sort(sort -> sort.field(f -> f.field("timestamp").order(SortOrder.Asc))));

            return esBatchIndexSearcher.collectHits(response);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Trace query failed: " + e.getMessage(), e);
        }
    }

    public List<String> listAppNames() {
        return indexQueryPlanner.discoverAppNames();
    }

    public List<String> listEnvs() {
        long endTime = System.currentTimeMillis();
        long startTime = endTime - ENV_DISCOVERY_LOOKBACK_MS;
        List<String> indices = indexQueryPlanner.planSearchIndices(null, startTime, endTime);
        if (indices.isEmpty()) {
            return List.of();
        }
        try {
            SearchResponse<LogRecord> response = esBatchIndexSearcher.search(indices, s -> s
                    .size(0)
                    .query(q -> q.range(r -> r.field("timestamp")
                            .gte(JsonData.of(startTime))
                            .lte(JsonData.of(endTime))))
                    .aggregations("by_env", a -> a.terms(t -> t.field("env").size(100))));

            List<String> envs = new ArrayList<>();
            var agg = response.aggregations().get("by_env");
            if (agg != null && agg.sterms() != null) {
                for (StringTermsBucket bucket : agg.sterms().buckets().array()) {
                    String env = bucket.key().stringValue();
                    if (StringUtils.hasText(env)) {
                        envs.add(env);
                    }
                }
            }
            return envs;
        } catch (Exception e) {
            log.warn("List envs failed: {}", e.getMessage());
            return List.of();
        }
    }

    private PageResult<LogRecord> emptyResult(int page, int size) {
        PageResult<LogRecord> result = new PageResult<>(0, page, size, List.of());
        result.setDeepPagination(false);
        return result;
    }

    private List<FieldValue> toFieldValues(List<Object> values) {
        return values.stream()
                .map(this::toFieldValue)
                .collect(Collectors.toList());
    }

    private FieldValue toFieldValue(Object value) {
        if (value == null) {
            return FieldValue.of("");
        }
        if (value instanceof Number) {
            return FieldValue.of(((Number) value).longValue());
        }
        if (value instanceof Boolean) {
            return FieldValue.of((Boolean) value);
        }
        return FieldValue.of(String.valueOf(value));
    }

    private Query buildQuery(LogQueryRequest request) {
        List<Query> must = new ArrayList<>();

        if (StringUtils.hasText(request.getTraceId())) {
            must.add(buildTraceIdQuery(request.getTraceId().trim()));
        }
        if (StringUtils.hasText(request.getAppName())) {
            must.add(Query.of(q -> q.term(t -> t.field("appName").value(request.getAppName()))));
        }
        if (StringUtils.hasText(request.getLevel())) {
            must.add(Query.of(q -> q.term(t -> t.field("level").value(request.getLevel()))));
        }
        if (StringUtils.hasText(request.getEnv())) {
            must.add(Query.of(q -> q.term(t -> t.field("env").value(request.getEnv()))));
        }
        applyKeywordQuery(must, request);
        must.add(Query.of(q -> q.range(r -> r
                .field("timestamp")
                .gte(JsonData.of(request.getStartTime()))
                .lte(JsonData.of(request.getEndTime())))));

        return Query.of(q -> q.bool(BoolQuery.of(b -> b.must(must))));
    }

    private void applyKeywordQuery(List<Query> must, LogQueryRequest request) {
        List<String> keywords = resolveKeywords(request);
        if (keywords.isEmpty()) {
            return;
        }

        List<Query> messageQueries = keywords.stream()
                .map(keyword -> Query.of(q -> q.match(m -> m.field("message").query(keyword))))
                .collect(Collectors.toList());

        KeywordOperator operator = request.getKeywordOperator() != null
                ? request.getKeywordOperator()
                : KeywordOperator.AND;

        if (operator == KeywordOperator.OR) {
            must.add(Query.of(q -> q.bool(b -> b
                    .should(messageQueries)
                    .minimumShouldMatch("1"))));
        } else {
            must.addAll(messageQueries);
        }
    }

    private List<String> resolveKeywords(LogQueryRequest request) {
        List<String> keywords = new ArrayList<>();
        if (request.getKeywords() != null) {
            for (String keyword : request.getKeywords()) {
                if (StringUtils.hasText(keyword)) {
                    keywords.add(keyword.trim());
                }
            }
        }
        if (keywords.isEmpty() && StringUtils.hasText(request.getKeyword())) {
            keywords.add(request.getKeyword().trim());
        }
        return keywords;
    }

    private Query buildTraceIdQuery(String traceId) {
        return Query.of(q -> q.bool(b -> b
                .should(s -> s.term(t -> t.field("traceId").value(traceId)))
                .should(s -> s.match(m -> m.field("traceId").query(traceId)))
                .minimumShouldMatch("1")));
    }

    private SortOrder resolveSortOrder(String sortOrder) {
        return "asc".equalsIgnoreCase(sortOrder) ? SortOrder.Asc : SortOrder.Desc;
    }
}
