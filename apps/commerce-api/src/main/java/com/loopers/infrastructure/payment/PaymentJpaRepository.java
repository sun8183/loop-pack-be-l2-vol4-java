package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.PaymentModel;
import com.loopers.domain.payment.enums.PaymentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<PaymentModel, Long> {

    List<PaymentModel> findAllByOrderId(Long orderId);

    Optional<PaymentModel> findByTransactionKey(String transactionKey);

    List<PaymentModel> findByStatusInAndCreatedAtBeforeOrderByIdAsc(List<PaymentStatus> statuses, ZonedDateTime before, Pageable pageable);
}