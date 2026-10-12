package com.loopers.domain.payment;

import com.loopers.domain.order.OrderModel;
import com.loopers.domain.payment.enums.CardType;
import com.loopers.domain.payment.enums.PgTransactionStatus;

import java.util.List;

public interface PaymentGatewayClient {

    Result request(Command command);

    QueryResult query(String transactionKey, Long userId);

    /** PG에 해당 주문번호로 접수된 거래가 없으면 빈 목록 */
    List<Transaction> findByOrderNumber(String orderNumber, Long userId);

    record Command(
            Long userId,
            String orderNumber,
            Long amount,
            CardType cardType,
            String cardNo
    ) {
        public static Command of(Long userId, OrderModel order, CardType cardType, String cardNo) {
            return new Command(userId, order.getOrderNumber(), order.getTotalMoney().getValue(), cardType, cardNo);
        }
    }

    record Result(String transactionKey) {}

    record QueryResult(PgTransactionStatus status) {}

    record Transaction(String transactionKey, PgTransactionStatus status) {}

    /** 요청은 보냈지만 응답을 받지 못해 PG 처리 여부를 알 수 없음 (read timeout 등) */
    class UnknownResultException extends RuntimeException {
        public UnknownResultException(Throwable cause) {
            super("PG 응답을 받지 못해 결제 처리 여부를 알 수 없습니다.", cause);
        }
    }
}
