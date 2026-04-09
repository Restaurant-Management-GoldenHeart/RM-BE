package org.example.goldenheartrestaurant.modules.customer.entity;

import jakarta.persistence.*;
import org.example.goldenheartrestaurant.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.goldenheartrestaurant.modules.order.entity.Order;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "customers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_customers_customer_code", columnNames = "customer_code"),
                @UniqueConstraint(name = "uk_customers_active_email", columnNames = "active_email"),
                @UniqueConstraint(name = "uk_customers_active_phone", columnNames = "active_phone")
        }
)
@SQLDelete(sql = "UPDATE customers SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP, active_email = NULL, active_phone = NULL WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "customer_code", unique = true, length = 30)
    private String customerCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(name = "active_phone", length = 20, unique = true)
    private String activePhone;

    @Column(name = "active_email", length = 100, unique = true)
    private String activeEmail;

    @Column(name = "loyalty_points")
    private Integer loyaltyPoints;

    @Column(length = 255)
    private String address;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 10)
    private String gender;

    @Column(length = 500)
    private String note;

    @Column(name = "last_visit_at")
    private java.time.LocalDateTime lastVisitAt;

    @Builder.Default
    @OneToMany(mappedBy = "customer")
    private List<Order> orders = new ArrayList<>();

    @Override
    protected void onCreate() {
        super.onCreate();
        syncActiveContacts();
    }

    @Override
    protected void onUpdate() {
        super.onUpdate();
        syncActiveContacts();
    }

    private void syncActiveContacts() {
        if (getDeletedAt() == null) {
            activeEmail = email;
            activePhone = phone;
        } else {
            activeEmail = null;
            activePhone = null;
        }
    }
}
