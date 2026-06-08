package com.xxxlog.server.dingtalk;

import com.xxxlog.server.config.DingTalkProperties;
import com.xxxlog.server.config.ServerProperties;
import com.xxxlog.server.redis.RedisDistributedLock;
import com.xxxlog.server.service.LogStatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "xxx-log.dingtalk", name = "enabled", havingValue = "true")
public class DingTalkDailySummaryJob {

    private static final Logger log = LoggerFactory.getLogger(DingTalkDailySummaryJob.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final DingTalkProperties properties;
    private final ServerProperties serverProperties;
    private final DingTalkAlertService alertService;
    private final LogStatisticsService logStatisticsService;
    private final RedisDistributedLock distributedLock;

    public DingTalkDailySummaryJob(DingTalkProperties properties,
                                   ServerProperties serverProperties,
                                   DingTalkAlertService alertService,
                                   LogStatisticsService logStatisticsService,
                                   RedisDistributedLock distributedLock) {
        this.properties = properties;
        this.serverProperties = serverProperties;
        this.alertService = alertService;
        this.logStatisticsService = logStatisticsService;
        this.distributedLock = distributedLock;
    }

    @Scheduled(cron = "${xxx-log.dingtalk.daily-summary.cron:0 0 9 * * ?}")
    public void sendYesterdaySummary() {
        if (!properties.getDailySummary().isEnabled()) {
            return;
        }
        String lockKey = serverProperties.getLock().lockKey("daily-summary");
        boolean executed = distributedLock.runWithLock(
                lockKey,
                serverProperties.getLock().getDailySummaryTtlSeconds(),
                this::doSendYesterdaySummary);
        if (!executed) {
            log.debug("DingTalk daily summary skipped, another node holds the lock");
        }
    }

    private void doSendYesterdaySummary() {
        try {
            ZoneId zone = ZoneId.systemDefault();
            LocalDate yesterday = LocalDate.now(zone).minusDays(1);
            long start = yesterday.atStartOfDay(zone).toInstant().toEpochMilli();
            long end = yesterday.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1;

            Map<String, Long> byApp = logStatisticsService.countErrorsByApp(start, end);
            long total = byApp.values().stream().mapToLong(Long::longValue).sum();
            alertService.sendDailySummary(DATE_FMT.format(yesterday), total, byApp);
            log.info("DingTalk daily summary sent, date={}, total={}", yesterday, total);
        } catch (Exception e) {
            log.warn("DingTalk daily summary failed: {}", e.getMessage());
        }
    }
}
