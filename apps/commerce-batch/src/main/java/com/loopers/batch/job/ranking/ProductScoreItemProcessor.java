package com.loopers.batch.job.ranking;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProductScoreItemProcessor implements ItemProcessor<ProductMetricRow, ProductScore> {

    @Value("${ranking.weight.order}")
    private double orderWeight;
    @Value("${ranking.weight.like}")
    private double likeWeight;

    @Override
    public ProductScore process(ProductMetricRow row) {
        double score = row.orderCount() * orderWeight + row.likeCount() * likeWeight;
        return new ProductScore(row.productId(), score);
    }
}
