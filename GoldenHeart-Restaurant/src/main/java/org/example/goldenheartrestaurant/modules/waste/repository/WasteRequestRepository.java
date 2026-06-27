package org.example.goldenheartrestaurant.modules.waste.repository;

import org.example.goldenheartrestaurant.modules.waste.entity.WasteRequest;
import org.example.goldenheartrestaurant.modules.waste.entity.WasteRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WasteRequestRepository extends JpaRepository<WasteRequest, Integer> {

    @Query(
            value = """
                    select wr from WasteRequest wr
                    join fetch wr.branch
                    join fetch wr.requestedBy rb
                    left join fetch rb.profile
                    left join fetch wr.reviewedBy rv
                    left join fetch rv.profile
                    where (:branchId is null or wr.branch.id = :branchId)
                      and (:status is null or wr.status = :status)
                      and (:dateFrom is null or wr.createdAt >= :dateFrom)
                      and (:dateTo is null or wr.createdAt <= :dateTo)
                    """,
            countQuery = """
                    select count(wr) from WasteRequest wr
                    where (:branchId is null or wr.branch.id = :branchId)
                      and (:status is null or wr.status = :status)
                      and (:dateFrom is null or wr.createdAt >= :dateFrom)
                      and (:dateTo is null or wr.createdAt <= :dateTo)
                    """
    )
    Page<WasteRequest> findAllFiltered(
            @Param("branchId") Integer branchId,
            @Param("status") WasteRequestStatus status,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable
    );

    @Query("""
            select count(wr) from WasteRequest wr
            where (:branchId is null or wr.branch.id = :branchId)
              and (:status is null or wr.status = :status)
              and (:dateFrom is null or wr.createdAt >= :dateFrom)
              and (:dateTo is null or wr.createdAt <= :dateTo)
            """)
    long countFiltered(
            @Param("branchId") Integer branchId,
            @Param("status") WasteRequestStatus status,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo
    );

    @Query("""
            select wr from WasteRequest wr
            join fetch wr.branch
            join fetch wr.requestedBy rb
            left join fetch rb.profile
            left join fetch wr.reviewedBy rv
            left join fetch rv.profile
            left join fetch wr.items wi
            left join fetch wi.ingredient ing
            left join fetch ing.measurementUnit
            where wr.status = org.example.goldenheartrestaurant.modules.waste.entity.WasteRequestStatus.APPROVED
              and (:branchId is null or wr.branch.id = :branchId)
              and (:dateFrom is null or wr.reviewedAt >= :dateFrom)
              and (:dateTo is null or wr.reviewedAt <= :dateTo)
            order by wr.reviewedAt desc
            """)
    List<WasteRequest> findApprovedForExport(
            @Param("branchId") Integer branchId,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo
    );

    @Query("""
            select wr from WasteRequest wr
            join fetch wr.branch
            join fetch wr.requestedBy rb
            left join fetch rb.profile
            left join fetch wr.reviewedBy rv
            left join fetch rv.profile
            left join fetch wr.items wi
            left join fetch wi.ingredient ing
            left join fetch ing.measurementUnit
            where wr.id = :id
            """)
    Optional<WasteRequest> findDetailById(@Param("id") Integer id);

    long countByBranchIdAndStatus(Integer branchId, WasteRequestStatus status);

    long countByStatus(WasteRequestStatus status);
}
