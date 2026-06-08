package com.xxxlog.server.config;

import com.xxxlog.common.enums.IndexSplitType;
import com.xxxlog.common.enums.QueueType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "xxx-log")
public class ServerProperties {

    private String indexPrefix = "xxx-log";
    private IndexSplitType indexSplitType = IndexSplitType.DAY;
    private QueueType queueType = QueueType.REDIS;
    private String queueKey = "xxx-log:queue";
    private int consumeBatchSize = 100;
    private int consumeIntervalMs = 500;
    /** 单次查询允许扫描的最大时间分片数（按天/月） */
    private int maxSearchIndices = 2000;
    private IndexRetentionProperties indexRetention = new IndexRetentionProperties();
    private RabbitMqProperties rabbitmq = new RabbitMqProperties();
    private LockProperties lock = new LockProperties();

    public static class IndexRetentionProperties {
        /** 是否启用过期索引自动删除 */
        private boolean enabled = true;
        /** 日志索引保留天数，超出后自动删除 */
        private int days = 30;
        /** 自动清理 Cron，默认每天 02:00 执行一次 */
        private String cron = "0 0 2 * * ?";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getDays() {
            return days;
        }

        public void setDays(int days) {
            this.days = days;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }
    }

    public static class LockProperties {
        /** 分布式锁 key 前缀 */
        private String prefix = "xxx-log:lock";
        /** 从 Redis 队列拉取并写 ES 的锁 TTL（秒），仅 queue-type=redis 时生效 */
        private int redisConsumeTtlSeconds = 30;
        /** 钉钉日总结的锁 TTL（秒） */
        private int dailySummaryTtlSeconds = 300;
        /** 索引过期清理的锁 TTL（秒） */
        private int indexRetentionTtlSeconds = 600;

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public int getRedisConsumeTtlSeconds() {
            return redisConsumeTtlSeconds;
        }

        public void setRedisConsumeTtlSeconds(int redisConsumeTtlSeconds) {
            this.redisConsumeTtlSeconds = redisConsumeTtlSeconds;
        }

        public int getDailySummaryTtlSeconds() {
            return dailySummaryTtlSeconds;
        }

        public void setDailySummaryTtlSeconds(int dailySummaryTtlSeconds) {
            this.dailySummaryTtlSeconds = dailySummaryTtlSeconds;
        }

        public int getIndexRetentionTtlSeconds() {
            return indexRetentionTtlSeconds;
        }

        public void setIndexRetentionTtlSeconds(int indexRetentionTtlSeconds) {
            this.indexRetentionTtlSeconds = indexRetentionTtlSeconds;
        }

        public String lockKey(String name) {
            return prefix + ":" + name;
        }
    }

    public String getIndexPrefix() {
        return indexPrefix;
    }

    public void setIndexPrefix(String indexPrefix) {
        this.indexPrefix = indexPrefix;
    }

    public IndexSplitType getIndexSplitType() {
        return indexSplitType;
    }

    public void setIndexSplitType(IndexSplitType indexSplitType) {
        this.indexSplitType = indexSplitType;
    }

    public QueueType getQueueType() {
        return queueType;
    }

    public void setQueueType(QueueType queueType) {
        this.queueType = queueType;
    }

    public String getQueueKey() {
        return queueKey;
    }

    public void setQueueKey(String queueKey) {
        this.queueKey = queueKey;
    }

    public int getConsumeBatchSize() {
        return consumeBatchSize;
    }

    public void setConsumeBatchSize(int consumeBatchSize) {
        this.consumeBatchSize = consumeBatchSize;
    }

    public int getConsumeIntervalMs() {
        return consumeIntervalMs;
    }

    public void setConsumeIntervalMs(int consumeIntervalMs) {
        this.consumeIntervalMs = consumeIntervalMs;
    }

    public int getMaxSearchIndices() {
        return maxSearchIndices;
    }

    public void setMaxSearchIndices(int maxSearchIndices) {
        this.maxSearchIndices = maxSearchIndices;
    }

    public IndexRetentionProperties getIndexRetention() {
        return indexRetention;
    }

    public void setIndexRetention(IndexRetentionProperties indexRetention) {
        this.indexRetention = indexRetention;
    }

    public LockProperties getLock() {
        return lock;
    }

    public void setLock(LockProperties lock) {
        this.lock = lock;
    }

    public RabbitMqProperties getRabbitmq() {
        return rabbitmq;
    }

    public void setRabbitmq(RabbitMqProperties rabbitmq) {
        this.rabbitmq = rabbitmq;
    }
}
