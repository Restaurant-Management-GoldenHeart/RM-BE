package org.example.goldenheartrestaurant.modules.identity.entity;

import org.example.goldenheartrestaurant.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.goldenheartrestaurant.modules.restaurant.entity.Branch;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "user_profiles",
        uniqueConstraints = {
                @jakarta.persistence.UniqueConstraint(name = "uk_user_profiles_active_email", columnNames = "active_email"),
                @jakarta.persistence.UniqueConstraint(name = "uk_user_profiles_active_phone", columnNames = "active_phone"),
                @jakarta.persistence.UniqueConstraint(name = "uk_user_profiles_employee_code", columnNames = "employee_code")
        }
)
@SQLDelete(sql = "UPDATE user_profiles SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP, active_email = NULL, active_phone = NULL WHERE user_id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile extends BaseEntity {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "employee_code", unique = true, length = 30)
    private String employeeCode;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "active_phone", length = 20, unique = true)
    private String activePhone;

    @Column(name = "active_email", length = 100, unique = true)
    private String activeEmail;

    @Column(length = 255)
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 10)
    private String gender;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(precision = 12, scale = 2)
    private BigDecimal salary;

    @Column(name = "internal_notes", length = 1000)
    private String internalNotes;

    @Override
    @jakarta.persistence.PrePersist
    protected void onCreate() {
        super.onCreate();
        syncActiveContacts();
    }

    @Override
    @jakarta.persistence.PreUpdate
    protected void onUpdate() {
        super.onUpdate();
        syncActiveContacts();
    }

    private void syncActiveContacts() {
        if (getDeletedAt() == null) {
            activeEmail = email;
            activePhone = phone;
        } else {
            activeEmail = null;
            activePhone = null;
        }
    }
}
