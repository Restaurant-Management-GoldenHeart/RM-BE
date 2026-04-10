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
 * Security-oriented projection of {@code User}.
 *
 * Spring Security only needs credentials and authorities, not the whole entity graph.
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
                // hasRole('ADMIN') resolves through ROLE_ADMIN authority generated here.
                List.of(new SimpleGrantedAuthority("ROLE_" + normalizedRole))
        );
    }
}
