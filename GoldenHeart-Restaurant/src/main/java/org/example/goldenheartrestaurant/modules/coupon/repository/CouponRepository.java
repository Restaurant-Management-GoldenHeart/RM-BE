package org.example.goldenheartrestaurant.modules.coupon.repository;

import org.example.goldenheartrestaurant.modules.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repository cơ bản của Coupon — admin dùng để CRUD định nghĩa coupon. */
public interface CouponRepository extends JpaRepository<Coupon, Integer> {

    Optional<Coupon> findByCode(String code);

    boolean existsByCode(String code);
}
