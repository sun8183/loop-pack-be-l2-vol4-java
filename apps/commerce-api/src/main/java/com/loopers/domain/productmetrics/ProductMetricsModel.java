package com.loopers.domain.productmetrics;

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
@Table(name = "product_metrics", uniqueConstraints = {
        @UniqueConstraint(name = "uq_product_metrics_product_date", columnNames = {"product_id", "base_date"})
}, indexes = {
        @Index(name = "idx_product_metrics_base_date", columnList = "base_date")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductMetricsModel extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;

    @Column(name = "order_count", nullable = false)
    private long orderCount;

    @Column(name = "like_count", nullable = false)
    private long likeCount;

    public ProductMetricsModel(Long productId, LocalDate baseDate) {
        Guard.notNull(productId, "상품 ID는 필수입니다.");
        Guard.notNull(baseDate, "기준 일자는 필수입니다.");
        this.productId = productId;
        this.baseDate = baseDate;
        this.orderCount = 0;
        this.likeCount = 0;
    }

    public Long getProductId() {
        return productId;
    }

    public LocalDate getBaseDate() {
        return baseDate;
    }

    public long getOrderCount() {
        return orderCount;
    }

    public long getLikeCount() {
        return likeCount;
    }
}
