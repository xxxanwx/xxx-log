package com.xxxlog.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ServerStartupLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ServerStartupLogger.class);

    private final ServerProperties serverProperties;
    private final DingTalkProperties dingTalkProperties;

    public ServerStartupLogger(ServerProperties serverProperties, DingTalkProperties dingTalkProperties) {
        this.serverProperties = serverProperties;
        this.dingTalkProperties = dingTalkProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("========== xxx-log-server 已启动 ==========");
        log.info("队列类型: {}, Redis Key: {}, 消费批次: {}, 轮询间隔: {}ms",
                serverProperties.getQueueType(),
                serverProperties.getQueueKey(),
                serverProperties.getConsumeBatchSize(),
                serverProperties.getConsumeIntervalMs());
        log.info("ES 索引前缀: {}, 拆分粒度: {}",
                serverProperties.getIndexPrefix(),
                serverProperties.getIndexSplitType());
        if (dingTalkProperties.isEnabled()) {
            log.info("钉钉通知: 已启用, 日总结={}, ERROR策略={}, 关键词数={}",
                    dingTalkProperties.getDailySummary().isEnabled(),
                    dingTalkProperties.getErrorAlert().getStrategy(),
                    dingTalkProperties.getErrorAlert().getKeywords().size());
        } else {
            log.info("钉钉通知: 未启用");
        }
        log.info("==========================================");
    }
}
