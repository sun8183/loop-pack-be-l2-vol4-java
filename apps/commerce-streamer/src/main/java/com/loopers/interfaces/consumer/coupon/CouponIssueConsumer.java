package com.loopers.interfaces.consumer.coupon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.config.CouponKafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
public class CouponIssueConsumer {

    private final CouponIssueProcessor couponIssueProcessor;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = {"${coupon.issue.topic-name}"},
            containerFactory = CouponKafkaConfig.COUPON_ISSUE_LISTENER
    )
    public void handleCouponIssue(ConsumerRecord<Object, Object> record, Acknowledgment acknowledgment) throws IOException {
        CouponIssueMessage message = objectMapper.readValue((byte[]) record.value(), CouponIssueMessage.class);
        couponIssueProcessor.process(message);
        acknowledgment.acknowledge();
    }
}
