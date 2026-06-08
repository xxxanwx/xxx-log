package com.xxxlog.server.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.xxxlog.server.config.ServerProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 按  clientQuery 方式规划 ES 查询索引：按天/月生成索引模式，过滤存在的索引后逗号拼接 _search。
 */
@Component
public class IndexQueryPlanner {

    private final ElasticsearchClient client;
    private final IndexNameResolver indexNameResolver;
    private final ServerProperties properties;

    public IndexQueryPlanner(ElasticsearchClient client,
                             IndexNameResolver indexNameResolver,
                             ServerProperties properties) {
        this.client = client;
        this.indexNameResolver = indexNameResolver;
        this.properties = properties;
    }

    public List<String> planSearchIndices(String appName, long startTime, long endTime) {
        validateTimeRange(startTime, endTime);

        List<String> patterns = indexNameResolver.enumerateSearchIndexPatterns(appName, startTime, endTime);
        int maxBuckets = properties.getMaxSearchIndices();
        if (patterns.size() > maxBuckets) {
            throw new IllegalArgumentException(String.format(
                    "查询时间范围过大，需扫描 %d 个时间分片（上限 %d），请缩小时间范围",
                    patterns.size(), maxBuckets));
        }

        return resolveExistingIndices(patterns);
    }

    /**
     * 检查索引模式是否存在。
     */
    public List<String> resolveExistingIndices(List<String> patterns) {
        List<String> existIndices = new ArrayList<>();
        for (String pattern : patterns) {
            try {
                if (client.indices().exists(e -> e.index(pattern)).value()) {
                    existIndices.add(pattern);
                }
            } catch (Exception ignored) {
                // 单个模式检查失败则跳过
            }
        }
        return existIndices;
    }

    public List<String> discoverAppNames() {
        try {
            String prefix = properties.getIndexPrefix() + "-";
            var response = client.cat().indices(c -> c.index(prefix + "*"));
            Set<String> apps = new TreeSet<>();
            for (var row : response.valueBody()) {
                String indexName = row.index();
                if (indexName != null) {
                    String app = indexNameResolver.extractAppName(indexName);
                    if (app != null) {
                        apps.add(app);
                    }
                }
            }
            return new ArrayList<>(apps);
        } catch (Exception e) {
            return List.of();
        }
    }

    public void validateTimeRange(long startTime, long endTime) {
        if (startTime <= 0 || endTime <= 0) {
            throw new IllegalArgumentException("查询开始时间和结束时间必填");
        }
        if (startTime > endTime) {
            throw new IllegalArgumentException("开始时间不能晚于结束时间");
        }
    }
}
