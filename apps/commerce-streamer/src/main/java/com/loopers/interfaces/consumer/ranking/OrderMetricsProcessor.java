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
public class OrderMetricsProcessor {

    private static final String ORDER_RAW_RANKING_KEY_PREFIX = "ranking:order:";
    private static final String COMBINED_RANKING_KEY_PREFIX = "ranking:combined:";
    private static final Duration RAW_RANKING_TTL = Duration.ofHours(48);
    private static final Duration DEDUP_TTL = Duration.ofHours(48);

    private final JdbcTemplate jdbcTemplate;
    private final RankingRedisIdempotencyWriter redisIdempotencyWriter;

    @Value("${ranking.weight.order}")
    private double orderWeight;

    @Transactional
    public void process(OrderPlacedMessage message, LocalDate baseDate) {
        List<Long> productIds = jdbcTemplate.queryForList(
                "SELECT DISTINCT product_id FROM order_items WHERE order_id = ?",
                Long.class, message.orderId()
        );
        if (productIds.isEmpty()) {
            return;
        }

        String dedupKey = "order:" + message.orderId();

        boolean redisApplied = redisIdempotencyWriter.applyIfAbsent(
                "dedup:redis:" + dedupKey,
                ORDER_RAW_RANKING_KEY_PREFIX + baseDate,
                COMBINED_RANKING_KEY_PREFIX + baseDate,
                productIds,
                1, orderWeight,
                DEDUP_TTL, RAW_RANKING_TTL
        );
        if (!redisApplied) {
            log.info("이미 처리된 주문, Redis 반영 스킵 [orderId={}]", message.orderId());
        }

        int inserted = jdbcTemplate.update(
                "INSERT IGNORE INTO ranking_event_log (event_key, created_at, updated_at) VALUES (?, NOW(6), NOW(6))",
                "dedup:db:" + dedupKey
        );
        if (inserted == 0) {
            log.info("이미 처리된 주문, DB 반영 스킵 [orderId={}]", message.orderId());
            return;
        }
        for (Long productId : productIds) {
            jdbcTemplate.update(
                    "INSERT INTO product_metrics (product_id, base_date, order_count, like_count, created_at, updated_at) " +
                            "VALUES (?, ?, 1, 0, NOW(6), NOW(6)) " +
                            "ON DUPLICATE KEY UPDATE order_count = order_count + 1, updated_at = NOW(6)",
                    productId, baseDate
            );
        }

        log.info("주문 메트릭 반영 완료 [orderId={}, productIds={}]", message.orderId(), productIds);
    }
}
