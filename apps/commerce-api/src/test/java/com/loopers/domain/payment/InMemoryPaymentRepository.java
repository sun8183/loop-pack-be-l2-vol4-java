package com.loopers.domain.payment;

import com.loopers.domain.payment.enums.PaymentStatus;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class InMemoryPaymentRepository implements PaymentRepository {

    private final List<PaymentModel> store = new ArrayList<>();

    @Override
    public PaymentModel save(PaymentModel payment) {
        store.add(payment);
        return payment;
    }

    @Override
    public Optional<PaymentModel> findById(Long id) {
        return store.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst();
    }

    @Override
    public Optional<PaymentModel> findByTransactionKey(String transactionKey) {
        return store.stream()
                .filter(p -> transactionKey.equals(p.getTransactionKey()))
                .findFirst();
    }

    @Override
    public List<PaymentModel> findAllByOrderId(Long orderId) {
        return store.stream()
                .filter(p -> p.getOrderId().equals(orderId))
                .toList();
    }

    @Override
    public List<PaymentModel> findAllByStatusInCreatedBefore(List<PaymentStatus> statuses, ZonedDateTime before, int limit) {
        return store.stream()
                .filter(p -> statuses.contains(p.getStatus()))
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isBefore(before))
                .limit(limit)
                .toList();
    }
}
