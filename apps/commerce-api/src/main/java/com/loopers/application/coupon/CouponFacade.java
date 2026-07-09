package com.loopers.application.coupon;

import com.loopers.config.redis.RedisConfig;
import com.loopers.domain.coupon.CouponIssueResultRepository;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.UserCouponService;
import com.loopers.domain.coupon.enums.CouponIssueStatus;
import com.loopers.domain.coupon.enums.CouponType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class CouponFacade {

    static final String ISSUE_RESULT_KEY_PREFIX = "coupon:issue:result:";
    static final Duration ISSUE_RESULT_TTL = Duration.ofMinutes(30);

    private final UserCouponService userCouponService;
    private final CouponService couponService;
    private final CouponIssueResultRepository couponIssueResultRepository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final RedisTemplate<String, String> defaultRedisTemplate;
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private final RedisTemplate<String, String> masterRedisTemplate;

    @Value("${coupon.issue.topic-name}")
    private String couponIssueTopic;

    public UserCouponInfo issue(Long couponId, Long userId) {
        return UserCouponInfo.from(userCouponService.issue(couponId, userId));
    }

    public List<UserCouponInfo> getMyList(Long userId) {
        return UserCouponInfo.from(userCouponService.getMyList(userId));
    }

    public CouponInfo create(String name, CouponType type, Long value, Long minOrderAmount, ZonedDateTime expiredAt, Long stock) {
        return CouponInfo.from(couponService.create(name, type, value, minOrderAmount, expiredAt, stock));
    }

    public String issueAsync(Long couponId, Long userId) {
        String requestId = UUID.randomUUID().toString();
        masterRedisTemplate.opsForValue().set(ISSUE_RESULT_KEY_PREFIX + requestId, CouponIssueStatus.PENDING.name(), ISSUE_RESULT_TTL);
        kafkaTemplate.send(couponIssueTopic, requestId, new CouponIssueMessage(requestId, couponId, userId));
        return requestId;
    }

    public CouponIssueResultInfo getIssueResult(String requestId) {
        String redisValue = defaultRedisTemplate.opsForValue().get(ISSUE_RESULT_KEY_PREFIX + requestId);
        if (redisValue != null) {
            return new CouponIssueResultInfo(requestId, CouponIssueStatus.valueOf(redisValue));
        }
        return couponIssueResultRepository.findByRequestId(requestId)
                .map(CouponIssueResultInfo::from)
                .orElse(CouponIssueResultInfo.notFound(requestId));
    }

    public Page<CouponInfo> getList(Pageable pageable) {
        return couponService.getList(pageable).map(CouponInfo::from);
    }

    public CouponInfo get(Long couponId) {
        return CouponInfo.from(couponService.get(couponId));
    }

    public CouponInfo update(Long couponId, String name, ZonedDateTime expiredAt) {
        return CouponInfo.from(couponService.update(couponId, name, expiredAt));
    }

    public void delete(Long couponId) {
        couponService.delete(couponId);
    }

    public Page<UserCouponInfo> getIssues(Long couponId, Pageable pageable) {
        return userCouponService.getListByCouponId(couponId, pageable)
                .map(UserCouponInfo::from);
    }
}
