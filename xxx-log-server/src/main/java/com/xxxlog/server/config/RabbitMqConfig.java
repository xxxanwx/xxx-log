package com.xxxlog.server.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "xxx-log", name = "queue-type", havingValue = "rabbitmq")
@ImportAutoConfiguration(RabbitAutoConfiguration.class)
public class RabbitMqConfig {

    @Bean
    public Queue xxxLogQueue(ServerProperties properties) {
        // singleActiveConsumer：RabbitMQ 侧仅一个消费者处于 active（需 RabbitMQ 3.11+）
        return QueueBuilder.durable(properties.getRabbitmq().getQueue())
                .singleActiveConsumer()
                .build();
    }
}
