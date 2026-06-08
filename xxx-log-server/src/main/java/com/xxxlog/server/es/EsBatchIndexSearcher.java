package com.xxxlog.server.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.xxxlog.common.model.LogRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 使用单次 _search 查询多索引（index1,index2,... 逗号拼接）。
 */
@Component
public class EsBatchIndexSearcher {

    private final ElasticsearchClient client;

    public EsBatchIndexSearcher(ElasticsearchClient client) {
        this.client = client;
    }

    public SearchResponse<LogRecord> search(
            List<String> indices,
            Consumer<SearchRequest.Builder> requestConfigurer) throws IOException {
        if (indices == null || indices.isEmpty()) {
            return null;
        }
        return client.search(s -> {
            s.index(indices)
                    .ignoreUnavailable(true)
                    .allowNoIndices(true);
            requestConfigurer.accept(s);
            return s;
        }, LogRecord.class);
    }

    public List<LogRecord> collectHits(SearchResponse<LogRecord> response) {
        if (response == null) {
            return List.of();
        }
        return response.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public long totalHits(SearchResponse<LogRecord> response) {
        if (response == null || response.hits().total() == null) {
            return 0;
        }
        return response.hits().total().value();
    }
}
