package com.loopers.interfaces.consumer.order;

import com.loopers.confg.kafka.KafkaConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class OrderPlacedConsumer {

    @KafkaListener(
            topics = {"${order.placed.topic-name}"},
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void handleOrderPlaced(List<ConsumerRecord<Object, Object>> messages, Acknowledgment acknowledgment) {
        messages.forEach(record -> log.info("주문 생성 알림톡 발송 [key={}]", record.key()));
        acknowledgment.acknowledge();
    }
}
