package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.coupon.enums.CouponIssueStatus;
import com.loopers.support.Guard;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "coupon_issue_results")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class CouponIssueResult extends BaseEntity {

    @Column(nullable = false, unique = true, length = 36)
    private String requestId;

    @Column(nullable = false)
    private Long couponId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponIssueStatus status;

    public CouponIssueResult(String requestId, Long couponId, Long userId, CouponIssueStatus status) {
        Guard.notBlank(requestId, "요청 ID는 필수입니다.");
        Guard.notNull(couponId, "쿠폰 ID는 필수입니다.");
        Guard.notNull(userId, "유저 ID는 필수입니다.");
        Guard.notNull(status, "상태는 필수입니다.");
        this.requestId = requestId;
        this.couponId = couponId;
        this.userId = userId;
        this.status = status;
    }
}