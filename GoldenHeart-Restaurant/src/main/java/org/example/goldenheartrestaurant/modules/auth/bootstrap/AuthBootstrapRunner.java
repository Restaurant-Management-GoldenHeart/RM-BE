package org.example.goldenheartrestaurant.modules.auth.bootstrap;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.modules.identity.entity.Role;
import org.example.goldenheartrestaurant.modules.identity.entity.User;
import org.example.goldenheartrestaurant.modules.identity.entity.UserProfile;
import org.example.goldenheartrestaurant.modules.identity.entity.UserStatus;
import org.example.goldenheartrestaurant.modules.identity.repository.RoleRepository;
import org.example.goldenheartrestaurant.modules.identity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthBootstrapRunner implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.enabled:true}")
    private boolean adminBootstrapEnabled;

    @Value("${app.bootstrap.admin.username:admin}")
    private String adminUsername;

    @Value("${app.bootstrap.admin.password:Admin123}")
    private String adminPassword;

    @Value("${app.bootstrap.admin.email:admin@goldenheart.com}")
    private String adminEmail;

    @Value("${app.bootstrap.admin.full-name:System Admin}")
    private String adminFullName;

    @Override
    public void run(ApplicationArguments args) {
        seedRoles();

        if (adminBootstrapEnabled && !userRepository.existsByUsernameIgnoreCase(adminUsername)) {
            Role adminRole = roleRepository.findByNameIgnoreCase("ADMIN")
                    .orElseThrow();

            User admin = User.builder()
                    .username(adminUsername)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .role(adminRole)
                    .status(UserStatus.ACTIVE)
                    .build();

            UserProfile profile = UserProfile.builder()
                    .user(admin)
                    .fullName(adminFullName)
                    .email(adminEmail)
                    .activeEmail(adminEmail)
                    .build();

            admin.setProfile(profile);
            userRepository.save(admin);
        }
    }

    private void seedRoles() {
        List<String> roles = List.of("ADMIN", "MANAGER", "STAFF", "KITCHEN", "CUSTOMER");

        for (String roleName : roles) {
            roleRepository.findByNameIgnoreCase(roleName)
                    .orElseGet(() -> roleRepository.save(
                            Role.builder()
                                    .name(roleName)
                                    .description(roleName + " role")
                                    .build()
                    ));
        }
    }
}
