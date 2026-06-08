package com.xxxlog.server.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Bytes;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.cat.indices.IndicesRecord;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import com.xxxlog.server.config.ServerProperties;
import com.xxxlog.server.dto.LogIndexInfoDto;
import com.xxxlog.server.dto.LogIndexManageDto;
import com.xxxlog.server.es.IndexNameResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class LogIndexService {

    private static final Logger log = LoggerFactory.getLogger(LogIndexService.class);

    private final ElasticsearchClient client;
    private final ServerProperties properties;
    private final IndexNameResolver indexNameResolver;

    public LogIndexService(ElasticsearchClient client,
                           ServerProperties properties,
                           IndexNameResolver indexNameResolver) {
        this.client = client;
        this.properties = properties;
        this.indexNameResolver = indexNameResolver;
    }

    public LogIndexManageDto listManageInfo() {
        LogIndexManageDto dto = new LogIndexManageDto();
        dto.setRetentionEnabled(properties.getIndexRetention().isEnabled());
        dto.setRetentionDays(properties.getIndexRetention().getDays());
        dto.setIndices(listLogIndices());
        return dto;
    }

    public List<LogIndexInfoDto> listLogIndices() {
        try {
            String pattern = properties.getIndexPrefix() + "-*";
            var response = client.cat().indices(c -> c
                    .index(pattern)
                    .bytes(Bytes.Bytes));
            List<LogIndexInfoDto> indices = new ArrayList<>();
            for (IndicesRecord row : response.valueBody()) {
                String indexName = row.index();
                if (!StringUtils.hasText(indexName) || !indexNameResolver.belongsToProject(indexName)) {
                    continue;
                }
                LogIndexInfoDto info = new LogIndexInfoDto();
                info.setIndexName(indexName);
                info.setAppName(indexNameResolver.extractAppName(indexName));
                info.setIndexTime(indexNameResolver.extractIndexTimeSuffix(indexName));
                info.setDocCount(parseLong(row.docsCount()));
                info.setStoreSize(formatStoreSize(row.storeSize()));
                info.setStoreSizeBytes(parseLong(row.storeSize()));
                indices.add(info);
            }
            indices.sort(Comparator.comparing(LogIndexInfoDto::getIndexName).reversed());
            return indices;
        } catch (IOException e) {
            throw new IllegalStateException("List log indices failed: " + e.getMessage(), e);
        }
    }

    public void deleteIndex(String indexName) {
        validateDeletable(indexName);
        try {
            DeleteIndexResponse response = client.indices().delete(d -> d.index(indexName));
            if (!response.acknowledged()) {
                throw new IllegalStateException("Delete index not acknowledged: " + indexName);
            }
            log.info("Deleted log index: {}", indexName);
        } catch (ElasticsearchException e) {
            if (e.response() != null && e.response().status() == 404) {
                throw new IllegalArgumentException("索引不存在: " + indexName);
            }
            throw new IllegalStateException("Delete index failed: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new IllegalStateException("Delete index failed: " + e.getMessage(), e);
        }
    }

    public int deleteExpiredIndices() {
        if (!properties.getIndexRetention().isEnabled()) {
            return 0;
        }
        int days = Math.max(properties.getIndexRetention().getDays(), 1);
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        int deleted = 0;
        for (LogIndexInfoDto index : listLogIndices()) {
            if (isExpired(index.getIndexName(), cutoff)) {
                try {
                    deleteIndex(index.getIndexName());
                    deleted++;
                } catch (Exception e) {
                    log.warn("Auto delete index failed, name={}, reason={}", index.getIndexName(), e.getMessage());
                }
            }
        }
        return deleted;
    }

    private boolean isExpired(String indexName, Instant cutoff) {
        return indexNameResolver.parseIndexStartInstant(indexName)
                .map(start -> start.isBefore(cutoff))
                .orElse(false);
    }

    private void validateDeletable(String indexName) {
        if (!StringUtils.hasText(indexName)) {
            throw new IllegalArgumentException("索引名不能为空");
        }
        if (!indexNameResolver.belongsToProject(indexName)) {
            throw new IllegalArgumentException("仅允许删除本项目日志索引: " + indexName);
        }
    }

    private long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String formatStoreSize(String bytesValue) {
        long bytes = parseLong(bytesValue);
        if (bytes <= 0) {
            return "0 B";
        }
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        double size = bytes;
        int unitIndex = 0;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        if (unitIndex == 0) {
            return String.format(Locale.ROOT, "%d %s", (long) size, units[unitIndex]);
        }
        return String.format(Locale.ROOT, "%.1f %s", size, units[unitIndex]);
    }
}
