package org.example.goldenheartrestaurant.modules.paymentgateway.dto.payos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PayOsResponse<T>(
        String code,
        String desc,
        T data,
        String signature
) {
}
