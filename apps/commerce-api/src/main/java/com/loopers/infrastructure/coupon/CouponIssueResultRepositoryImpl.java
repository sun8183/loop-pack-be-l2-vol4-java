package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.CouponIssueResult;
import com.loopers.domain.coupon.CouponIssueResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CouponIssueResultRepositoryImpl implements CouponIssueResultRepository {

    private final CouponIssueResultJpaRepository jpaRepository;

    @Override
    public Optional<CouponIssueResult> findByRequestId(String requestId) {
        return jpaRepository.findByRequestId(requestId);
    }
}