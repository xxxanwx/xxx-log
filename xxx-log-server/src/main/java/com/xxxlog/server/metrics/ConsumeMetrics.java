package com.xxxlog.server.metrics;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ConsumeMetrics {

    public static final String FAIL_COUNT_KEY = "xxx-log:stats:consume-fail";

    private final StringRedisTemplate redisTemplate;

    public ConsumeMetrics(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void incrementFailCount(int count) {
        if (count <= 0) {
            return;
        }
        redisTemplate.opsForValue().increment(FAIL_COUNT_KEY, count);
    }

    public long getFailCount() {
        String value = redisTemplate.opsForValue().get(FAIL_COUNT_KEY);
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
