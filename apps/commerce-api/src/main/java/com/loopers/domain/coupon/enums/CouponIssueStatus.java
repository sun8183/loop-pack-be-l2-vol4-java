package com.loopers.domain.coupon.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CouponIssueStatus {
    PENDING("대기중"),
    SUCCESS("성공"),
    FAILED("실패");

    private final String description;
}