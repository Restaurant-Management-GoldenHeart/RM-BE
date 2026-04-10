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
 * Loads application users for Spring Security.
 *
 * Both login and request-time JWT authentication reuse this service so permissions always reflect
 * the latest persisted user/role state.
 */
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Repository method is the gatekeeper for which accounts are allowed to authenticate.
        User user = userRepository.findActiveAuthUserByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return CustomUserDetails.from(user);
    }
}
