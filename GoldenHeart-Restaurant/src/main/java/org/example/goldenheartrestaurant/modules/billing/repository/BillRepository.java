package org.example.goldenheartrestaurant.modules.billing.repository;

import org.example.goldenheartrestaurant.modules.billing.entity.BillStatus;
import org.example.goldenheartrestaurant.modules.billing.entity.Bill;
import org.example.goldenheartrestaurant.modules.report.repository.projection.BillStatusCountProjection;
import org.example.goldenheartrestaurant.modules.report.repository.projection.PaidBillRevenueSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Integer> {

    boolean existsByOrder_Id(Integer orderId);

    @Query("""
            select distinct b
            from Bill b
            join fetch b.order o
            left join fetch o.table
            left join fetch o.customer
            left join fetch b.appliedCustomerTier
            left join fetch b.payments p
            where b.id = :billId
            """)
    Optional<Bill> findDetailById(@Param("billId") Integer billId);

    @Query("""
            select distinct b
            from Bill b
            join fetch b.order o
            left join fetch o.table
            left join fetch o.customer
            left join fetch b.appliedCustomerTier
            left join fetch b.payments p
            where o.id = :orderId
            order by b.id desc
            """)
    List<Bill> findAllDetailsByOrderId(@Param("orderId") Integer orderId);

    @Query("""
            select distinct b
            from Bill b
            join fetch b.order o
            join fetch o.branch
            left join fetch o.table
            left join fetch o.customer
            left join fetch b.appliedCustomerTier
            left join fetch b.payments p
            where b.id in :billIds
            """)
    List<Bill> findAllDetailsByIds(@Param("billIds") List<Integer> billIds);

    @Query(
            value = """
                    select b.id
                    from Bill b
                    join b.order o
                    left join o.table t
                    left join o.customer c
                    where (:branchId is null or o.branch.id = :branchId)
                      and (:status is null or b.status = :status)
                      and (:fromDateTime is null or (select max(p.paidAt) from Payment p where p.bill = b) >= :fromDateTime)
                      and (:toDateTime is null or (select max(p.paidAt) from Payment p where p.bill = b) < :toDateTime)
                      and (:keyword is null
                           or lower(coalesce(c.name, '')) like lower(concat('%', :keyword, '%'))
                           or lower(coalesce(t.tableNumber, '')) like lower(concat('%', :keyword, '%'))
                           or lower(str(b.id)) like lower(concat('%', :keyword, '%'))
                           or lower(str(o.id)) like lower(concat('%', :keyword, '%')))
                    order by (select max(p2.paidAt) from Payment p2 where p2.bill = b) desc, b.id desc
                    """,
            countQuery = """
                    select count(b)
                    from Bill b
                    join b.order o
                    left join o.table t
                    left join o.customer c
                    where (:branchId is null or o.branch.id = :branchId)
                      and (:status is null or b.status = :status)
                      and (:fromDateTime is null or (select max(p.paidAt) from Payment p where p.bill = b) >= :fromDateTime)
                      and (:toDateTime is null or (select max(p.paidAt) from Payment p where p.bill = b) < :toDateTime)
                      and (:keyword is null
                           or lower(coalesce(c.name, '')) like lower(concat('%', :keyword, '%'))
                           or lower(coalesce(t.tableNumber, '')) like lower(concat('%', :keyword, '%'))
                           or lower(str(b.id)) like lower(concat('%', :keyword, '%'))
                           or lower(str(o.id)) like lower(concat('%', :keyword, '%')))
                    """
    )
    Page<Integer> searchHistoryIds(@Param("keyword") String keyword,
                                   @Param("branchId") Integer branchId,
                                   @Param("status") BillStatus status,
                                   @Param("fromDateTime") LocalDateTime fromDateTime,
                                   @Param("toDateTime") LocalDateTime toDateTime,
                                   Pageable pageable);

    @Query("""
            select b.status as status, count(b) as total
            from Bill b
            join b.order o
            where (:branchId is null or o.branch.id = :branchId)
            group by b.status
            """)
    List<BillStatusCountProjection> countByStatusForReport(@Param("branchId") Integer branchId);

    @Query("""
            select count(b) as paidBillsCount,
                   coalesce(sum(b.total), 0) as paidBillRevenue,
                   coalesce(sum(b.grossProfit), 0) as grossProfit
            from Bill b
            join b.order o
            where b.status = org.example.goldenheartrestaurant.modules.billing.entity.BillStatus.PAID
              and (:branchId is null or o.branch.id = :branchId)
              and (select max(p.paidAt) from Payment p where p.bill = b) >= :fromDateTime
              and (select max(p.paidAt) from Payment p where p.bill = b) < :toDateTime
            """)
    PaidBillRevenueSummaryProjection summarizePaidBillsByCompletedAtForReport(
            @Param("branchId") Integer branchId,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTime") LocalDateTime toDateTime
    );
}
