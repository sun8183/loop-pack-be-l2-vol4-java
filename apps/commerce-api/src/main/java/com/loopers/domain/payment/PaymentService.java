package com.loopers.domain.payment;

import com.loopers.domain.order.vo.Money;
import com.loopers.domain.payment.PaymentGatewayClient.Transaction;
import com.loopers.domain.payment.enums.PaymentStatus;
import com.loopers.domain.payment.enums.PgTransactionStatus;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentModel create(Long orderId, Money amount) {
        boolean hasActivePayment = paymentRepository.findAllByOrderId(orderId).stream()
                .anyMatch(PaymentModel::isActive);
        if (hasActivePayment) {
            throw new CoreException(ErrorType.CONFLICT, "이미 진행 중인 결제가 있습니다. 결제 결과를 확인해주세요.");
        }
        return paymentRepository.save(new PaymentModel(orderId, amount));
    }

    @Transactional(readOnly = true)
    public List<PaymentModel> getActiveCreatedBefore(ZonedDateTime before, int limit) {
        return paymentRepository.findAllByStatusInCreatedBefore(
                List.of(PaymentStatus.REQUEST, PaymentStatus.PENDING), before, limit);
    }

    /**
     * transactionKey 없이 REQUEST로 남은 결제를 PG 거래 목록으로 확정한다.
     * 다른 결제가 이미 가진 거래(이전 시도)는 제외하고, SUCCESS > PENDING > FAILED 순으로 하나를 고른다.
     * 고를 거래가 없으면 PG에 접수되지 않은 것으로 보고 실패 처리한다.
     */
    @Transactional
    public PaymentModel reconcileRequested(Long id, List<Transaction> pgTransactions) {
        PaymentModel payment = get(id);
        List<Transaction> candidates = pgTransactions.stream()
                .filter(t -> paymentRepository.findByTransactionKey(t.transactionKey()).isEmpty())
                .toList();
        Optional<Transaction> chosen = candidates.stream().filter(t -> t.status() == PgTransactionStatus.SUCCESS).findFirst()
                .or(() -> candidates.stream().filter(t -> t.status() == PgTransactionStatus.PENDING).findFirst())
                .or(() -> candidates.stream().findFirst());

        chosen.ifPresentOrElse(
                t -> {
                    payment.startProcessing(t.transactionKey());
                    payment.applyCallback(t.status());
                },
                payment::fail
        );
        return payment;
    }

    @Transactional(readOnly = true)
    public PaymentModel get(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[id = " + id + "] 결제를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public PaymentModel getByTransactionKey(String transactionKey) {
        return paymentRepository.findByTransactionKey(transactionKey)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[transactionKey = " + transactionKey + "] 결제를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public List<PaymentModel> getListByOrderId(Long orderId) {
        return paymentRepository.findAllByOrderId(orderId);
    }

    @Transactional
    public PaymentModel applyCallback(String transactionKey, PgTransactionStatus pgStatus) {
        PaymentModel payment = getByTransactionKey(transactionKey);
        payment.applyCallback(pgStatus);
        return payment;
    }

    @Transactional
    public PaymentModel startProcessing(Long id, String transactionKey) {
        PaymentModel payment = get(id);
        payment.startProcessing(transactionKey);
        return payment;
    }

    @Transactional
    public PaymentModel approve(Long id) {
        PaymentModel payment = get(id);
        payment.approve();
        return payment;
    }

    @Transactional
    public PaymentModel fail(Long id) {
        PaymentModel payment = get(id);
        payment.fail();
        return payment;
    }

    @Transactional
    public PaymentModel expire(Long id) {
        PaymentModel payment = get(id);
        payment.expire();
        return payment;
    }
}
