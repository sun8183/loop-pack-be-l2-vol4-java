package com.loopers.interfaces.consumer.coupon;

import com.loopers.config.redis.RedisConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
@Component
public class CouponIssueProcessor {

    private static final String ISSUE_RESULT_KEY_PREFIX = "coupon:issue:result:";
    private static final Duration ISSUE_RESULT_TTL = Duration.ofMinutes(30);

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private final RedisTemplate<String, String> masterRedisTemplate;

    public void process(CouponIssueMessage message) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                jdbcTemplate.update(
                        "INSERT INTO user_coupons (user_id, coupon_id, status, created_at, updated_at) VALUES (?, ?, 'ISSUED', NOW(6), NOW(6))",
                        message.userId(), message.couponId()
                );
                int affected = jdbcTemplate.update(
                        "UPDATE coupons SET stock = stock - 1, updated_at = NOW() WHERE id = ? AND stock > 0 AND deleted_at IS NULL",
                        message.couponId()
                );
                if (affected == 0) {
                    throw new RuntimeException("재고 없음");
                }
            });
        } catch (DataIntegrityViolationException e) {
            log.info("쿠폰 발급 실패 - 중복 발급 [requestId={}]", message.requestId());
            saveResult(message, "FAILED");
            return;
        } catch (Exception e) {
            log.info("쿠폰 발급 실패 [requestId={}]: {}", message.requestId(), e.getMessage());
            saveResult(message, "FAILED");
            return;
        }

        log.info("쿠폰 발급 성공 [requestId={}, couponId={}, userId={}]", message.requestId(), message.couponId(), message.userId());
        saveResult(message, "SUCCESS");
    }

    private void saveResult(CouponIssueMessage message, String status) {
        masterRedisTemplate.opsForValue().set(ISSUE_RESULT_KEY_PREFIX + message.requestId(), status, ISSUE_RESULT_TTL);
        jdbcTemplate.update(
                "INSERT INTO coupon_issue_results (request_id, coupon_id, user_id, status, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(6), NOW(6))",
                message.requestId(), message.couponId(), message.userId(), status
        );
    }
}
