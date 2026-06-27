package org.example.goldenheartrestaurant.modules.menu.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PopularDishResponse {
    private Integer id;
    private String name;
    private String imageUrl;
    private String description;
    private BigDecimal price;
    private Integer categoryId;
    private String categoryName;
    private Long totalOrderCount;
    private boolean bestSeller;
}
