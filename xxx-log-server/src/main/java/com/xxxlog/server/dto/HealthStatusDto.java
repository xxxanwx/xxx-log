package com.xxxlog.server.dto;

import java.util.Map;

public class HealthStatusDto {

    private String es;
    private String redis;
    private String rabbitmq;

    public HealthStatusDto() {
    }

    public HealthStatusDto(String es, String redis, String rabbitmq) {
        this.es = es;
        this.redis = redis;
        this.rabbitmq = rabbitmq;
    }

    public String getEs() {
        return es;
    }

    public void setEs(String es) {
        this.es = es;
    }

    public String getRedis() {
        return redis;
    }

    public void setRedis(String redis) {
        this.redis = redis;
    }

    public String getRabbitmq() {
        return rabbitmq;
    }

    public void setRabbitmq(String rabbitmq) {
        this.rabbitmq = rabbitmq;
    }

    public boolean isHealthy() {
        return "UP".equals(es) && "UP".equals(redis);
    }
}
