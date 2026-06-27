package org.example.goldenheartrestaurant.modules.combo.dto.request;

import jakarta.validation.Valid;

import java.util.List;

public record UpdateComboRequest(
        String name,
        String description,
        String status,
        // null = giữ nguyên danh sách items; không null = thay thế toàn bộ
        @Valid List<ComboItemRequest> items
) {
}
