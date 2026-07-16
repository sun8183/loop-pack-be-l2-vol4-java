package com.loopers.interfaces.api.queue;

import com.loopers.application.queue.QueueFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class QueueV1Controller implements QueueV1ApiSpec {

    private final QueueFacade queueFacade;

    @PostMapping("/api/v1/queue/enter")
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public ApiResponse<QueueV1Dto.EnterResponse> enter(
            @RequestAttribute("authenticatedUserId") Long userId
    ) {
        return ApiResponse.success(QueueV1Dto.EnterResponse.from(queueFacade.enter(userId)));
    }

    @GetMapping("/api/v1/queue/rank/{token}")
    @Override
    public ApiResponse<QueueV1Dto.RankResponse> getRank(@PathVariable String token) {
        return ApiResponse.success(QueueV1Dto.RankResponse.from(queueFacade.getRank(token)));
    }
}
