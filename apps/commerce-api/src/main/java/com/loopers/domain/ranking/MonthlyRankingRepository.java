package com.loopers.domain.ranking;

import java.time.LocalDate;
import java.util.List;

public interface MonthlyRankingRepository {
    List<RankingEntry> getRankings(LocalDate periodStart, long offset, long limit);
    long count(LocalDate periodStart);
}
