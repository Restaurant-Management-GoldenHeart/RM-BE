package org.example.goldenheartrestaurant.common.security;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.modules.identity.entity.User;
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
 *
 * Nhờ đó quyền luôn phản ánh dữ liệu mới nhất trong DB.
 */
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Repository này đóng vai trò "cổng kiểm tra" xem account nào còn được phép đăng nhập.
        User user = userRepository.findActiveAuthUserByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return CustomUserDetails.from(user);
    }
}
