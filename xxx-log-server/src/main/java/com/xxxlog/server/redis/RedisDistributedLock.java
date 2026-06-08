package com.xxxlog.server.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 基于 Redis SET NX EX 的分布式锁，供集群部署下单节点执行任务。
 */
@Component
public class RedisDistributedLock {

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            """
                    if redis.call('get', KEYS[1]) == ARGV[1] then
                      return redis.call('del', KEYS[1])
                    else
                      return 0
                    end
                    """,
            Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisDistributedLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryLock(String key, String token, long ttlSeconds) {
        if (ttlSeconds <= 0) {
            ttlSeconds = 30;
        }
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, token, Duration.ofSeconds(ttlSeconds));
        return Boolean.TRUE.equals(acquired);
    }

    public void unlock(String key, String token) {
        redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), token);
    }

    /**
     * 获取锁后执行任务，未获取到锁则跳过。
     *
     * @return true 表示已执行；false 表示未抢到锁
     */
    public boolean runWithLock(String lockKey, long ttlSeconds, Runnable action) {
        String token = UUID.randomUUID().toString();
        if (!tryLock(lockKey, token, ttlSeconds)) {
            return false;
        }
        try {
            action.run();
            return true;
        } finally {
            unlock(lockKey, token);
        }
    }

    /**
     * 获取锁后执行任务并返回结果，未获取到锁返回 null。
     */
    public <T> T callWithLock(String lockKey, long ttlSeconds, Supplier<T> action) {
        String token = UUID.randomUUID().toString();
        if (!tryLock(lockKey, token, ttlSeconds)) {
            return null;
        }
        try {
            return action.get();
        } finally {
            unlock(lockKey, token);
        }
    }
}
