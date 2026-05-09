package org.example.goldenheartrestaurant.modules.paymentgateway.repository;

import org.example.goldenheartrestaurant.modules.paymentgateway.entity.PaymentGatewayProvider;
import org.example.goldenheartrestaurant.modules.paymentgateway.entity.PaymentGatewayTransaction;
import org.example.goldenheartrestaurant.modules.paymentgateway.entity.PaymentGatewayTransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentGatewayTransactionRepository extends JpaRepository<PaymentGatewayTransaction, Integer> {

    boolean existsByBill_IdAndProviderAndStatus(Integer billId,
                                                PaymentGatewayProvider provider,
                                                PaymentGatewayTransactionStatus status);

    boolean existsByProviderAndProviderOrderCode(PaymentGatewayProvider provider, Long providerOrderCode);

    @Query("""
            select t
            from PaymentGatewayTransaction t
            join fetch t.bill b
            join fetch b.order o
            join fetch o.branch
            left join fetch o.table
            left join fetch o.customer
            left join fetch t.payment
            where t.id = :transactionId
            """)
    Optional<PaymentGatewayTransaction> findDetailById(@Param("transactionId") Integer transactionId);

    @Query("""
            select t
            from PaymentGatewayTransaction t
            join fetch t.bill b
            join fetch b.order o
            join fetch o.branch
            left join fetch o.table
            left join fetch o.customer
            left join fetch t.payment
            where b.id = :billId
              and t.provider = :provider
            order by t.id desc
            """)
    List<PaymentGatewayTransaction> findAllByBillIdAndProviderOrderByIdDesc(@Param("billId") Integer billId,
                                                                             @Param("provider") PaymentGatewayProvider provider);

    @Query("""
            select t
            from PaymentGatewayTransaction t
            join fetch t.bill b
            join fetch b.order o
            join fetch o.branch
            left join fetch o.table
            left join fetch o.customer
            left join fetch t.payment
            where t.provider = :provider
              and t.providerOrderCode = :providerOrderCode
            """)
    Optional<PaymentGatewayTransaction> findByProviderAndProviderOrderCodeForWebhook(@Param("provider") PaymentGatewayProvider provider,
                                                                                     @Param("providerOrderCode") Long providerOrderCode);
}
