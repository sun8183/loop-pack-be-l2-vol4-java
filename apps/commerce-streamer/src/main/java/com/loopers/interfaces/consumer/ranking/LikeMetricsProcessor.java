package com.loopers.interfaces.consumer.ranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class LikeMetricsProcessor {

    private static final String LIKE_RAW_RANKING_KEY_PREFIX = "ranking:like:";
    private static final String COMBINED_RANKING_KEY_PREFIX = "ranking:combined:";
    private static final Duration RAW_RANKING_TTL = Duration.ofHours(48);
    private static final Duration DEDUP_TTL = Duration.ofHours(48);

    private final JdbcTemplate jdbcTemplate;
    private final RankingRedisIdempotencyWriter redisIdempotencyWriter;

    @Value("${ranking.weight.like}")
    private double likeWeight;

    @Transactional
    public void processLiked(Long productId, LocalDate baseDate, String eventKey) {
        process(productId, baseDate, "like:" + eventKey, 1, likeWeight, 1);
        log.info("좋아요 메트릭 반영 완료 [productId={}, baseDate={}]", productId, baseDate);
    }

    @Transactional
    public void processUnliked(Long productId, LocalDate baseDate, String eventKey) {
        process(productId, baseDate, "unlike:" + eventKey, -1, -likeWeight, -1);
        log.info("좋아요 취소 메트릭 반영 완료 [productId={}, baseDate={}]", productId, baseDate);
    }

    private void process(Long productId, LocalDate baseDate, String dedupKey, double rawDelta, double combinedDelta, long likeCountDelta) {
        boolean redisApplied = redisIdempotencyWriter.applyIfAbsent(
                "dedup:redis:" + dedupKey,
                LIKE_RAW_RANKING_KEY_PREFIX + baseDate,
                COMBINED_RANKING_KEY_PREFIX + baseDate,
                List.of(productId),
                rawDelta, combinedDelta,
                DEDUP_TTL, RAW_RANKING_TTL
        );
        if (!redisApplied) {
            log.info("이미 처리된 이벤트, Redis 반영 스킵 [dedupKey={}]", dedupKey);
        }

        int inserted = jdbcTemplate.update(
                "INSERT IGNORE INTO ranking_event_log (event_key, created_at, updated_at) VALUES (?, NOW(6), NOW(6))",
                "dedup:db:" + dedupKey
        );
        if (inserted == 0) {
            log.info("이미 처리된 이벤트, DB 반영 스킵 [dedupKey={}]", dedupKey);
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO product_metrics (product_id, base_date, order_count, like_count, created_at, updated_at) " +
                        "VALUES (?, ?, 0, GREATEST(?, 0), NOW(6), NOW(6)) " +
                        "ON DUPLICATE KEY UPDATE like_count = GREATEST(like_count + ?, 0), updated_at = NOW(6)",
                productId, baseDate, likeCountDelta, likeCountDelta
        );
    }
}
