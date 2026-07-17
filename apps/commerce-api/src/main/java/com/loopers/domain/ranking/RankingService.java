package com.loopers.domain.ranking;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class RankingService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final int TOP_RANK_SIZE = 100;

    private final RankingRepository rankingRepository;
    private final RankingHistoryRepository rankingHistoryRepository;

    public List<RankingEntry> getTop(Pageable pageable) {
        long offset = (long) pageable.getPageNumber() * pageable.getPageSize();
        if (offset >= TOP_RANK_SIZE) {
            return List.of();
        }
        long limit = Math.min(pageable.getPageSize(), TOP_RANK_SIZE - offset);
        return rankingRepository.getTop(resolveBaseDate(), offset, limit);
    }

    public long count() {
        return Math.min(rankingRepository.count(resolveBaseDate()), TOP_RANK_SIZE);
    }

    public Optional<Long> getRank(Long productId) {
        return rankingRepository.getRank(resolveBaseDate(), productId);
    }

    // 오늘자 랭킹이 아직 비어있으면(콜드스타트) 어제자로 대체한다.
    private LocalDate resolveBaseDate() {
        LocalDate today = LocalDate.now(ZONE);
        return rankingRepository.count(today) > 0 ? today : today.minusDays(1);
    }

    public List<RankingEntry> getRankings(LocalDate baseDate, Pageable pageable) {
        validateHistoricalDate(baseDate);
        long offset = (long) pageable.getPageNumber() * pageable.getPageSize();
        return rankingHistoryRepository.getRankings(baseDate, offset, pageable.getPageSize());
    }

    public long countRankings(LocalDate baseDate) {
        validateHistoricalDate(baseDate);
        return rankingHistoryRepository.count(baseDate);
    }

    private void validateHistoricalDate(LocalDate baseDate) {
        if (!baseDate.isBefore(LocalDate.now(ZONE))) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                    "오늘 이후 날짜는 조회할 수 없습니다. 실시간 랭킹은 /api/v1/rankings/top을 이용하세요.");
        }
    }
}
