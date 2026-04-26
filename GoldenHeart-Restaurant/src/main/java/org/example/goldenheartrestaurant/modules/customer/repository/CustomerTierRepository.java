package org.example.goldenheartrestaurant.modules.customer.repository;

import org.example.goldenheartrestaurant.modules.customer.entity.CustomerTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerTierRepository extends JpaRepository<CustomerTier, Integer> {

    List<CustomerTier> findAllByOrderByMinPointsAsc();

    List<CustomerTier> findAllByActiveTrueOrderByMinPointsAsc();

    Optional<CustomerTier> findByCodeIgnoreCase(String code);

    Optional<CustomerTier> findFirstByActiveTrueAndMinPointsLessThanEqualOrderByMinPointsDesc(Integer loyaltyPoints);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Integer id);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);

    boolean existsByMinPoints(Integer minPoints);

    boolean existsByMinPointsAndIdNot(Integer minPoints, Integer id);
}
