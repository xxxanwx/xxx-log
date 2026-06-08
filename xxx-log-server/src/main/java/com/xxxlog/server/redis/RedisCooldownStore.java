package com.xxxlog.server.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 基于 Redis 的冷却窗口，集群内共享，避免多节点重复告警。
 */
@Component
public class RedisCooldownStore {

    private final StringRedisTemplate redisTemplate;

    public RedisCooldownStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * @return true 表示在冷却期内应跳过；false 表示可继续执行并已记录本次时间
     */
    public boolean inCooldown(String key, int cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            return false;
        }
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofSeconds(cooldownSeconds));
        return !Boolean.TRUE.equals(acquired);
    }
}
