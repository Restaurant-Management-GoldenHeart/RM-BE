package org.example.goldenheartrestaurant.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
/**
 * Shared audit fields for entities that need created/updated timestamps and soft delete support.
 *
 * Concrete entities combine this base class with Hibernate soft-delete annotations so rows can be
 * hidden from normal queries without losing historical data.
 */
public abstract class BaseEntity {

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        // Keep audit timestamps centralized instead of repeating this in every service method.
        updatedAt = LocalDateTime.now();
    }
}
