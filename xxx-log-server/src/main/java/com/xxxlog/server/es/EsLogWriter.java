package com.xxxlog.server.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.xxxlog.common.model.LogRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EsLogWriter {

    private static final Logger log = LoggerFactory.getLogger(EsLogWriter.class);

    private final ElasticsearchClient client;
    private final IndexNameResolver indexNameResolver;

    public EsLogWriter(ElasticsearchClient client, IndexNameResolver indexNameResolver) {
        this.client = client;
        this.indexNameResolver = indexNameResolver;
    }

    public void bulkWrite(List<LogRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        try {
            BulkRequest.Builder builder = new BulkRequest.Builder();
            for (LogRecord record : records) {
                String index = indexNameResolver.resolve(record);
                builder.operations(op -> op
                        .index(idx -> idx
                                .index(index)
                                .id(record.getId())
                                .document(record)));
            }
            BulkResponse response = client.bulk(builder.build());
            if (response.errors()) {
                List<String> errors = response.items().stream()
                        .filter(item -> item.error() != null)
                        .map(item -> item.error().reason())
                        .collect(Collectors.toList());
                log.error("ES bulk write errors: {}", errors);
                throw new IllegalStateException("ES bulk write failed: " + errors);
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("ES bulk write failed", e);
            throw new IllegalStateException("ES bulk write failed", e);
        }
    }

    public void ensureIndexTemplate() {
        try {
            client.indices().putIndexTemplate(t -> t
                    .name("xxx-log-template")
                    .indexPatterns("xxx-log-*")
                    .template(tmpl -> tmpl
                            .mappings(m -> m
                                    .properties("traceId", p -> p.keyword(k -> k))
                                    .properties("spanId", p -> p.keyword(k -> k))
                                    .properties("parentSpanId", p -> p.keyword(k -> k))
                                    .properties("appName", p -> p.keyword(k -> k))
                                    .properties("env", p -> p.keyword(k -> k))
                                    .properties("serverIp", p -> p.keyword(k -> k))
                                    .properties("threadName", p -> p.keyword(k -> k))
                                    .properties("loggerName", p -> p.keyword(k -> k))
                                    .properties("className", p -> p.keyword(k -> k))
                                    .properties("methodName", p -> p.keyword(k -> k))
                                    .properties("level", p -> p.keyword(k -> k))
                                    .properties("message", p -> p.text(txt -> txt
                                            .analyzer("standard")
                                            .fields("keyword", f -> f.keyword(k -> k.ignoreAbove(512)))))
                                    .properties("stackTrace", p -> p.text(txt -> txt.analyzer("standard")))
                                    .properties("timestamp", p -> p.long_(l -> l))
                                    .properties("mdc", p -> p.object(o -> o.enabled(true)))
                            )));
        } catch (Exception e) {
            log.warn("Failed to create index template (may already exist): {}", e.getMessage());
        }
    }
}
