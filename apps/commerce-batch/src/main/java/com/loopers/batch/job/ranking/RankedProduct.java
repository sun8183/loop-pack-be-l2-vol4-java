package com.loopers.batch.job.ranking;

public record RankedProduct(Long productId, long rank, double score) {
}
