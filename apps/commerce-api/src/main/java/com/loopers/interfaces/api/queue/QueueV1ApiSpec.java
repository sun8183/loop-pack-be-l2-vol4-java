package com.loopers.interfaces.api.queue;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Queue V1 API", description = "대기열 API")
public interface QueueV1ApiSpec {

    @Operation(summary = "대기열 입장", description = "대기열에 입장하고 입장 토큰과 현재 순번을 반환합니다.")
    ApiResponse<QueueV1Dto.EnterResponse> enter(Long userId);

    @Operation(summary = "대기열 순번 조회", description = "토큰으로 현재 대기 순번 또는 입장 허용 상태를 조회합니다.")
    ApiResponse<QueueV1Dto.RankResponse> getRank(String token);
}
