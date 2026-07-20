package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingEntry;
import com.loopers.domain.ranking.RankingHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Repository
public class RankingHistoryRepositoryImpl implements RankingHistoryRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<RankingEntry> getRankings(LocalDate baseDate, long offset, long limit) {
        return jdbcTemplate.query(
                "SELECT product_id, rank_no, score FROM product_rank_daily " +
                        "WHERE base_date = ? ORDER BY rank_no ASC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new RankingEntry(rs.getLong("product_id"), rs.getLong("rank_no"), rs.getDouble("score")),
                baseDate, limit, offset
        );
    }

    @Override
    public long count(LocalDate baseDate) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product_rank_daily WHERE base_date = ?",
                Long.class, baseDate
        );
        return count == null ? 0 : count;
    }
}
