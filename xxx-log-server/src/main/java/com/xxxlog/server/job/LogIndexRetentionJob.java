package com.xxxlog.server.job;

import com.xxxlog.server.config.ServerProperties;
import com.xxxlog.server.redis.RedisDistributedLock;
import com.xxxlog.server.service.LogIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LogIndexRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(LogIndexRetentionJob.class);

    private final ServerProperties properties;
    private final LogIndexService logIndexService;
    private final RedisDistributedLock distributedLock;

    public LogIndexRetentionJob(ServerProperties properties,
                                LogIndexService logIndexService,
                                RedisDistributedLock distributedLock) {
        this.properties = properties;
        this.logIndexService = logIndexService;
        this.distributedLock = distributedLock;
    }

    @Scheduled(cron = "${xxx-log.index-retention.cron:0 0 2 * * ?}")
    public void cleanExpiredIndices() {
        if (!properties.getIndexRetention().isEnabled()) {
            return;
        }
        String lockKey = properties.getLock().lockKey("index-retention");
        boolean executed = distributedLock.runWithLock(
                lockKey,
                properties.getLock().getIndexRetentionTtlSeconds(),
                this::doClean);
        if (!executed) {
            log.debug("Index retention job skipped, another node holds the lock");
        }
    }

    private void doClean() {
        try {
            int deleted = logIndexService.deleteExpiredIndices();
            log.info("Index retention finished, deleted={}, retentionDays={}",
                    deleted, properties.getIndexRetention().getDays());
        } catch (Exception e) {
            log.warn("Index retention job failed: {}", e.getMessage());
        }
    }
}
