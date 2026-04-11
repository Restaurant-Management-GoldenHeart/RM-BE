package org.example.goldenheartrestaurant.modules.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.goldenheartrestaurant.modules.identity.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_action_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "inventory_id", nullable = false)
    private Integer inventoryId;

    @Column(name = "branch_id", nullable = false)
    private Integer branchId;

    @Column(name = "branch_name", nullable = false, length = 150)
    private String branchName;

    @Column(name = "ingredient_id", nullable = false)
    private Integer ingredientId;

    @Column(name = "ingredient_name", nullable = false, length = 100)
    private String ingredientName;

    @Column(name = "unit_symbol", length = 20)
    private String unitSymbol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acted_by")
    private User actedBy;

    @Column(name = "acted_by_username", length = 50)
    private String actedByUsername;

    @Column(name = "acted_by_full_name", length = 100)
    private String actedByFullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private InventoryActionType actionType;

    @Column(name = "before_quantity", precision = 12, scale = 2)
    private BigDecimal beforeQuantity;

    @Column(name = "after_quantity", precision = 12, scale = 2)
    private BigDecimal afterQuantity;

    @Column(name = "before_min_stock_level", precision = 12, scale = 2)
    private BigDecimal beforeMinStockLevel;

    @Column(name = "after_min_stock_level", precision = 12, scale = 2)
    private BigDecimal afterMinStockLevel;

    @Column(name = "before_reorder_level", precision = 12, scale = 2)
    private BigDecimal beforeReorderLevel;

    @Column(name = "after_reorder_level", precision = 12, scale = 2)
    private BigDecimal afterReorderLevel;

    @Column(name = "before_average_unit_cost", precision = 12, scale = 2)
    private BigDecimal beforeAverageUnitCost;

    @Column(name = "after_average_unit_cost", precision = 12, scale = 2)
    private BigDecimal afterAverageUnitCost;

    @Column(name = "before_ingredient_name", length = 100)
    private String beforeIngredientName;

    @Column(name = "after_ingredient_name", length = 100)
    private String afterIngredientName;

    @Column(name = "before_unit_symbol", length = 20)
    private String beforeUnitSymbol;

    @Column(name = "after_unit_symbol", length = 20)
    private String afterUnitSymbol;

    @Column(length = 1000)
    private String summary;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
}
