package org.example.goldenheartrestaurant.modules.waste.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.example.goldenheartrestaurant.modules.waste.entity.WasteReason;

import java.math.BigDecimal;

public record CreateWasteRequestItemDto(
        @NotNull(message = "ingredientId là bắt buộc")
        Integer ingredientId,

        @NotNull(message = "Số lượng là bắt buộc")
        @DecimalMin(value = "0.01", message = "Số lượng phải lớn hơn 0")
        BigDecimal quantity,

        @NotNull(message = "Lý do xuất hủy là bắt buộc")
        WasteReason reason,

        String note
) {}
