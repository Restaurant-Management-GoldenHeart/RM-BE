package org.example.goldenheartrestaurant.modules.paymentgateway.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.config.PayOsProperties;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.modules.paymentgateway.dto.payos.PayOsCancelPaymentLinkRequest;
import org.example.goldenheartrestaurant.modules.paymentgateway.dto.payos.PayOsCreatePaymentLinkRequest;
import org.example.goldenheartrestaurant.modules.paymentgateway.dto.payos.PayOsPaymentLinkData;
import org.example.goldenheartrestaurant.modules.paymentgateway.dto.payos.PayOsResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PayOsClient {

    private final PayOsProperties payOsProperties;

    public PayOsResponse<PayOsPaymentLinkData> createPaymentLink(PayOsCreatePaymentLinkRequest request) {
        return executePost("/v2/payment-requests", request);
    }

    public PayOsResponse<PayOsPaymentLinkData> getPaymentLink(Object identifier) {
        return executeGet("/v2/payment-requests/" + identifier);
    }

    public PayOsResponse<PayOsPaymentLinkData> cancelPaymentLink(Object identifier, String reason) {
        return executePost(
                "/v2/payment-requests/" + identifier + "/cancel",
                new PayOsCancelPaymentLinkRequest(reason)
        );
    }

    public PayOsResponse<Map<String, Object>> confirmWebhook(String webhookUrl) {
        return executePost(
                "/confirm-webhook",
                Map.of("webhookUrl", webhookUrl),
                new org.springframework.core.ParameterizedTypeReference<>() {}
        );
    }

    private PayOsResponse<PayOsPaymentLinkData> executePost(String path, Object body) {
        return executePost(
                path,
                body,
                new org.springframework.core.ParameterizedTypeReference<>() {}
        );
    }

    private <T> PayOsResponse<T> executePost(String path,
                                             Object body,
                                             org.springframework.core.ParameterizedTypeReference<PayOsResponse<T>> responseType) {
        try {
            RestClient.RequestBodySpec request = buildRestClient().post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON);

            if (StringUtils.hasText(payOsProperties.getPartnerCode())) {
                request.header("x-partner-code", payOsProperties.getPartnerCode().trim());
            }

            return request
                    .body(body)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientResponseException exception) {
            throw new ConflictException("payOS request failed: " + exception.getResponseBodyAsString());
        }
    }

    private PayOsResponse<PayOsPaymentLinkData> executeGet(String path) {
        try {
            return buildRestClient().get()
                    .uri(path)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<>() {});
        } catch (RestClientResponseException exception) {
            throw new ConflictException("payOS request failed: " + exception.getResponseBodyAsString());
        }
    }

    private RestClient buildRestClient() {
        return RestClient.builder()
                .baseUrl(payOsProperties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("x-client-id", payOsProperties.getClientId())
                .defaultHeader("x-api-key", payOsProperties.getApiKey())
                .build();
    }
}
