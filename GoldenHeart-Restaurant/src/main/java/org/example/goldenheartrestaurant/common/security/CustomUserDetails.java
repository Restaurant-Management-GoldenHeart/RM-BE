package org.example.goldenheartrestaurant.common.security;

import lombok.Getter;
import org.example.goldenheartrestaurant.modules.identity.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
/**
 * Phiên bản rút gọn của User dành riêng cho Spring Security.
 *
 * Thay vì giữ nguyên cả entity User trong SecurityContext, ta chỉ giữ đúng dữ liệu
 * phục vụ xác thực và phân quyền:
 * - userId
 * - username
 * - password hash
 * - roleName
 * - authorities
 */
public class CustomUserDetails implements UserDetails {

    private final Integer userId;
    private final String username;
    private final String password;
    private final String roleName;
    private final Collection<? extends GrantedAuthority> authorities;

    private CustomUserDetails(Integer userId,
                              String username,
                              String password,
                              String roleName,
                              Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.roleName = roleName;
        this.authorities = authorities;
    }

    public static CustomUserDetails from(User user) {
        String normalizedRole = user.getRole().getName().toUpperCase();

        return new CustomUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                normalizedRole,
                // Spring Security với hasRole('ADMIN') thực chất sẽ kiểm tra authority ROLE_ADMIN.
                // Vì vậy cần chuẩn hóa authority theo format ROLE_<TEN_ROLE>.
                List.of(new SimpleGrantedAuthority("ROLE_" + normalizedRole))
        );
    }
}
