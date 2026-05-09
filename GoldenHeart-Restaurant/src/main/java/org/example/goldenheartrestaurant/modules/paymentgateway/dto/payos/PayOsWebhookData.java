package org.example.goldenheartrestaurant.modules.paymentgateway.dto.payos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PayOsWebhookData(
        Long orderCode,
        Integer amount,
        String description,
        String accountNumber,
        String reference,
        String transactionDateTime,
        String currency,
        String paymentLinkId,
        String code,
        String desc,
        String counterAccountBankId,
        String counterAccountBankName,
        String counterAccountName,
        String counterAccountNumber,
        String virtualAccountName,
        String virtualAccountNumber
) {
}
