package com.loopers.interfaces.consumer.ranking;

import com.loopers.config.redis.RedisConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

// 좋아요/주문 랭킹 반영을, 같은 이벤트가 재전달돼도 정확히 한 번만 적용되도록 멱등 처리한다.
// dedup 체크(SET NX)와 ZSET 반영을 하나의 Lua 스크립트로 묶어 Redis 서버에서 원자적으로 실행한다.
@RequiredArgsConstructor
@Component
public class RankingRedisIdempotencyWriter {

    private static final RedisScript<Long> IDEMPOTENT_INCREMENT_SCRIPT = RedisScript.of("""
            local dedupKey = KEYS[1]
            local rawKey = KEYS[2]
            local combinedKey = KEYS[3]
            local rawDelta = tonumber(ARGV[1])
            local combinedDelta = tonumber(ARGV[2])
            local dedupTtlSec = tonumber(ARGV[3])
            local rawTtlSec = tonumber(ARGV[4])

            local firstTime = redis.call('SET', dedupKey, '1', 'NX', 'EX', dedupTtlSec)
            if not firstTime then
                return 0
            end

            for i = 5, #ARGV do
                redis.call('ZINCRBY', rawKey, rawDelta, ARGV[i])
                redis.call('ZINCRBY', combinedKey, combinedDelta, ARGV[i])
            end
            redis.call('EXPIRE', rawKey, rawTtlSec)
            redis.call('EXPIRE', combinedKey, rawTtlSec)

            return 1
            """, Long.class);

    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private final RedisTemplate<String, String> masterRedisTemplate;

    /**
     * dedupKey가 처음 등장하면 productIds 전체에 rawKey/combinedKey ZSET 증감을 적용하고 true를 반환한다.
     * 이미 처리된 dedupKey면 아무 것도 하지 않고 false를 반환한다.
     */
    public boolean applyIfAbsent(
            String dedupKey,
            String rawKey,
            String combinedKey,
            List<Long> productIds,
            double rawDelta,
            double combinedDelta,
            Duration dedupTtl,
            Duration rawTtl
    ) {
        List<String> keys = List.of(dedupKey, rawKey, combinedKey);
        List<String> args = new ArrayList<>(List.of(
                String.valueOf(rawDelta),
                String.valueOf(combinedDelta),
                String.valueOf(dedupTtl.toSeconds()),
                String.valueOf(rawTtl.toSeconds())
        ));
        productIds.forEach(id -> args.add(String.valueOf(id)));

        Long applied = masterRedisTemplate.execute(IDEMPOTENT_INCREMENT_SCRIPT, keys, args.toArray());
        return applied != null && applied == 1L;
    }
}
