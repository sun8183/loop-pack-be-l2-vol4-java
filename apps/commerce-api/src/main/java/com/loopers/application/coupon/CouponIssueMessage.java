package com.loopers.application.coupon;

public record CouponIssueMessage(String requestId, Long couponId, Long userId) {
}