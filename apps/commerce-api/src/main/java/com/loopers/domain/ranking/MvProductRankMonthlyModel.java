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
@Table(name = "mv_product_rank_monthly", uniqueConstraints = {
        @UniqueConstraint(name = "uq_mv_product_rank_monthly_product_period", columnNames = {"product_id", "period_start"})
}, indexes = {
        @Index(name = "idx_mv_product_rank_monthly_period_rank", columnList = "period_start, rank_no")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MvProductRankMonthlyModel extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "rank_no", nullable = false)
    private long rank;

    @Column(name = "score", nullable = false)
    private double score;

    public MvProductRankMonthlyModel(Long productId, LocalDate periodStart, LocalDate periodEnd, long rank, double score) {
        Guard.notNull(productId, "상품 ID는 필수입니다.");
        Guard.notNull(periodStart, "집계 시작일은 필수입니다.");
        Guard.notNull(periodEnd, "집계 종료일은 필수입니다.");
        this.productId = productId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.rank = rank;
        this.score = score;
    }

    public Long getProductId() {
        return productId;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public long getRank() {
        return rank;
    }

    public double getScore() {
        return score;
    }
}
