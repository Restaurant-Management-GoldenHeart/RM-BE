package org.example.goldenheartrestaurant.modules.customer.repository;

import org.example.goldenheartrestaurant.modules.customer.entity.CustomerLoyaltyTransaction;
import org.example.goldenheartrestaurant.modules.customer.entity.CustomerLoyaltyTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerLoyaltyTransactionRepository extends JpaRepository<CustomerLoyaltyTransaction, Integer> {

    boolean existsByBill_IdAndType(Integer billId, CustomerLoyaltyTransactionType type);

    Optional<CustomerLoyaltyTransaction> findFirstByBill_IdAndType(Integer billId, CustomerLoyaltyTransactionType type);

    Page<CustomerLoyaltyTransaction> findByCustomer_IdOrderByCreatedAtDesc(Integer customerId, Pageable pageable);
}
