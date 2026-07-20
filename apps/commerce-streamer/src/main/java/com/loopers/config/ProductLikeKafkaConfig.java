package com.loopers.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class ProductLikeKafkaConfig {

    @Bean
    public NewTopic productLikedTopic(@Value("${product.liked.topic-name}") String topicName) {
        return TopicBuilder.name(topicName).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic productUnlikedTopic(@Value("${product.unliked.topic-name}") String topicName) {
        return TopicBuilder.name(topicName).partitions(1).replicas(1).build();
    }
}
