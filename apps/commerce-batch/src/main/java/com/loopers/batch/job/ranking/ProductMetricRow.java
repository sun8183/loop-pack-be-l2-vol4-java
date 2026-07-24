package com.loopers.batch.job.ranking;

public record ProductMetricRow(Long productId, long orderCount, long likeCount) {
}
