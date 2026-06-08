package com.xxxlog.server.consumer;

import com.xxxlog.server.config.ServerProperties;
import com.xxxlog.server.metrics.ConsumeMetrics;
import com.xxxlog.server.redis.RedisDistributedLock;
import com.xxxlog.server.service.LogBatchWriter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "xxx-log", name = "queue-type", havingValue = "redis", matchIfMissing = true)
public class RedisLogConsumer {

    private static final Logger log = LoggerFactory.getLogger(RedisLogConsumer.class);

    private final StringRedisTemplate redisTemplate;
    private final ServerProperties properties;
    private final LogBatchWriter logBatchWriter;
    private final RedisDistributedLock distributedLock;
    private final ConsumeMetrics consumeMetrics;

    public RedisLogConsumer(StringRedisTemplate redisTemplate,
                            ServerProperties properties,
                            LogBatchWriter logBatchWriter,
                            RedisDistributedLock distributedLock,
                            ConsumeMetrics consumeMetrics) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.logBatchWriter = logBatchWriter;
        this.distributedLock = distributedLock;
        this.consumeMetrics = consumeMetrics;
    }

    @PostConstruct
    public void init() {
        log.info("Redis log consumer started, queue={}, splitType={}",
                properties.getQueueKey(), properties.getIndexSplitType());
    }

    /**
     * 从 Redis 队列拉取并直接写入 ES，集群内通过 Redis 分布式锁保证单节点消费。
     */
    @Scheduled(fixedDelayString = "${xxx-log.consume-interval-ms:500}")
    public void consume() {
        String lockKey = properties.getLock().lockKey("redis-consume");
        distributedLock.runWithLock(lockKey, properties.getLock().getRedisConsumeTtlSeconds(), () -> {
            String queueKey = properties.getQueueKey();
            int batchSize = properties.getConsumeBatchSize();
            List<String> batch = new ArrayList<>(batchSize);

            for (int i = 0; i < batchSize; i++) {
                String json = redisTemplate.opsForList().rightPop(queueKey);
                if (json == null) {
                    break;
                }
                batch.add(json);
            }
            if (!batch.isEmpty()) {
                try {
                    logBatchWriter.writeBatch(batch);
                    log.info("Consumed {} message(s) from Redis queue [{}]", batch.size(), queueKey);
                } catch (Exception e) {
                    log.error("Failed to write batch to ES, pushing to dead-letter queue", e);
                    pushToDeadLetter(batch);
                    consumeMetrics.incrementFailCount(batch.size());
                }
            }
        });
    }

    private void pushToDeadLetter(List<String> batch) {
        String deadLetterKey = properties.getDeadLetterKey();
        for (String json : batch) {
            redisTemplate.opsForList().leftPush(deadLetterKey, json);
        }
        log.warn("Pushed {} message(s) to dead-letter key [{}]", batch.size(), deadLetterKey);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Redis log consumer shutting down");
    }
}
