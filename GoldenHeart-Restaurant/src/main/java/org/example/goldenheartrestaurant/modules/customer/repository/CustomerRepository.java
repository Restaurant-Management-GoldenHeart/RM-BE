package org.example.goldenheartrestaurant.modules.customer.repository;

import jakarta.persistence.LockModeType;
import org.example.goldenheartrestaurant.modules.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository cua Customer.
 *
 * Gom 4 nhom query:
 * - search/lookup
 * - uniqueness cho field business key
 * - lock customer khi cap nhat diem
 * - update loyalty point tu cac luong cu
 */
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    @Query("""
            select c
            from Customer c
            where (:keyword is null
                   or lower(c.name) like lower(concat('%', :keyword, '%'))
                   or lower(c.email) like lower(concat('%', :keyword, '%'))
                   or c.phone like concat('%', :keyword, '%')
                   or lower(c.customerCode) like lower(concat('%', :keyword, '%')))
            """)
    Page<Customer> search(@Param("keyword") String keyword, Pageable pageable);

    Optional<Customer> findById(Integer id);

    /** Tìm Customer CRM theo user_id — dùng cho customer portal (khách đã đăng ký tài khoản). */
    Optional<Customer> findByUserId(Integer userId);

    /** Tìm Customer CRM theo active_email. */
    Optional<Customer> findByActiveEmailIgnoreCase(String activeEmail);

    /**
     * Tìm hồ sơ khách walk-in (userId == null) theo số điện thoại.
     *
     * <p>Dùng trong luồng đăng ký tài khoản: khách cung cấp SĐT đã đăng ký với nhà hàng
     * để hệ thống tìm và liên kết hồ sơ CRM cũ — giữ nguyên điểm tích lũy.
     * Điều kiện {@code userId is null} bảo đảm không bao giờ link nhầm vào
     * hồ sơ đã được claim bởi tài khoản khác.
     */
    @Query("select c from Customer c where c.activePhone = :phone and c.userId is null")
    Optional<Customer> findWalkInByActivePhone(@Param("phone") String phone);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Customer c where c.id = :customerId")
    Optional<Customer> findByIdForUpdate(@Param("customerId") Integer customerId);

    boolean existsByActiveEmailIgnoreCase(String activeEmail);

    boolean existsByActivePhone(String activePhone);

    boolean existsByCustomerCodeIgnoreCase(String customerCode);

    boolean existsByActiveEmailIgnoreCaseAndIdNot(String activeEmail, Integer id);

    boolean existsByActivePhoneAndIdNot(String activePhone, Integer id);

    boolean existsByCustomerCodeIgnoreCaseAndIdNot(String customerCode, Integer id);

    @Modifying
    @Query("update Customer c set c.loyaltyPoints = :loyaltyPoints where c.id = :customerId")
    void updateLoyaltyPoints(@Param("customerId") Integer customerId, @Param("loyaltyPoints") Integer loyaltyPoints);

    @Query("""
            select count(c)
            from Customer c
            where (:branchId is null
                   or exists (
                        select 1
                        from Order o
                        where o.customer = c
                          and o.branch.id = :branchId
                   ))
            """)
    long countCustomersForReport(@Param("branchId") Integer branchId);
}
