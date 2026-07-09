package com.loopers.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
        log.info("주문 outbox 이벤트 스케줄러 시작 [count={}]", pendingEvents.size());

        // 전송은 전부 먼저 날리고(논블로킹), 결과 확인만 순서대로 함
        // → worst case가 N x 타임아웃이 아니라 가장 느린 건 1개 분량으로 줄어듦
        List<Map.Entry<OutboxEvent, CompletableFuture<SendResult<Object, Object>>>> sendings = new ArrayList<>();
        for (OutboxEvent event : pendingEvents) {
            try {
                Class<?> payloadType = Class.forName(event.getEventType());
                Object payload = objectMapper.readValue(event.getPayload(), payloadType);
                sendings.add(Map.entry(event, kafkaTemplate.send(event.getTopic(), event.getMessageKey(), payload)));
            } catch (Exception e) {
                log.warn("outbox 이벤트 발행 실패 [id={}, eventType={}]: {}", event.getId(), event.getEventType(), e.getMessage());
            }
        }

        for (Map.Entry<OutboxEvent, CompletableFuture<SendResult<Object, Object>>> entry : sendings) {
            OutboxEvent event = entry.getKey();
            try {
                // 타임아웃에 걸린다면 예외상황으로 빠진다.
                entry.getValue().get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                event.markSent();
            } catch (Exception e) {
                log.warn("outbox 이벤트 발행 실패 [id={}, eventType={}]: {}", event.getId(), event.getEventType(), e.getMessage());
            }
        }
        log.info("주문 outbox 이벤트 스케줄러 완료");
    }
}
