package com.reggie.api_gateway.security;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;

@Component
public class RateLimiter {
    private RedisTemplate<String, Object> redis;

    public RateLimiter(RedisTemplate<String, Object> redis) {
        this.redis = redis;
    }

    public boolean isAllowed(String username) {
        String key = "rate:" + username;
        Long count = redis.opsForValue().increment(key);
        if (count.equals(1L)) {
            redis.expire(key, Duration.ofSeconds(60));
        }
        return count <= 100;
    }
}
