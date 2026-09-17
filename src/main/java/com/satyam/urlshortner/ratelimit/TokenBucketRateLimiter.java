package com.satyam.urlshortner.ratelimit;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Component
public class TokenBucketRateLimiter {

    // Refill + consume happen atomically in Redis so every app instance shares
    // one consistent bucket per client, making the limit hold across the cluster.
    private static final String SCRIPT = """
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local refill_rate = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local requested = tonumber(ARGV[4])

            local bucket = redis.call('HMGET', key, 'tokens', 'timestamp')
            local tokens = tonumber(bucket[1])
            local timestamp = tonumber(bucket[2])

            if tokens == nil then
              tokens = capacity
              timestamp = now
            end

            local elapsed = math.max(0, now - timestamp)
            tokens = math.min(capacity, tokens + (elapsed * refill_rate))

            local allowed = 0
            if tokens >= requested then
              tokens = tokens - requested
              allowed = 1
            end

            redis.call('HMSET', key, 'tokens', tokens, 'timestamp', now)
            redis.call('EXPIRE', key, math.ceil(capacity / refill_rate) + 1)

            return allowed
            """;

    private static final RedisScript<Long> REDIS_SCRIPT = RedisScript.of(SCRIPT, Long.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RateLimiterProperties properties;

    public TokenBucketRateLimiter(ReactiveStringRedisTemplate redisTemplate, RateLimiterProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public Mono<Boolean> tryConsume(String clientKey) {
        String key = "ratelimit:" + clientKey;
        double nowSeconds = Instant.now().toEpochMilli() / 1000.0;
        List<String> keys = List.of(key);
        List<String> args = List.of(
                String.valueOf(properties.capacity()),
                String.valueOf(properties.refillTokensPerSecond()),
                String.valueOf(nowSeconds),
                "1");

        return redisTemplate.execute(REDIS_SCRIPT, keys, args)
                .single()
                .map(result -> result == 1L);
    }
}
