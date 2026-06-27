package org.example.goldenheartrestaurant.modules.waste.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateWasteRequestRequest(
        Integer branchId,

        String note,

        @NotEmpty(message = "Phiếu xuất hủy phải có ít nhất một nguyên liệu")
        @Valid
        List<CreateWasteRequestItemDto> items
) {}
