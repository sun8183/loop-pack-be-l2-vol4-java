package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponIssueResult;
import com.loopers.domain.coupon.enums.CouponIssueStatus;

public record CouponIssueResultInfo(String requestId, CouponIssueStatus status) {

    public static CouponIssueResultInfo notFound(String requestId) {
        return new CouponIssueResultInfo(requestId, CouponIssueStatus.NOT_FOUND);
    }

    public static CouponIssueResultInfo from(CouponIssueResult result) {
        return new CouponIssueResultInfo(result.getRequestId(), result.getStatus());
    }
}