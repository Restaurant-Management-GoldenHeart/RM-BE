package org.example.goldenheartrestaurant.modules.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.goldenheartrestaurant.modules.menu.entity.Recipe;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "ingredients",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ingredients_name", columnNames = "name")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private MeasurementUnit measurementUnit;

    @Column(name = "unit", length = 20)
    private String legacyUnit;

    @Column(length = 500)
    private String description;

    @Builder.Default
    @OneToMany(mappedBy = "ingredient")
    private List<Recipe> recipes = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "ingredient")
    private List<Inventory> inventories = new ArrayList<>();

    @PrePersist
    @PreUpdate
    protected void syncLegacyUnit() {
        // Giữ lại cột unit cũ để tương thích dữ liệu cũ trong DB khi chuyển sang bảng đơn vị.
        if (measurementUnit != null) {
            legacyUnit = measurementUnit.getSymbol();
        }
    }

    public String resolveUnitSymbol() {
        return measurementUnit != null ? measurementUnit.getSymbol() : legacyUnit;
    }

    public String resolveUnitName() {
        return measurementUnit != null ? measurementUnit.getName() : legacyUnit;
    }
}
