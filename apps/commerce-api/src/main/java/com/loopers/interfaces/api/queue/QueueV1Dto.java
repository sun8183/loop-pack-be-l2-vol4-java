package com.loopers.interfaces.api.queue;

import com.loopers.application.queue.QueueEnterInfo;
import com.loopers.application.queue.QueueRankInfo;

public class QueueV1Dto {

    public record EnterResponse(String token, Long rank) {
        public static EnterResponse from(QueueEnterInfo info) {
            return new EnterResponse(info.token(), info.rank());
        }
    }

    public record RankResponse(String token, Long rank, String status, Long estimatedWaitSeconds) {
        public static RankResponse from(QueueRankInfo info) {
            return new RankResponse(info.token(), info.rank(), info.status().name(), info.estimatedWaitSeconds());
        }
    }
}
