package org.example.goldenheartrestaurant.modules.identity.repository;

import org.example.goldenheartrestaurant.modules.identity.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy vấn role.
 *
 * Vì role là bảng nền khá nhỏ nên các method hiện tại chủ yếu phục vụ:
 * - tìm role theo tên khi bootstrap / auth / employee create
 * - lấy danh sách role để đổ dropdown ở frontend
 */
public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByNameIgnoreCase(String name);

    List<Role> findAllByOrderByNameAsc();
}
