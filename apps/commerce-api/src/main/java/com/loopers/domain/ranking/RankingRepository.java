package com.loopers.domain.ranking;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RankingRepository {
    List<RankingEntry> getTop(LocalDate baseDate, long offset, long limit);
    long count(LocalDate baseDate);
    Optional<Long> getRank(LocalDate baseDate, Long productId);
}
