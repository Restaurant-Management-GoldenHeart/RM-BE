package org.example.goldenheartrestaurant.modules.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.goldenheartrestaurant.common.entity.BaseEntity;

import java.math.BigDecimal;

@Entity
@Table(
        name = "customer_tiers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_customer_tiers_code", columnNames = "code"),
                @UniqueConstraint(name = "uk_customer_tiers_name", columnNames = "name"),
                @UniqueConstraint(name = "uk_customer_tiers_min_points", columnNames = "min_points")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Cau hinh hang khach hang theo so diem tich luy.
 *
 * minPoints la nguong toi thieu de khach duoc huong discountRate (%).
 */
public class CustomerTier extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "min_points", nullable = false)
    private Integer minPoints;

    @Column(name = "discount_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountRate;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(length = 255)
    private String note;
}
