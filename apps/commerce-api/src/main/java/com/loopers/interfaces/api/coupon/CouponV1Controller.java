package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RequiredArgsConstructor
@RestController
public class CouponV1Controller implements CouponV1ApiSpec {

    private final CouponFacade couponFacade;

    @PostMapping("/api/v1/coupons/{couponId}/issue")
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public ApiResponse<CouponV1Dto.UserCouponResponse> issueCoupon(
            @PathVariable Long couponId,
            @RequestAttribute("authenticatedUserId") Long userId
    ) {
        return ApiResponse.success(CouponV1Dto.UserCouponResponse.from(couponFacade.issue(couponId, userId)));
    }

    @PostMapping("/api/v1/coupons/{couponId}/issue-async")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Override
    public ApiResponse<CouponV1Dto.IssueAsyncResponse> issueAsyncCoupon(
            @PathVariable Long couponId,
            @RequestAttribute("authenticatedUserId") Long userId
    ) {
        return ApiResponse.success(new CouponV1Dto.IssueAsyncResponse(couponFacade.issueAsync(couponId, userId)));
    }

    @GetMapping("/api/v1/coupons/issue-result/{requestId}")
    @Override
    public ApiResponse<CouponV1Dto.IssueResultResponse> getIssueResult(@PathVariable String requestId) {
        return ApiResponse.success(CouponV1Dto.IssueResultResponse.from(couponFacade.getIssueResult(requestId)));
    }

    @GetMapping("/api/v1/users/me/coupons")
    @Override
    public ApiResponse<List<CouponV1Dto.UserCouponResponse>> getMyCoupons(
            @RequestAttribute("authenticatedUserId") Long userId
    ) {
        return ApiResponse.success(CouponV1Dto.UserCouponResponse.from(couponFacade.getMyList(userId)));
    }
}
