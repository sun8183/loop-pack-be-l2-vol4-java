package com.loopers.domain.coupon;

import java.util.Optional;

public interface CouponIssueResultRepository {
    Optional<CouponIssueResult> findByRequestId(String requestId);
}