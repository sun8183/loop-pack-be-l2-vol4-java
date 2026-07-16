package com.loopers.application.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueAdmissionScheduler {

    private final QueueFacade queueFacade;

    @Value("${queue.admission.batch-size}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${queue.admission.fixed-delay-ms}")
    public void admit() {
        int admitted = queueFacade.admitNext(batchSize);
        if (admitted > 0) {
            log.info("대기열 입장 처리 [count={}]", admitted);
        }
    }
}
