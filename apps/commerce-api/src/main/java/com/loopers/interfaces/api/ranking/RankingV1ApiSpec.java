package com.loopers.interfaces.api.ranking;

import com.loopers.domain.ranking.RankingPeriod;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

@Tag(name = "Ranking V1 API", description = "랭킹 API")
public interface RankingV1ApiSpec {

    @Operation(
            summary = "상품 랭킹 조회",
            description = "특정 일자 기준으로 일간/주간/월간 상품 랭킹을 페이지 단위로 조회합니다. " +
                    "주간은 date가 속한 ISO 주(월~일), 월간은 date가 속한 달의 랭킹을 반환합니다."
    )
    ApiResponse<PageResponse<RankingV1Dto.RankingResponse>> getRankings(LocalDate date, RankingPeriod period, Pageable pageable);

    @Operation(summary = "실시간 상품 랭킹 Top 조회", description = "현재 실시간 랭킹을 페이지 단위로 조회합니다.")
    ApiResponse<PageResponse<RankingV1Dto.RankingResponse>> getTopRankings(Pageable pageable);

    @Operation(summary = "상품 랭킹 순위 조회", description = "특정 상품의 현재 실시간 랭킹 순위를 조회합니다.")
    ApiResponse<RankingV1Dto.ProductRankResponse> getProductRank(Long productId);
}
