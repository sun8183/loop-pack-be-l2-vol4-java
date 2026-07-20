package com.loopers.infrastructure.event;

import com.loopers.domain.wishlist.event.ProductLikedEvent;
import com.loopers.domain.wishlist.event.ProductUnlikedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class WishlistEventPublisher {

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    @Value("${product.liked.topic-name}")
    private String productLikedTopic;

    @Value("${product.unliked.topic-name}")
    private String productUnlikedTopic;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductLiked(ProductLikedEvent event) {
        send(productLikedTopic, event.productId(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductUnliked(ProductUnlikedEvent event) {
        send(productUnlikedTopic, event.productId(), event);
    }

    private void send(String topic, Long productId, Object event) {
        kafkaTemplate.send(topic, String.valueOf(productId), event)
                .exceptionally(e -> {
                    log.warn("좋아요 이벤트 발행 실패 [topic={}, productId={}]: {}", topic, productId, e.getMessage());
                    return null;
                });
    }
}
