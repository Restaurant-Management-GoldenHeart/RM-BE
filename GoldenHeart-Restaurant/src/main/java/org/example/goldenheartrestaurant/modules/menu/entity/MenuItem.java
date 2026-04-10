package org.example.goldenheartrestaurant.modules.menu.entity;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.goldenheartrestaurant.modules.order.entity.OrderItem;
import org.example.goldenheartrestaurant.modules.restaurant.entity.Branch;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "menu_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_menu_items_branch_category_name", columnNames = {"branch_id", "category_id", "name"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Sellable menu item for a specific branch and category.
 *
 * Recipes are attached here because kitchen completion needs per-dish ingredient consumption data.
 */
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MenuItemStatus status;

    @Builder.Default
    @OneToMany(mappedBy = "menuItem", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    // Orphan removal helps replace recipe definitions cleanly during menu edits.
    private List<Recipe> recipes = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "menuItem")
    private List<OrderItem> orderItems = new ArrayList<>();
}
