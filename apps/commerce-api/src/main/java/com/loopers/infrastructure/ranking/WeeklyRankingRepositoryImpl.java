package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingEntry;
import com.loopers.domain.ranking.WeeklyRankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Repository
public class WeeklyRankingRepositoryImpl implements WeeklyRankingRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<RankingEntry> getRankings(LocalDate periodStart, long offset, long limit) {
        return jdbcTemplate.query(
                "SELECT product_id, rank_no, score FROM mv_product_rank_weekly " +
                        "WHERE period_start = ? ORDER BY rank_no ASC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new RankingEntry(rs.getLong("product_id"), rs.getLong("rank_no"), rs.getDouble("score")),
                periodStart, limit, offset
        );
    }

    @Override
    public long count(LocalDate periodStart) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mv_product_rank_weekly WHERE period_start = ?",
                Long.class, periodStart
        );
        return count == null ? 0 : count;
    }
}
