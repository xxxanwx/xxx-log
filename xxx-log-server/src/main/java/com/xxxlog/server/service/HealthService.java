package com.xxxlog.server.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.xxxlog.common.enums.QueueType;
import com.xxxlog.server.config.ServerProperties;
import com.xxxlog.server.dto.HealthStatusDto;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    private final ElasticsearchClient esClient;
    private final StringRedisTemplate redisTemplate;
    private final ServerProperties properties;
    private final ObjectProvider<ConnectionFactory> rabbitConnectionFactory;

    public HealthService(ElasticsearchClient esClient,
                         StringRedisTemplate redisTemplate,
                         ServerProperties properties,
                         ObjectProvider<ConnectionFactory> rabbitConnectionFactory) {
        this.esClient = esClient;
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.rabbitConnectionFactory = rabbitConnectionFactory;
    }

    public HealthStatusDto check() {
        return new HealthStatusDto(checkEs(), checkRedis(), checkRabbitMq());
    }

    private String checkEs() {
        try {
            boolean ok = esClient.ping().value();
            return ok ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    private String checkRedis() {
        try {
            redisTemplate.hasKey("xxx-log:health-probe");
            return "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    private String checkRabbitMq() {
        if (properties.getQueueType() != QueueType.RABBITMQ) {
            return "N/A";
        }
        ConnectionFactory factory = rabbitConnectionFactory.getIfAvailable();
        if (factory == null) {
            return "DOWN";
        }
        try {
            factory.createConnection().close();
            return "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }
}
