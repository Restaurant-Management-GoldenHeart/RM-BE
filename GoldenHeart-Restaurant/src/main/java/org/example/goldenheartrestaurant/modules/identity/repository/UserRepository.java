package org.example.goldenheartrestaurant.modules.identity.repository;

import org.example.goldenheartrestaurant.modules.identity.entity.User;
import org.example.goldenheartrestaurant.modules.identity.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Collection;

/**
 * Repository truy vấn User cho cả auth và employee module.
 *
 * Các query auth luôn join fetch đủ role/profile để:
 * - tránh N+1
 * - đọc đúng branch/email/phone ngay trong cùng transaction
 * - siết chỉ cho account ACTIVE đi vào các luồng nhạy cảm
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
              and u.status = :activeStatus
              and u.deletedAt is null
              and up.deletedAt is null
              and r.deletedAt is null
            """)
    Optional<User> findActiveAuthUserByUsername(
            @Param("username") String username,
            @Param("activeStatus") UserStatus activeStatus
    );

    @Query("""
            select u
            from User u
            join fetch u.role r
            join fetch u.profile up
            left join fetch up.branch b
            where lower(up.activeEmail) = lower(:email)
              and u.status = :activeStatus
              and u.deletedAt is null
              and up.deletedAt is null
              and r.deletedAt is null
            """)
    Optional<User> findActiveAuthUserByEmail(
            @Param("email") String email,
            @Param("activeStatus") UserStatus activeStatus
    );

    @Query("""
            select u
            from User u
            join fetch u.role r
            join fetch u.profile up
            left join fetch up.branch b
            where up.activePhone = :phone
              and u.status = :activeStatus
              and u.deletedAt is null
              and up.deletedAt is null
              and r.deletedAt is null
            """)
    Optional<User> findActiveAuthUserByPhone(
            @Param("phone") String phone,
            @Param("activeStatus") UserStatus activeStatus
    );

    @Query("""
            select u
            from User u
            join fetch u.role r
            join fetch u.profile up
            left join fetch up.branch b
            where u.id = :userId
              and u.status = :activeStatus
              and u.deletedAt is null
              and up.deletedAt is null
              and r.deletedAt is null
            """)
    Optional<User> findActiveAuthUserById(
            @Param("userId") Integer userId,
            @Param("activeStatus") UserStatus activeStatus
    );

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

    @Query("""
            select count(u)
            from User u
            join u.role r
            join u.profile up
            where u.deletedAt is null
              and up.deletedAt is null
              and r.deletedAt is null
              and u.status = :activeStatus
              and upper(r.name) in :roleNames
              and (:branchId is null or up.branch.id = :branchId)
            """)
    long countActiveEmployeesByBranchAndRoles(@Param("branchId") Integer branchId,
                                              @Param("roleNames") Collection<String> roleNames,
                                              @Param("activeStatus") UserStatus activeStatus);
}
