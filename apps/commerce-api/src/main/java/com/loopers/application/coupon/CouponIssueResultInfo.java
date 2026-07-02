package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponIssueResult;
import com.loopers.domain.coupon.enums.CouponIssueStatus;

public record CouponIssueResultInfo(String requestId, CouponIssueStatus status) {

    public static CouponIssueResultInfo pending(String requestId) {
        return new CouponIssueResultInfo(requestId, CouponIssueStatus.PENDING);
    }

    public static CouponIssueResultInfo from(CouponIssueResult result) {
        return new CouponIssueResultInfo(result.getRequestId(), result.getStatus());
    }
}