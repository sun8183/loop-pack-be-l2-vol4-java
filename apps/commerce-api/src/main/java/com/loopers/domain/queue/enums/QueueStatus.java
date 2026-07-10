package com.loopers.domain.queue.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QueueStatus {
    WAITING("대기중"),
    ALLOWED("입장 허용"),
    NOT_FOUND("결과 없음");

    private final String description;
}
