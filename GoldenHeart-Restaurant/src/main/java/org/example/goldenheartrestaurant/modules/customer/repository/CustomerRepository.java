package org.example.goldenheartrestaurant.modules.customer.repository;

import org.example.goldenheartrestaurant.modules.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository của Customer.
 *
 * Tập trung vào 3 nhóm việc:
 * - search phân trang
 * - check uniqueness cho các field business key
 * - update loyalty point theo id
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

    boolean existsByActiveEmailIgnoreCase(String activeEmail);

    boolean existsByActivePhone(String activePhone);

    boolean existsByCustomerCodeIgnoreCase(String customerCode);

    boolean existsByActiveEmailIgnoreCaseAndIdNot(String activeEmail, Integer id);

    boolean existsByActivePhoneAndIdNot(String activePhone, Integer id);

    boolean existsByCustomerCodeIgnoreCaseAndIdNot(String customerCode, Integer id);

    @Modifying
    @Query("update Customer c set c.loyaltyPoints = :loyaltyPoints where c.id = :customerId")
    void updateLoyaltyPoints(@Param("customerId") Integer customerId, @Param("loyaltyPoints") Integer loyaltyPoints);
}
