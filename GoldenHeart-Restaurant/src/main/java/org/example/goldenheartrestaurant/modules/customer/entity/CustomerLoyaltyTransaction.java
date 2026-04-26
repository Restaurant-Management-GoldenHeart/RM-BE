package org.example.goldenheartrestaurant.modules.customer.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.goldenheartrestaurant.modules.billing.entity.Bill;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "customer_loyalty_transactions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_customer_loyalty_transactions_bill_type", columnNames = {"bill_id", "type"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * So cai cho lich su cong/tru diem cua tung customer.
 *
 * Moi bill PAID chi duoc sinh toi da 1 transaction type EARN.
 */
public class CustomerLoyaltyTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id")
    private Bill bill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CustomerLoyaltyTransactionType type;

    @Column(name = "points_delta", nullable = false)
    private Integer pointsDelta;

    @Column(name = "points_before", nullable = false)
    private Integer pointsBefore;

    @Column(name = "points_after", nullable = false)
    private Integer pointsAfter;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
