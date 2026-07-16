package com.loopers.application.order;

import com.loopers.config.redis.RedisConfig;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

// 대기열 admission(초당 처리량)은 "입장 속도"만 제어하고, ALLOWED 이후 5분 TTL 동안
// 유저가 언제 주문을 누르는지는 제어하지 않는다. 그 5분 동안 쌓인 사람들이
// 비슷한 시점에 몰리면 admission 페이스와 무관하게 주문 API가 순간적으로 과부하될 수 있어서,
// 전역 토큰버킷으로 초당 처리 상한(부하테스트로 확인한 안전선)을 별도로 건다.
@RequiredArgsConstructor
@Component
public class OrderRateLimiter {

    private static final String BUCKET_KEY = "order:ratelimit:bucket";

    private static final RedisScript<Long> TOKEN_BUCKET_SCRIPT = RedisScript.of("""
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local refillPerSec = tonumber(ARGV[2])
            local time = redis.call('TIME')
            local nowMs = tonumber(time[1]) * 1000 + tonumber(time[2]) / 1000

            local bucket = redis.call('HMGET', key, 'tokens', 'ts')
            local tokens = tonumber(bucket[1])
            local ts = tonumber(bucket[2])
            if tokens == nil then
                tokens = capacity
                ts = nowMs
            end

            local elapsedSec = math.max(0, (nowMs - ts) / 1000)
            tokens = math.min(capacity, tokens + elapsedSec * refillPerSec)

            local allowed = 0
            if tokens >= 1 then
                tokens = tokens - 1
                allowed = 1
            end

            redis.call('HMSET', key, 'tokens', tostring(tokens), 'ts', tostring(nowMs))
            redis.call('EXPIRE', key, 60)
            return allowed
            """, Long.class);

    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private final RedisTemplate<String, String> masterRedisTemplate;

    @Value("${order.rate-limit.capacity}")
    private int capacity;
    @Value("${order.rate-limit.refill-per-sec}")
    private int refillPerSec;

    public void checkLimit() {
        Long allowed = masterRedisTemplate.execute(
                TOKEN_BUCKET_SCRIPT,
                List.of(BUCKET_KEY),
                String.valueOf(capacity),
                String.valueOf(refillPerSec)
        );
        if (allowed == null || allowed != 1L) {
            throw new CoreException(ErrorType.TOO_MANY_REQUESTS);
        }
    }
}
