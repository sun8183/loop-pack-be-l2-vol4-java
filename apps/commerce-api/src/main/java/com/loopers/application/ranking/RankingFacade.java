package com.loopers.application.ranking;

import com.loopers.config.redis.RedisConfig;
import com.loopers.domain.ranking.RankingPeriod;
import com.loopers.domain.ranking.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class RankingFacade {

    private final RankingService rankingService;

    public RankingPageResult getTopRankings(Pageable pageable) {
        return new RankingPageResult(
                rankingService.getTop(pageable).stream().map(RankingInfo::from).toList(),
                rankingService.count()
        );
    }

    public Optional<Long> getProductRank(Long productId) {
        return rankingService.getRank(productId);
    }

    @Cacheable(
            cacheNames = RedisConfig.RANKING_HISTORY_CACHE,
            key = "#baseDate + '_' + #period + '_' + #pageable.pageNumber + '_' + #pageable.pageSize"
    )
    public RankingPageResult getRankings(LocalDate baseDate, RankingPeriod period, Pageable pageable) {
        return new RankingPageResult(
                rankingService.getRankings(baseDate, period, pageable).stream().map(RankingInfo::from).toList(),
                rankingService.countRankings(baseDate, period)
        );
    }
}
