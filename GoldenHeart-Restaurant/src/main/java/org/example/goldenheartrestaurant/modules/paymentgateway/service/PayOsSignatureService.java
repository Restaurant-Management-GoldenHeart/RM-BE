package org.example.goldenheartrestaurant.modules.paymentgateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.config.PayOsProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PayOsSignatureService {

    private final PayOsProperties payOsProperties;
    private final ObjectMapper objectMapper;

    public String createPaymentRequestSignature(int amount,
                                                String cancelUrl,
                                                String description,
                                                long orderCode,
                                                String returnUrl) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("amount", amount);
        fields.put("cancelUrl", normalizeValue(cancelUrl));
        fields.put("description", normalizeValue(description));
        fields.put("orderCode", orderCode);
        fields.put("returnUrl", normalizeValue(returnUrl));
        return sign(fields);
    }

    public boolean isValidWebhookSignature(Object payloadData, String providedSignature) {
        if (payloadData == null || providedSignature == null || providedSignature.isBlank()) {
            return false;
        }
        return sign(objectToSortedMap(payloadData)).equals(providedSignature);
    }

    private String sign(Map<String, Object> sortedFields) {
        try {
            String payload = buildSignaturePayload(sortedFields);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(payOsProperties.getChecksumKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signedBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return toHex(signedBytes);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot create payOS signature", exception);
        }
    }

    private Map<String, Object> objectToSortedMap(Object source) {
        Map<String, Object> original = objectMapper.convertValue(source, objectMapper.getTypeFactory()
                .constructMapType(LinkedHashMap.class, String.class, Object.class));
        Map<String, Object> normalized = new LinkedHashMap<>();
        original.keySet().stream()
                .sorted(String::compareTo)
                .forEach(key -> normalized.put(key, normalizeValue(original.get(key))));
        return normalized;
    }

    private String buildSignaturePayload(Map<String, Object> sortedFields) {
        List<String> pairs = new ArrayList<>();
        sortedFields.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .forEach(entry -> pairs.add(entry.getKey() + "=" + normalizeToString(entry.getValue())));
        return String.join("&", pairs);
    }

    private Object normalizeValue(Object rawValue) {
        if (rawValue == null || "null".equals(rawValue) || "undefined".equals(rawValue)) {
            return "";
        }
        if (rawValue instanceof Map<?, ?> mapValue) {
            Map<String, Object> sortedMap = new LinkedHashMap<>();
            mapValue.keySet().stream()
                    .map(String::valueOf)
                    .sorted(String::compareTo)
                    .forEach(key -> sortedMap.put(key, normalizeValue(mapValue.get(key))));
            return sortedMap;
        }
        if (rawValue instanceof List<?> listValue) {
            return listValue.stream().map(this::normalizeValue).toList();
        }
        return rawValue;
    }

    private String normalizeToString(Object rawValue) {
        if (rawValue instanceof Map<?, ?> || rawValue instanceof List<?>) {
            try {
                return objectMapper.writeValueAsString(rawValue);
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Cannot serialize payOS signature payload", exception);
            }
        }
        return String.valueOf(normalizeValue(rawValue));
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte current : bytes) {
            builder.append(String.format("%02x", current));
        }
        return builder.toString();
    }
}
