package org.example.goldenheartrestaurant.modules.combo.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class ComboResponse {
    private Integer id;
    private Integer branchId;
    private String branchName;
    private String name;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private Integer discountPct;
    private String status;
    private List<ComboItemResponse> items;
}
