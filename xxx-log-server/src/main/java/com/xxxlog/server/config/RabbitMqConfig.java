package com.xxxlog.server.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
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

    public static final String DEAD_LETTER_EXCHANGE = "xxx-log.dlx";
    public static final String DEAD_LETTER_ROUTING_KEY = "";

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue deadLetterQueue(ServerProperties properties) {
        return QueueBuilder.durable(properties.getRabbitmq().getDeadLetterQueue()).build();
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    public Queue xxxLogQueue(ServerProperties properties) {
        // singleActiveConsumer：RabbitMQ 侧仅一个消费者处于 active（需 RabbitMQ 3.11+）
        return QueueBuilder.durable(properties.getRabbitmq().getQueue())
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(DEAD_LETTER_ROUTING_KEY)
                .singleActiveConsumer()
                .build();
    }
}
