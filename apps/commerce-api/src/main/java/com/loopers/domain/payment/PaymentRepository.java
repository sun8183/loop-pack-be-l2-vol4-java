package com.loopers.domain.payment;

import com.loopers.domain.payment.enums.PaymentStatus;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    PaymentModel save(PaymentModel payment);
    Optional<PaymentModel> findById(Long id);
    Optional<PaymentModel> findByTransactionKey(String transactionKey);
    List<PaymentModel> findAllByOrderId(Long orderId);
    List<PaymentModel> findAllByStatusInCreatedBefore(List<PaymentStatus> statuses, ZonedDateTime before, int limit);
}
