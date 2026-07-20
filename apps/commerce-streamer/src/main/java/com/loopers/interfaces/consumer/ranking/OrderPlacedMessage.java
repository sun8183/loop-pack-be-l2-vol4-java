package com.loopers.interfaces.consumer.ranking;

public record OrderPlacedMessage(Long orderId, String orderNumber, Long userId, Long totalAmount) {
}
