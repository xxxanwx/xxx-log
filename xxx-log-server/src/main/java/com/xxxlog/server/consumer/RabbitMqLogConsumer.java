package com.xxxlog.server.consumer;

import com.rabbitmq.client.Channel;
import com.xxxlog.server.config.ServerProperties;
import com.xxxlog.server.metrics.ConsumeMetrics;
import com.xxxlog.server.service.LogBatchWriter;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

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
    private final ConsumeMetrics consumeMetrics;

    public RabbitMqLogConsumer(ServerProperties properties,
                               LogBatchWriter logBatchWriter,
                               ConsumeMetrics consumeMetrics) {
        this.properties = properties;
        this.logBatchWriter = logBatchWriter;
        this.consumeMetrics = consumeMetrics;
    }

    @PostConstruct
    public void init() {
        log.info("RabbitMQ log consumer started, queue={}, splitType={}",
                properties.getRabbitmq().getQueue(), properties.getIndexSplitType());
    }

    @RabbitListener(
            queues = "${xxx-log.rabbitmq.queue:xxx-log.queue}",
            containerFactory = "rabbitListenerContainerFactory")
    public void onMessage(String body,
                          Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            logBatchWriter.writeJson(body);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to consume RabbitMQ message", e);
            consumeMetrics.incrementFailCount(1);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("Failed to nack message", ioException);
            }
        }
    }
}
