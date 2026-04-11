package org.example.goldenheartrestaurant.modules.auth.repository;

import org.example.goldenheartrestaurant.modules.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

    // Tìm token theo bản hash để backend không cần lưu raw refresh token trong DB.
    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
