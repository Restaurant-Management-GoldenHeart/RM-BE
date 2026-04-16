package org.example.goldenheartrestaurant.modules.billing.repository;

import org.example.goldenheartrestaurant.modules.billing.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Integer> {

    boolean existsByOrder_Id(Integer orderId);

    @Query("""
            select distinct b
            from Bill b
            join fetch b.order o
            left join fetch o.table
            left join fetch b.payments p
            where b.id = :billId
            """)
    Optional<Bill> findDetailById(@Param("billId") Integer billId);

    @Query("""
            select distinct b
            from Bill b
            join fetch b.order o
            left join fetch o.table
            left join fetch b.payments p
            where o.id = :orderId
            order by b.id desc
            """)
    List<Bill> findAllDetailsByOrderId(@Param("orderId") Integer orderId);
}
