package com.loopers.application.payment;

import com.loopers.domain.order.OrderModel;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.PaymentGatewayClient;
import com.loopers.domain.payment.PaymentModel;
import com.loopers.domain.payment.PaymentPolicy;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.enums.CardType;
import com.loopers.domain.payment.enums.PaymentStatus;
import com.loopers.domain.payment.enums.PgTransactionStatus;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentFacade {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final PaymentPolicy paymentPolicy;
    private final PaymentGatewayClient paymentGatewayClient;
    private final TransactionTemplate transactionTemplate;

    public PaymentInfo createPayment(String orderNumber, Long userId, CardType cardType, String cardNo) {
        OrderModel order = orderService.getByOrderNumberAndUserId(orderNumber, userId);
        paymentPolicy.validatePayable(order, ZonedDateTime.now());

        PaymentModel payment = paymentService.create(order.getId(), order.getTotalMoney());

        try {
            PaymentGatewayClient.Result result = paymentGatewayClient.request(
                    PaymentGatewayClient.Command.of(userId, order, cardType, cardNo)
            );
            // PENDING 커밋
            paymentService.startProcessing(payment.getId(), result.transactionKey());
        } catch (CallNotPermittedException e) {
            log.warn("PG 서킷 오픈 [paymentId={}]", payment.getId());
            paymentService.fail(payment.getId());
            throw new CoreException(ErrorType.SERVICE_UNAVAILABLE, "PG 일시 점검 중입니다. 잠시 후 다시 시도해주세요.");
        } catch (PaymentGatewayClient.UnknownResultException e) {
            // PG가 이미 승인했을 수 있어 실패로 확정하지 않는다. REQUEST로 두고 대사 스케줄러가 주문번호로 확정
            log.warn("PG 결제 결과 불명, 대사 대기 [paymentId={}]: {}", payment.getId(), e.getMessage());
        } catch (Exception e) {
            log.warn("PG 결제 요청 실패 [paymentId={}]: {}", payment.getId(), e.getMessage());
            paymentService.fail(payment.getId());
        }

        return PaymentInfo.from(paymentService.get(payment.getId()));
    }

    public PaymentInfo syncPayment(String transactionKey, Long userId) {
        PaymentModel payment = paymentService.getByTransactionKey(transactionKey);
        orderService.getByUser(payment.getOrderId(), userId);
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return PaymentInfo.from(payment);
        }
        try {
            PaymentGatewayClient.QueryResult result = paymentGatewayClient.query(transactionKey, userId);
            PaymentModel updated = transactionTemplate.execute(status -> {
                PaymentModel applied = paymentService.applyCallback(transactionKey, result.status());
                if (applied.getStatus() == PaymentStatus.APPROVED) {
                    orderService.complete(applied.getOrderId());
                }
                return applied;
            });
            return PaymentInfo.from(updated);
        } catch (CallNotPermittedException e) {
            log.warn("PG 서킷 오픈 - 상태 조회 불가 [transactionKey={}]", transactionKey);
            throw new CoreException(ErrorType.SERVICE_UNAVAILABLE, "PG 일시 점검 중입니다. 잠시 후 다시 시도해주세요.");
        } catch (Exception e) {
            log.warn("PG 상태 조회 실패 [transactionKey={}]: {}", transactionKey, e.getMessage());
            throw new CoreException(ErrorType.INTERNAL_ERROR, "PG 상태 조회에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    /**
     * 콜백·응답을 받지 못하고 남은 결제를 PG 조회로 확정한다.
     * grace 이전에 생성된 건만 대상으로 해, PG가 아직 저장하지 않은 요청을 미접수(404)로 오판하지 않게 한다.
     */
    public int reconcileStalePayments(Duration grace, int limit) {
        List<PaymentModel> stalePayments = paymentService.getActiveCreatedBefore(ZonedDateTime.now().minus(grace), limit);
        int reconciled = 0;
        for (PaymentModel payment : stalePayments) {
            try {
                OrderModel order = orderService.get(payment.getOrderId());
                PaymentModel result = payment.getStatus() == PaymentStatus.REQUEST
                        ? reconcileRequested(payment, order)
                        : reconcilePending(payment, order);
                if (!result.isActive()) {
                    reconciled++;
                }
            } catch (Exception e) {
                log.warn("결제 대사 실패 [paymentId={}]: {}", payment.getId(), e.getMessage());
            }
        }
        return reconciled;
    }

    private PaymentModel reconcileRequested(PaymentModel payment, OrderModel order) {
        List<PaymentGatewayClient.Transaction> transactions =
                paymentGatewayClient.findByOrderNumber(order.getOrderNumber(), order.getUserId());
        return transactionTemplate.execute(status -> completeOrderIfApproved(
                paymentService.reconcileRequested(payment.getId(), transactions)));
    }

    private PaymentModel reconcilePending(PaymentModel payment, OrderModel order) {
        PaymentGatewayClient.QueryResult result = paymentGatewayClient.query(payment.getTransactionKey(), order.getUserId());
        return transactionTemplate.execute(status -> completeOrderIfApproved(
                paymentService.applyCallback(payment.getTransactionKey(), result.status())));
    }

    private PaymentModel completeOrderIfApproved(PaymentModel payment) {
        if (payment.getStatus() == PaymentStatus.APPROVED) {
            orderService.complete(payment.getOrderId());
        }
        return payment;
    }

    @Transactional
    public void handleCallback(String transactionKey, PgTransactionStatus pgStatus) {
        PaymentModel payment = paymentService.applyCallback(transactionKey, pgStatus);
        if (payment.getStatus() == PaymentStatus.APPROVED) {
            orderService.complete(payment.getOrderId());
        }
    }

}
