package org.example.goldenheartrestaurant.modules.identity.repository;

import org.example.goldenheartrestaurant.modules.identity.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository cho UserProfile.
 *
 * Các method trong đây tập trung nhiều vào uniqueness logic,
 * vì email / phone / employeeCode thực tế nằm ở profile chứ không nằm ở bảng users.
 */
public interface UserProfileRepository extends JpaRepository<UserProfile, Integer> {

    boolean existsByActiveEmailIgnoreCase(String activeEmail);

    boolean existsByActivePhone(String activePhone);

    boolean existsByEmployeeCodeIgnoreCase(String employeeCode);

    boolean existsByActiveEmailIgnoreCaseAndUserIdNot(String activeEmail, Integer userId);

    boolean existsByActivePhoneAndUserIdNot(String activePhone, Integer userId);

    boolean existsByEmployeeCodeIgnoreCaseAndUserIdNot(String employeeCode, Integer userId);

    @Query("""
            select up
            from UserProfile up
            left join fetch up.branch
            where up.userId = :userId
            """)
    Optional<UserProfile> findActiveDetailByUserId(@Param("userId") Integer userId);

    @Modifying
    @Query("""
            update UserProfile up
            set up.deletedAt = current timestamp,
                up.updatedAt = current timestamp,
                up.activeEmail = null,
                up.activePhone = null
            where up.userId = :userId
            """)
    void softDeleteByUserId(@Param("userId") Integer userId);
}
