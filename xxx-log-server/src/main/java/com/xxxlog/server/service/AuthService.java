package com.xxxlog.server.service;

import com.xxxlog.common.constant.LogConstants;
import com.xxxlog.server.config.AuthProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    private static final String TOKEN_PREFIX = "xxx-log:auth:token:";

    private final AuthProperties authProperties;
    private final StringRedisTemplate redisTemplate;

    public AuthService(AuthProperties authProperties, StringRedisTemplate redisTemplate) {
        this.authProperties = authProperties;
        this.redisTemplate = redisTemplate;
    }

    public String login(String username, String password) {
        if (!authProperties.getUsername().equals(username)
                || !authProperties.getPassword().equals(password)) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
                TOKEN_PREFIX + token,
                username,
                authProperties.getTokenExpireHours(),
                TimeUnit.HOURS);
        return token;
    }

    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_PREFIX + token));
    }

    public String getUsername(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        return redisTemplate.opsForValue().get(TOKEN_PREFIX + token);
    }

    public void logout(String token) {
        if (StringUtils.hasText(token)) {
            redisTemplate.delete(TOKEN_PREFIX + token);
        }
    }
}
