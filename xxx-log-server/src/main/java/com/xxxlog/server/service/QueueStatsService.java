package com.xxxlog.server.service;

import com.xxxlog.common.enums.QueueType;
import com.xxxlog.server.config.ServerProperties;
import com.xxxlog.server.dto.QueueStatsDto;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class QueueStatsService {

    private final ServerProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final ObjectProvider<AmqpAdmin> amqpAdminProvider;

    public QueueStatsService(ServerProperties properties,
                             StringRedisTemplate redisTemplate,
                             ObjectProvider<AmqpAdmin> amqpAdminProvider) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.amqpAdminProvider = amqpAdminProvider;
    }

    public QueueStatsDto getStats() {
        QueueStatsDto stats = new QueueStatsDto();
        stats.setQueueType(properties.getQueueType().name().toLowerCase());
        stats.setBufferPending(0);

        if (properties.getQueueType() == QueueType.REDIS) {
            stats.setQueueName(properties.getQueueKey());
            Long size = redisTemplate.opsForList().size(properties.getQueueKey());
            stats.setQueuePending(size != null ? size : 0L);
        } else {
            stats.setQueueName(properties.getRabbitmq().getQueue());
            stats.setQueuePending(getRabbitMqPending(properties.getRabbitmq().getQueue()));
        }

        stats.setTotalPending(stats.getQueuePending());
        return stats;
    }

    private long getRabbitMqPending(String queueName) {
        AmqpAdmin amqpAdmin = amqpAdminProvider.getIfAvailable();
        if (amqpAdmin == null) {
            return 0L;
        }
        Properties props = amqpAdmin.getQueueProperties(queueName);
        if (props == null || props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT) == null) {
            return 0L;
        }
        return Long.parseLong(props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT).toString());
    }
}
