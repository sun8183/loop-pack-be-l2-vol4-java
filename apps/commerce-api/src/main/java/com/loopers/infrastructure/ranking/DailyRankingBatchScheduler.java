package com.loopers.infrastructure.ranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class DailyRankingBatchScheduler {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final JdbcTemplate jdbcTemplate;

    @Value("${ranking.weight.order}")
    private double orderWeight;
    @Value("${ranking.weight.like}")
    private double likeWeight;

    @Scheduled(cron = "${ranking.daily-batch.cron}", zone = "Asia/Seoul")
    public void run() {
        LocalDate baseDate = LocalDate.now(ZONE).minusDays(1);

        List<ProductMetric> metrics = jdbcTemplate.query(
                "SELECT product_id, order_count, like_count FROM product_metrics WHERE base_date = ?",
                (rs, rowNum) -> new ProductMetric(
                        rs.getLong("product_id"), rs.getLong("order_count"), rs.getLong("like_count")),
                baseDate
        );

        if (metrics.isEmpty()) {
            fallbackToPreviousDay(baseDate);
            return;
        }

        List<ProductMetric> ranked = metrics.stream()
                .sorted(Comparator.comparingDouble((ProductMetric m) -> score(m)).reversed())
                .toList();

        long rank = 1;
        for (ProductMetric metric : ranked) {
            double score = score(metric);
            jdbcTemplate.update(
                    "INSERT INTO product_rank_daily (product_id, base_date, rank_no, score, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, NOW(6), NOW(6)) " +
                            "ON DUPLICATE KEY UPDATE rank_no = ?, score = ?, updated_at = NOW(6)",
                    metric.productId(), baseDate, rank, score, rank, score
            );
            rank++;
        }

        log.info("일별 랭킹 배치 완료 [baseDate={}, count={}]", baseDate, ranked.size());
    }

    private double score(ProductMetric metric) {
        return metric.orderCount() * orderWeight + metric.likeCount() * likeWeight;
    }

    // product_metrics에 그 날짜 데이터가 아예 없으면(장애 추정) 바로 전날 스냅샷을 그대로 복사해 대체한다.
    private void fallbackToPreviousDay(LocalDate baseDate) {
        LocalDate previousDate = baseDate.minusDays(1);
        int inserted = jdbcTemplate.update(
                "INSERT INTO product_rank_daily (product_id, base_date, rank_no, score, created_at, updated_at) " +
                        "SELECT product_id, ?, rank_no, score, NOW(6), NOW(6) FROM product_rank_daily WHERE base_date = ? " +
                        "ON DUPLICATE KEY UPDATE rank_no = VALUES(rank_no), score = VALUES(score), updated_at = NOW(6)",
                baseDate, previousDate
        );

        if (inserted == 0) {
            log.info("일별 랭킹 배치 스킵 - 데이터 없음 [baseDate={}]", baseDate);
            return;
        }

        log.warn("일별 랭킹 배치 - product_metrics 데이터 없어 전날({}) 스냅샷으로 대체 [baseDate={}]", previousDate, baseDate);
    }

    private record ProductMetric(Long productId, long orderCount, long likeCount) {
    }
}
