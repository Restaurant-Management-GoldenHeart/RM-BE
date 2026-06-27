package org.example.goldenheartrestaurant.modules.waste.dto.response;

import java.math.BigDecimal;

public record WasteStatsResponse(
        long totalPending,
        long totalApproved,
        long totalRejected,
        BigDecimal estimatedWastedValue
) {}
