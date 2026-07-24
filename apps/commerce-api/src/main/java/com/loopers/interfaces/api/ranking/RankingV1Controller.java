package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingFacade;
import com.loopers.application.ranking.RankingPageResult;
import com.loopers.domain.ranking.RankingPeriod;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RequiredArgsConstructor
@RestController
public class RankingV1Controller implements RankingV1ApiSpec {

    private final RankingFacade rankingFacade;

    @GetMapping("/api/v1/rankings")
    @Override
    public ApiResponse<PageResponse<RankingV1Dto.RankingResponse>> getRankings(
            @RequestParam @DateTimeFormat(pattern = "yyyyMMdd") LocalDate date,
            @RequestParam(defaultValue = "DAILY") RankingPeriod period,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        RankingPageResult result = rankingFacade.getRankings(date, period, pageable);
        return ApiResponse.success(PageResponse.from(
                result.content().stream().map(RankingV1Dto.RankingResponse::from).toList(),
                pageable, result.totalElements()
        ));
    }

    @GetMapping("/api/v1/rankings/top")
    @Override
    public ApiResponse<PageResponse<RankingV1Dto.RankingResponse>> getTopRankings(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        RankingPageResult result = rankingFacade.getTopRankings(pageable);
        return ApiResponse.success(PageResponse.from(
                result.content().stream().map(RankingV1Dto.RankingResponse::from).toList(),
                pageable, result.totalElements()
        ));
    }

    @GetMapping("/api/v1/products/{productId}/rank")
    @Override
    public ApiResponse<RankingV1Dto.ProductRankResponse> getProductRank(@PathVariable Long productId) {
        Long rank = rankingFacade.getProductRank(productId).orElse(null);
        return ApiResponse.success(new RankingV1Dto.ProductRankResponse(productId, rank));
    }
}
