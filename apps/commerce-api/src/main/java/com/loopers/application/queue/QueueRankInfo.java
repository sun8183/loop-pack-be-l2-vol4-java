package com.loopers.application.queue;

import com.loopers.domain.queue.enums.QueueStatus;

public record QueueRankInfo(String token, Long rank, QueueStatus status, Long estimatedWaitSeconds) {
}
