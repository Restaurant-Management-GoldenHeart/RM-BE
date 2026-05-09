package org.example.goldenheartrestaurant.modules.paymentgateway.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.goldenheartrestaurant.common.entity.BaseEntity;
import org.example.goldenheartrestaurant.modules.billing.entity.Bill;
import org.example.goldenheartrestaurant.modules.billing.entity.Payment;
import org.example.goldenheartrestaurant.modules.billing.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payment_gateway_transactions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_gateway_tx_provider_order_code", columnNames = {"provider", "provider_order_code"}),
                @UniqueConstraint(name = "uk_gateway_tx_provider_payment_link_id", columnNames = {"provider", "provider_payment_link_id"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGatewayTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentGatewayProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "provider_order_code", nullable = false)
    private Long providerOrderCode;

    @Column(name = "provider_payment_link_id", length = 64)
    private String providerPaymentLinkId;

    @Column(name = "requested_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal requestedAmount;

    @Column(name = "paid_amount", precision = 12, scale = 2)
    private BigDecimal paidAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentGatewayTransactionStatus status;

    @Column(name = "checkout_url", length = 1000)
    private String checkoutUrl;

    @Column(name = "qr_code", length = 4000)
    private String qrCode;

    @Column(name = "deep_link", length = 1000)
    private String deepLink;

    @Column(name = "return_url", length = 1000)
    private String returnUrl;

    @Column(name = "cancel_url", length = 1000)
    private String cancelUrl;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "provider_reference", length = 128)
    private String providerReference;

    @Column(name = "provider_code", length = 32)
    private String providerCode;

    @Column(name = "provider_message", length = 255)
    private String providerMessage;

    @Column(name = "webhook_payload", columnDefinition = "TEXT")
    private String webhookPayload;
}
