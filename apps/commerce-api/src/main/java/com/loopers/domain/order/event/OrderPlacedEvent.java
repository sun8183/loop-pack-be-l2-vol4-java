package com.loopers.domain.order.event;

public record OrderPlacedEvent(Long orderId, String orderNumber, Long userId, Long totalAmount) {
}