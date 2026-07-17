package com.loopers.interfaces.consumer.ranking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.confg.kafka.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class OrderMetricsConsumer {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final String CONSUMER_GROUP_ID = "product-metrics-consumer";

    private final OrderMetricsProcessor orderMetricsProcessor;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = {"${order.placed.topic-name}"},
            groupId = CONSUMER_GROUP_ID,
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void handleOrderPlaced(List<ConsumerRecord<Object, Object>> messages, Acknowledgment acknowledgment) throws IOException {
        for (ConsumerRecord<Object, Object> record : messages) {
            OrderPlacedMessage message = objectMapper.readValue((byte[]) record.value(), OrderPlacedMessage.class);
            LocalDate baseDate = Instant.ofEpochMilli(record.timestamp()).atZone(ZONE).toLocalDate();
            orderMetricsProcessor.process(message, baseDate);
        }
        acknowledgment.acknowledge();
    }
}
