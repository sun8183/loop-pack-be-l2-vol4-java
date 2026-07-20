package com.loopers.application.ranking;

import com.loopers.domain.ranking.RankingEntry;

public record RankingInfo(Long productId, long rank, double score) {
    public static RankingInfo from(RankingEntry entry) {
        return new RankingInfo(entry.productId(), entry.rank(), entry.score());
    }
}
