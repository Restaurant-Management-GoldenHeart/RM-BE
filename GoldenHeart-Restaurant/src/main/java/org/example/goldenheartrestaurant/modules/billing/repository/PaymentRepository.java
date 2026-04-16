package org.example.goldenheartrestaurant.modules.billing.repository;

import org.example.goldenheartrestaurant.modules.billing.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
}
