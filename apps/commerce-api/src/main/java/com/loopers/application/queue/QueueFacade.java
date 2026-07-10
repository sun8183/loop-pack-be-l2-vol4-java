package com.loopers.application.queue;

import com.loopers.config.redis.RedisConfig;
import com.loopers.domain.queue.enums.QueueStatus;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class QueueFacade {

    static final String QUEUE_SEQ_KEY = "order:queue:seq";
    static final String QUEUE_KEY = "order:queue:waiting";
    static final String ALLOWED_KEY_PREFIX = "order:queue:allowed:";
    static final Duration ALLOWED_TTL = Duration.ofMinutes(5);
    static final String USER_TOKEN_KEY_PREFIX = "order:queue:user:";
    static final Duration USER_TOKEN_TTL = Duration.ofMinutes(30);

    // KEYS[1]=대기열 ZSET, ARGV[1]=꺼낼 인원수, ARGV[2]=허용TTL(초), ARGV[3]=허용상태값, ARGV[4]=허용키 prefix
    // ZPOPMIN으로 꺼낸 인원 전부에게 허용 키를 세팅하는 것까지 한 번에 원자적으로 처리해, 중간에 죽어도 "꺼냈는데 허용 못 받은" 유령 상태가 안 생기게 한다.
    private static final RedisScript<Long> ADMIT_SCRIPT = RedisScript.of(
            "local popped = redis.call('ZPOPMIN', KEYS[1], ARGV[1]) "
                    + "local count = 0 "
                    + "for i = 1, #popped, 2 do "
                    + "local token = popped[i] "
                    + "redis.call('SET', ARGV[4] .. token, ARGV[3], 'EX', ARGV[2]) "
                    + "count = count + 1 "
                    + "end "
                    + "return count",
            Long.class
    );

    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private final RedisTemplate<String, String> masterRedisTemplate;

    @Value("${queue.admission.batch-size}")
    private int admissionBatchSize;
    @Value("${queue.admission.fixed-delay-ms}")
    private long admissionFixedDelayMs;

    public QueueEnterInfo enter(Long userId) {
        String existingToken = masterRedisTemplate.opsForValue().get(USER_TOKEN_KEY_PREFIX + userId);
        if (existingToken != null) {
            QueueRankInfo existing = getRank(existingToken);
            if (existing.status() != QueueStatus.NOT_FOUND) {
                return new QueueEnterInfo(existingToken, existing.rank());
            }
        }

        String token = userId + ":" + UUID.randomUUID();
        masterRedisTemplate.opsForValue().set(USER_TOKEN_KEY_PREFIX + userId, token, USER_TOKEN_TTL);

        Long seq = masterRedisTemplate.opsForValue().increment(QUEUE_SEQ_KEY);
        masterRedisTemplate.opsForZSet().add(QUEUE_KEY, token, seq);

        Long rank = masterRedisTemplate.opsForZSet().rank(QUEUE_KEY, token);
        return new QueueEnterInfo(token, rank == null ? null : rank + 1);
    }

    public int admitNext(int batchSize) {
        Long admitted = masterRedisTemplate.execute(
                ADMIT_SCRIPT,
                List.of(QUEUE_KEY),
                String.valueOf(batchSize),
                String.valueOf(ALLOWED_TTL.toSeconds()),
                QueueStatus.ALLOWED.name(),
                ALLOWED_KEY_PREFIX
        );
        return admitted == null ? 0 : admitted.intValue();
    }

    public QueueRankInfo getRank(String token) {
        if (masterRedisTemplate.hasKey(ALLOWED_KEY_PREFIX + token)) {
            return new QueueRankInfo(token, null, QueueStatus.ALLOWED, null);
        }

        Long rank = masterRedisTemplate.opsForZSet().rank(QUEUE_KEY, token);
        if (rank == null) {
            return new QueueRankInfo(token, null, QueueStatus.NOT_FOUND, null);
        }

        long position = rank + 1;
        long ticksNeeded = (long) Math.ceil((double) position / admissionBatchSize);
        long estimatedWaitSeconds = ticksNeeded * admissionFixedDelayMs / 1000;
        return new QueueRankInfo(token, position, QueueStatus.WAITING, estimatedWaitSeconds);
    }

    public void verify(String token, Long userId) {
        boolean valid = token != null
                && token.startsWith(userId + ":")
                && Boolean.TRUE.equals(masterRedisTemplate.hasKey(ALLOWED_KEY_PREFIX + token));
        if (!valid) {
            throw new CoreException(ErrorType.FORBIDDEN, "대기열 입장이 확인되지 않았습니다.");
        }
    }

    public void consume(String token) {
        masterRedisTemplate.delete(ALLOWED_KEY_PREFIX + token);
    }
}
