package com.loopers.batch.job.ranking;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Component
public class MvRankRepository {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void replaceAll(MvRankTable table, LocalDate periodStart, LocalDate periodEnd, List<RankedProduct> ranked) {
        jdbcTemplate.update("DELETE FROM " + table.tableName() + " WHERE period_start = ?", periodStart);
        if (ranked.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(
                "INSERT INTO " + table.tableName() +
                        " (product_id, period_start, period_end, rank_no, score, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(6), NOW(6))",
                ranked,
                ranked.size(),
                (ps, item) -> {
                    ps.setLong(1, item.productId());
                    ps.setObject(2, periodStart);
                    ps.setObject(3, periodEnd);
                    ps.setLong(4, item.rank());
                    ps.setDouble(5, item.score());
                }
        );
    }
}
