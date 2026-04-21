package org.example.goldenheartrestaurant.common.security;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.modules.identity.entity.User;
import org.example.goldenheartrestaurant.modules.identity.entity.UserStatus;
import org.example.goldenheartrestaurant.modules.identity.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
/**
 * Service trung gian để Spring Security lấy user từ database.
 *
 * Cả hai luồng đều dùng class này:
 * - lúc login bằng username/password
 * - lúc JwtAuthenticationFilter xác thực token ở các request sau
 */
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Chỉ account ACTIVE mới được đi tiếp vào các flow xác thực.
        User user = userRepository.findActiveAuthUserByUsername(username, UserStatus.ACTIVE)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return CustomUserDetails.from(user);
    }
}
