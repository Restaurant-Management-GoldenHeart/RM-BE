package org.example.goldenheartrestaurant.modules.combo.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ComboItemResponse {
    private Integer menuItemId;
    private String menuItemName;
    private String menuItemImageUrl;
    private BigDecimal menuItemPrice;
    private Integer quantity;
}
