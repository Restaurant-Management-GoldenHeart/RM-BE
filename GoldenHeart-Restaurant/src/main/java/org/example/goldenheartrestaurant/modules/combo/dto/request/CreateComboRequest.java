package org.example.goldenheartrestaurant.modules.combo.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateComboRequest(
        // ADMIN phải truyền branchId (lấy từ BranchContext); MANAGER bỏ qua — backend tự resolve từ profile
        Integer branchId,

        @NotBlank String name,

        String description,

        @Valid
        @NotEmpty
        List<ComboItemRequest> items
) {
}
