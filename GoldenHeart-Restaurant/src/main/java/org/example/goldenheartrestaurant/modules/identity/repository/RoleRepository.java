package org.example.goldenheartrestaurant.modules.identity.repository;

import org.example.goldenheartrestaurant.modules.identity.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByNameIgnoreCase(String name);

    List<Role> findAllByOrderByNameAsc();
}
