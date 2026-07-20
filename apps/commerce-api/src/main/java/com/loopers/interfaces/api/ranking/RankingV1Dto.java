package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingInfo;

public class RankingV1Dto {

    public record RankingResponse(
            Long productId,
            long rank,
            double score
    ) {
        public static RankingResponse from(RankingInfo info) {
            return new RankingResponse(info.productId(), info.rank(), info.score());
        }
    }

    public record ProductRankResponse(
            Long productId,
            Long rank
    ) {
    }
}
