package org.example.goldenheartrestaurant.modules.identity.repository;

import org.example.goldenheartrestaurant.modules.identity.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository truy vấn User cho cả auth và employee module.
 *
 * Phần khó của repository này nằm ở các JPQL có join fetch,
 * vì ta cần vừa lấy đủ dữ liệu cho service vừa tránh N+1 query.
 */
public interface UserRepository extends JpaRepository<User, Integer> {

    boolean existsByUsernameIgnoreCase(String username);

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Integer id);

    @Query("""
            select u
            from User u
            join fetch u.role r
            join fetch u.profile up
            left join fetch up.branch b
            where lower(u.username) = lower(:username)
              and u.deletedAt is null
              and up.deletedAt is null
              and r.deletedAt is null
            """)
    Optional<User> findActiveAuthUserByUsername(@Param("username") String username);

    @Query(
            value = """
                    select u
                    from User u
                    join fetch u.role r
                    join fetch u.profile up
                    left join fetch up.branch b
                    where u.deletedAt is null
                      and up.deletedAt is null
                      and r.deletedAt is null
                      and (:keyword is null
                           or lower(u.username) like lower(concat('%', :keyword, '%'))
                           or lower(up.fullName) like lower(concat('%', :keyword, '%'))
                           or lower(up.email) like lower(concat('%', :keyword, '%'))
                           or up.phone like concat('%', :keyword, '%')
                           or lower(up.employeeCode) like lower(concat('%', :keyword, '%')))
                    """,
            countQuery = """
                    select count(u)
                    from User u
                    join u.role r
                    join u.profile up
                    left join up.branch b
                    where u.deletedAt is null
                      and up.deletedAt is null
                      and r.deletedAt is null
                      and (:keyword is null
                           or lower(u.username) like lower(concat('%', :keyword, '%'))
                           or lower(up.fullName) like lower(concat('%', :keyword, '%'))
                           or lower(up.email) like lower(concat('%', :keyword, '%'))
                           or up.phone like concat('%', :keyword, '%')
                           or lower(up.employeeCode) like lower(concat('%', :keyword, '%')))
                    """
    )
    Page<User> searchEmployees(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            select u
            from User u
            join fetch u.role r
            join fetch u.profile up
            left join fetch up.branch b
            where u.id = :userId
              and u.deletedAt is null
              and up.deletedAt is null
              and r.deletedAt is null
            """)
    Optional<User> findEmployeeDetailById(@Param("userId") Integer userId);
}
