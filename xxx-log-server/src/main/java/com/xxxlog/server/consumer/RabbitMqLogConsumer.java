package com.xxxlog.server.consumer;

import com.xxxlog.server.config.ServerProperties;
import com.xxxlog.server.service.LogBatchWriter;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 日志消费：消息到达即消费并直接写 ES。
 * 集群防重复由队列 singleActiveConsumer 保证（见 RabbitMqConfig）。
 */
@Component
@ConditionalOnProperty(prefix = "xxx-log", name = "queue-type", havingValue = "rabbitmq")
public class RabbitMqLogConsumer {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqLogConsumer.class);

    private final ServerProperties properties;
    private final LogBatchWriter logBatchWriter;

    public RabbitMqLogConsumer(ServerProperties properties, LogBatchWriter logBatchWriter) {
        this.properties = properties;
        this.logBatchWriter = logBatchWriter;
    }

    @PostConstruct
    public void init() {
        log.info("RabbitMQ log consumer started, queue={}, splitType={}",
                properties.getRabbitmq().getQueue(), properties.getIndexSplitType());
    }

    @RabbitListener(queues = "${xxx-log.rabbitmq.queue:xxx-log.queue}")
    public void onMessage(String body) {
        logBatchWriter.writeJson(body);
    }
}
