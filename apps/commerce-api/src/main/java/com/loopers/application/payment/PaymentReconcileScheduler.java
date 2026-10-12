package com.loopers.application.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconcileScheduler {

    private final PaymentFacade paymentFacade;

    @Value("${payment.reconcile.batch-size}")
    private int batchSize;

    @Value("${payment.reconcile.grace-seconds}")
    private long graceSeconds;

    @Scheduled(fixedDelayString = "${payment.reconcile.fixed-delay-ms}")
    public void reconcile() {
        int reconciled = paymentFacade.reconcileStalePayments(Duration.ofSeconds(graceSeconds), batchSize);
        if (reconciled > 0) {
            log.info("결제 대사 처리 [count={}]", reconciled);
        }
    }
}
