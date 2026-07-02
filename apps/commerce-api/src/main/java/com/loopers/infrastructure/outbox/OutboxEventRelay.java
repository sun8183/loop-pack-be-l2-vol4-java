package com.loopers.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventRelay {

    private static final long SEND_TIMEOUT_SECONDS = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${outbox.relay.batch-size}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${outbox.relay.fixed-delay-ms}")
    @Transactional
    public void relay() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findPending(batchSize);
        log.info("주문 outbox 이벤트 스케줄러 진행중");
        for (OutboxEvent event : pendingEvents) {
            try {
                Class<?> payloadType = Class.forName(event.getEventType());
                Object payload = objectMapper.readValue(event.getPayload(), payloadType);
                // 타임아웃에 걸린다면 예외상황으로 빠진다.
                kafkaTemplate.send(event.getTopic(), event.getMessageKey(), payload)
                        .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                event.markSent();
            } catch (Exception e) {
                log.warn("outbox 이벤트 발행 실패 [id={}, eventType={}]: {}", event.getId(), event.getEventType(), e.getMessage());
            }
        }
    }
}
