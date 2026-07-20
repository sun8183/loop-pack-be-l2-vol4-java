package com.loopers.domain.ranking;

import com.loopers.domain.BaseEntity;
import com.loopers.support.Guard;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "product_rank_daily", uniqueConstraints = {
        @UniqueConstraint(name = "uq_product_rank_daily_product_date", columnNames = {"product_id", "base_date"})
}, indexes = {
        @Index(name = "idx_product_rank_daily_date_rank", columnList = "base_date, rank_no")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductRankDailyModel extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;

    @Column(name = "rank_no", nullable = false)
    private long rank;

    @Column(name = "score", nullable = false)
    private double score;

    public ProductRankDailyModel(Long productId, LocalDate baseDate, long rank, double score) {
        Guard.notNull(productId, "상품 ID는 필수입니다.");
        Guard.notNull(baseDate, "기준 일자는 필수입니다.");
        this.productId = productId;
        this.baseDate = baseDate;
        this.rank = rank;
        this.score = score;
    }

    public Long getProductId() {
        return productId;
    }

    public LocalDate getBaseDate() {
        return baseDate;
    }

    public long getRank() {
        return rank;
    }

    public double getScore() {
        return score;
    }
}
