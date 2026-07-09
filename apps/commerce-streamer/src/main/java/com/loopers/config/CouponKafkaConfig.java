package com.loopers.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.ByteArrayJsonMessageConverter;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CouponKafkaConfig {

    public static final String COUPON_ISSUE_LISTENER = "COUPON_ISSUE_LISTENER";

    private static final long DLQ_RETRY_INTERVAL_MS = 500L;
    private static final long DLQ_RETRY_COUNT = 2L;

    @Bean
    public NewTopic couponIssueTopic(@Value("${coupon.issue.topic-name}") String topicName) {
        return TopicBuilder.name(topicName).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic couponIssueDeadLetterTopic(@Value("${coupon.issue.topic-name}") String topicName) {
        return TopicBuilder.name(topicName + ".DLT").partitions(1).replicas(1).build();
    }

    @Bean(COUPON_ISSUE_LISTENER)
    public ConcurrentKafkaListenerContainerFactory<Object, Object> couponIssueListenerContainerFactory(
            KafkaProperties kafkaProperties,
            ByteArrayJsonMessageConverter converter,
            KafkaTemplate<Object, Object> kafkaTemplate
    ) {
        Map<String, Object> consumerConfig = new HashMap<>(kafkaProperties.buildConsumerProperties());
        consumerConfig.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);

        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(consumerConfig));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setRecordMessageConverter(converter);
        factory.setConcurrency(1);
        factory.setBatchListener(false);
        factory.setCommonErrorHandler(new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate),
                new FixedBackOff(DLQ_RETRY_INTERVAL_MS, DLQ_RETRY_COUNT)
        ));
        return factory;
    }
}