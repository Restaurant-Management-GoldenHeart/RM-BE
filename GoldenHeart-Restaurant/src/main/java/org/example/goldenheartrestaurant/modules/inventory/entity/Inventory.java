package org.example.goldenheartrestaurant.modules.inventory.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.goldenheartrestaurant.common.entity.BaseEntity;
import org.example.goldenheartrestaurant.modules.restaurant.entity.Branch;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "inventory",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_inventory_branch_ingredient_active", columnNames = {"branch_id", "ingredient_id", "active_record_key"})
        }
)
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at")),
        @AttributeOverride(name = "deletedAt", column = @Column(name = "deleted_at"))
})
@SQLDelete(sql = "UPDATE inventory SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP, active_record_key = NULL WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Current stock snapshot for one ingredient in one branch.
 */
public class Inventory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(name = "active_record_key", length = 20)
    private String activeRecordKey;

    @Column(precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "min_stock_level", precision = 10, scale = 2)
    private BigDecimal minStockLevel;

    @Column(name = "reorder_level", precision = 10, scale = 2)
    private BigDecimal reorderLevel;

    @Column(name = "average_unit_cost", precision = 12, scale = 2)
    private BigDecimal averageUnitCost;

    @Column(name = "last_receipt_at")
    private LocalDateTime lastReceiptAt;

    @Column(name = "last_counted_at")
    private LocalDateTime lastCountedAt;

    @Override
    protected void onCreate() {
        super.onCreate();
        syncActiveRecordKey();
    }

    @Override
    protected void onUpdate() {
        super.onUpdate();
        syncActiveRecordKey();
    }

    @PrePersist
    @PreUpdate
    protected void syncBeforePersist() {
        syncActiveRecordKey();
    }

    private void syncActiveRecordKey() {
        if (getDeletedAt() == null) {
            activeRecordKey = "ACTIVE";
        } else {
            activeRecordKey = null;
        }
    }
}
