package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.PaymentGatewayClient;
import com.loopers.domain.payment.enums.PgTransactionStatus;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class RestTemplatePaymentGatewayClient implements PaymentGatewayClient {

    private final PaymentGatewayProperties properties;
    private final RestTemplate requestTemplate;
    private final RestTemplate queryTemplate;

    public RestTemplatePaymentGatewayClient(PaymentGatewayProperties properties) {
        this.properties = properties;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeoutMs());
        requestFactory.setReadTimeout(properties.requestReadTimeoutMs());
        this.requestTemplate = new RestTemplate(requestFactory);

        SimpleClientHttpRequestFactory queryFactory = new SimpleClientHttpRequestFactory();
        queryFactory.setConnectTimeout(properties.connectTimeoutMs());
        queryFactory.setReadTimeout(properties.queryReadTimeoutMs());
        this.queryTemplate = new RestTemplate(queryFactory);
    }

    @CircuitBreaker(name = "paymentGateway")
    @Override
    public Result request(Command command) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-USER-ID", String.valueOf(command.userId()));

        Map<String, Object> body = Map.of(
                "orderId", command.orderNumber(),
                "cardType", command.cardType().name(),
                "cardNo", command.cardNo(),
                "amount", command.amount(),
                "callbackUrl", properties.callbackUrl()
        );

        PgTransactionResponse response;
        try {
            response = requestTemplate.postForObject(
                    properties.url() + "/api/v1/payments",
                    new HttpEntity<>(body, headers),
                    PgTransactionResponse.class
            );
        } catch (ResourceAccessException e) {
            // connect 실패와 read timeout을 구분하지 않고 결과 불명으로 본다: 실제 미접수여도 대사 시 404로 실패 확정됨
            throw new PaymentGatewayClient.UnknownResultException(e);
        }
        if (response == null || response.data() == null) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "PG 응답이 올바르지 않습니다.");
        }
        return new Result(response.data().transactionKey());
    }

    @Retry(name = "paymentGatewayQuery")
    @CircuitBreaker(name = "paymentGatewayQuery")
    @Override
    public QueryResult query(String transactionKey, Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", String.valueOf(userId));

        PgQueryResponse response = queryTemplate.exchange(
                properties.url() + "/api/v1/payments/" + transactionKey,
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                PgQueryResponse.class
        ).getBody();
        if (response == null || response.data() == null) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "PG 응답이 올바르지 않습니다.");
        }
        return new QueryResult(PgTransactionStatus.valueOf(response.data().status()));
    }

    @Retry(name = "paymentGatewayQuery")
    @CircuitBreaker(name = "paymentGatewayQuery")
    @Override
    public List<Transaction> findByOrderNumber(String orderNumber, Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", String.valueOf(userId));

        PgOrderResponse response;
        try {
            response = queryTemplate.exchange(
                    properties.url() + "/api/v1/payments?orderId={orderId}",
                    org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(headers),
                    PgOrderResponse.class,
                    orderNumber
            ).getBody();
        } catch (HttpClientErrorException.NotFound e) {
            return List.of();
        }
        if (response == null || response.data() == null) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "PG 응답이 올바르지 않습니다.");
        }
        return response.data().transactions().stream()
                .map(t -> new Transaction(t.transactionKey(), PgTransactionStatus.valueOf(t.status())))
                .toList();
    }

    private record PgTransactionResponse(boolean success, PgTransactionData data) {}

    private record PgTransactionData(String transactionKey, String status, String reason) {}

    private record PgQueryResponse(boolean success, PgQueryData data) {}

    private record PgQueryData(String transactionKey, String status, String reason) {}

    private record PgOrderResponse(boolean success, PgOrderData data) {}

    private record PgOrderData(String orderId, List<PgQueryData> transactions) {}
}
