package com.loopers.domain.outbox.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OutboxEventStatus {
    PENDING("대기중"),
    SENT("전송 완료");

    private final String description;
}
