package com.loopers.domain.ranking;

import java.time.LocalDate;
import java.util.List;

public interface RankingHistoryRepository {
    List<RankingEntry> getRankings(LocalDate baseDate, long offset, long limit);
    long count(LocalDate baseDate);
}
