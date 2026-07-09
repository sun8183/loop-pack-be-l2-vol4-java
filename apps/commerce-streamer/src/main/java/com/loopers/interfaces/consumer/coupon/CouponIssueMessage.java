package com.loopers.interfaces.consumer.coupon;

public record CouponIssueMessage(String requestId, Long couponId, Long userId) {
}
