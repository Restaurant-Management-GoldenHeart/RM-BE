package org.example.goldenheartrestaurant.modules.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
import org.example.goldenheartrestaurant.common.entity.BaseEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "measurement_units",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_measurement_units_active_code", columnNames = "active_code"),
                @UniqueConstraint(name = "uk_measurement_units_active_symbol", columnNames = "active_symbol")
        }
)
@SQLDelete(sql = "UPDATE measurement_units SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP, active_code = NULL, active_symbol = NULL WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasurementUnit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(name = "active_code", length = 30, unique = true)
    private String activeCode;

    @Column(name = "active_symbol", length = 20, unique = true)
    private String activeSymbol;

    @Column(length = 255)
    private String description;

    @Builder.Default
    @OneToMany(mappedBy = "measurementUnit")
    private List<Ingredient> ingredients = new ArrayList<>();

    @Override
    protected void onCreate() {
        super.onCreate();
        syncActiveFields();
    }

    @Override
    protected void onUpdate() {
        super.onUpdate();
        syncActiveFields();
    }

    @PrePersist
    @PreUpdate
    protected void syncBeforePersist() {
        syncActiveFields();
    }

    private void syncActiveFields() {
        if (getDeletedAt() == null) {
            activeCode = code;
            activeSymbol = symbol;
        } else {
            activeCode = null;
            activeSymbol = null;
        }
    }

    public Integer getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getDescription() {
        return description;
    }
}
