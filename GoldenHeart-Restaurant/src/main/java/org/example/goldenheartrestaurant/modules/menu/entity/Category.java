package org.example.goldenheartrestaurant.modules.menu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Mo ta danh muc chi can text ngan de hien thi/tra cuu.
     *
     * Khong dung @Lob o day vi Hibernate 7 validate JPQL `lower(description)`
     * se xem field nay la CLOB va tu choi cac ham xu ly chuoi nhu lower/like.
     * Gioi han 2000 ky tu la du cho use-case hien tai va giu query search don gian.
     */
    @Column(length = 2000)
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "production_station", nullable = false, length = 30)
    @ColumnDefault("'KITCHEN'")
    private ProductionStation productionStation = ProductionStation.KITCHEN;

    @Builder.Default
    @OneToMany(mappedBy = "category")
    private List<MenuItem> menuItems = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (productionStation == null) {
            productionStation = ProductionStation.KITCHEN;
        }
    }
}