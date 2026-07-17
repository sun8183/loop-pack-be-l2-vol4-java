package com.loopers.infrastructure.ranking;

import com.loopers.config.redis.RedisConfig;
import com.loopers.domain.ranking.RankingEntry;
import com.loopers.domain.ranking.RankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
@Repository
public class RankingRepositoryImpl implements RankingRepository {

    private static final String COMBINED_RANKING_KEY_PREFIX = "ranking:combined:";

    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private final RedisTemplate<String, String> masterRedisTemplate;

    @Override
    public List<RankingEntry> getTop(LocalDate baseDate, long offset, long limit) {
        Set<ZSetOperations.TypedTuple<String>> tuples = masterRedisTemplate.opsForZSet()
                .reverseRangeWithScores(key(baseDate), offset, offset + limit - 1);
        if (tuples == null) {
            return List.of();
        }

        List<RankingEntry> entries = new ArrayList<>();
        long rank = offset + 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            entries.add(new RankingEntry(Long.valueOf(tuple.getValue()), rank++, tuple.getScore()));
        }
        return entries;
    }

    @Override
    public long count(LocalDate baseDate) {
        Long size = masterRedisTemplate.opsForZSet().zCard(key(baseDate));
        return size == null ? 0 : size;
    }

    @Override
    public Optional<Long> getRank(LocalDate baseDate, Long productId) {
        Long rank = masterRedisTemplate.opsForZSet().reverseRank(key(baseDate), String.valueOf(productId));
        return Optional.ofNullable(rank).map(r -> r + 1);
    }

    private String key(LocalDate baseDate) {
        return COMBINED_RANKING_KEY_PREFIX + baseDate;
    }
}
