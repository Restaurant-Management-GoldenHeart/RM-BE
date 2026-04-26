package org.example.goldenheartrestaurant.modules.billing.repository;

import org.example.goldenheartrestaurant.modules.billing.entity.Payment;
import org.example.goldenheartrestaurant.modules.report.repository.projection.PaymentMethodBreakdownProjection;
import org.example.goldenheartrestaurant.modules.report.repository.projection.PaymentRevenueSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    @Query("""
            select count(p) as paymentCount,
                   coalesce(sum(p.amount), 0) as cashIn
            from Payment p
            join p.bill b
            join b.order o
            where p.paidAt >= :fromDateTime
              and p.paidAt < :toDateTime
              and (:branchId is null or o.branch.id = :branchId)
            """)
    PaymentRevenueSummaryProjection summarizeCashInForReport(@Param("branchId") Integer branchId,
                                                             @Param("fromDateTime") LocalDateTime fromDateTime,
                                                             @Param("toDateTime") LocalDateTime toDateTime);

    @Query("""
            select p.method as method,
                   count(p) as paymentCount,
                   coalesce(sum(p.amount), 0) as totalAmount
            from Payment p
            join p.bill b
            join b.order o
            where p.paidAt >= :fromDateTime
              and p.paidAt < :toDateTime
              and (:branchId is null or o.branch.id = :branchId)
            group by p.method
            """)
    List<PaymentMethodBreakdownProjection> summarizeByMethodForReport(@Param("branchId") Integer branchId,
                                                                      @Param("fromDateTime") LocalDateTime fromDateTime,
                                                                      @Param("toDateTime") LocalDateTime toDateTime);
}
