package com.xxxlog.server.job;

import com.xxxlog.server.config.ServerProperties;
import com.xxxlog.server.dingtalk.DingTalkClient;
import com.xxxlog.server.dto.QueueStatsDto;
import com.xxxlog.server.redis.RedisCooldownStore;
import com.xxxlog.server.service.QueueStatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class QueueBacklogAlertJob {

    private static final Logger log = LoggerFactory.getLogger(QueueBacklogAlertJob.class);
    private static final String COOLDOWN_KEY = "xxx-log:alert:queue-backlog";

    private final ServerProperties properties;
    private final QueueStatsService queueStatsService;
    private final RedisCooldownStore cooldownStore;
    private final ObjectProvider<DingTalkClient> dingTalkClient;

    public QueueBacklogAlertJob(ServerProperties properties,
                                QueueStatsService queueStatsService,
                                RedisCooldownStore cooldownStore,
                                ObjectProvider<DingTalkClient> dingTalkClient) {
        this.properties = properties;
        this.queueStatsService = queueStatsService;
        this.cooldownStore = cooldownStore;
        this.dingTalkClient = dingTalkClient;
    }

    @Scheduled(fixedDelayString = "${xxx-log.alert.queue-backlog-check-interval-ms:60000}")
    public void checkBacklog() {
        QueueStatsDto stats = queueStatsService.getStats();
        long threshold = stats.getThreshold();
        if (stats.getTotalPending() < threshold) {
            return;
        }
        int cooldownMinutes = properties.getAlert().getQueueBacklogCooldownMinutes();
        if (cooldownStore.inCooldown(COOLDOWN_KEY, cooldownMinutes * 60)) {
            return;
        }
        DingTalkClient client = dingTalkClient.getIfAvailable();
        if (client == null) {
            return;
        }
        String markdown = String.format(
                "### xxx-log 队列积压告警\n\n" +
                        "- 队列类型：**%s**\n" +
                        "- 队列名称：`%s`\n" +
                        "- 待处理：**%d**（阈值 %d）\n" +
                        "- 消费失败累计：**%d**",
                stats.getQueueType(),
                stats.getQueueName(),
                stats.getTotalPending(),
                threshold,
                stats.getConsumeFailCount());
        client.sendMarkdown("xxx-log 队列积压告警", markdown);
        log.warn("Queue backlog alert sent, pending={}, threshold={}", stats.getTotalPending(), threshold);
    }
}
