package org.example.goldenheartrestaurant.modules.waste.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class WasteRequestItemResponse {
    private Integer id;
    private Integer ingredientId;
    private String ingredientNameSnapshot;
    private String unitSymbolSnapshot;
    private BigDecimal quantity;
    private String reason;
    private String note;
}
