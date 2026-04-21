package org.example.goldenheartrestaurant.modules.auth.service;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.exception.BadRequestException;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.exception.ForbiddenException;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.common.security.JwtService;
import org.example.goldenheartrestaurant.modules.auth.dto.request.ChangePasswordRequest;
import org.example.goldenheartrestaurant.modules.auth.dto.request.LoginRequest;
import org.example.goldenheartrestaurant.modules.auth.dto.request.RegisterRequest;
import org.example.goldenheartrestaurant.modules.auth.dto.response.RegisterResponse;
import org.example.goldenheartrestaurant.modules.auth.entity.RefreshToken;
import org.example.goldenheartrestaurant.modules.auth.repository.RefreshTokenRepository;
import org.example.goldenheartrestaurant.modules.identity.entity.Role;
import org.example.goldenheartrestaurant.modules.identity.entity.User;
import org.example.goldenheartrestaurant.modules.identity.entity.UserProfile;
import org.example.goldenheartrestaurant.modules.identity.entity.UserStatus;
import org.example.goldenheartrestaurant.modules.identity.repository.RoleRepository;
import org.example.goldenheartrestaurant.modules.identity.repository.UserProfileRepository;
import org.example.goldenheartrestaurant.modules.identity.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
/**
 * Service điều phối toàn bộ nghiệp vụ xác thực:
 * - register
 * - login
 * - refresh
 * - logout
 */
public class AuthService {

    private static final String DEFAULT_CUSTOMER_ROLE = "CUSTOMER";
    /**
     * Theo product hiện tại chỉ nhân sự vận hành nội bộ mới được tự đổi mật khẩu.
     */
    private static final Set<String> CHANGE_PASSWORD_ALLOWED_ROLES = Set.of("MANAGER", "STAFF", "KITCHEN");

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.getUsername())) {
            throw new ConflictException("Username already exists");
        }

        if (userProfileRepository.existsByActiveEmailIgnoreCase(request.getEmail())) {
            throw new ConflictException("Email already exists");
        }

        Role defaultRole = roleRepository.findByNameIgnoreCase(DEFAULT_CUSTOMER_ROLE)
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name(DEFAULT_CUSTOMER_ROLE)
                                .description("Default role for customer self-registration")
                                .build()
                ));

        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(defaultRole)
                .status(UserStatus.ACTIVE)
                .build();

        UserProfile profile = UserProfile.builder()
                .user(user)
                .fullName(request.getFullName())
                .email(request.getEmail())
                .activeEmail(request.getEmail())
                .build();

        user.setProfile(profile);

        User savedUser = userRepository.save(user);

        return RegisterResponse.builder()
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getProfile().getEmail())
                .fullName(savedUser.getProfile().getFullName())
                .role(savedUser.getRole().getName())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    @Transactional
    public IssuedTokens login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userRepository.findActiveAuthUserByUsername(userDetails.getUsername(), UserStatus.ACTIVE)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return issueTokens(user);
    }

    @Transactional
    public IssuedTokens refresh(String refreshToken) {
        String username = jwtService.extractUsernameFromRefreshToken(refreshToken);
        RefreshToken storedToken = refreshTokenService.requireActiveToken(refreshToken, username);
        User user = userRepository.findActiveAuthUserById(storedToken.getUser().getId(), UserStatus.ACTIVE)
                .orElseThrow(() -> new JwtException("User account is not active"));

        refreshTokenService.revoke(storedToken);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revokeByRawToken(refreshToken);
    }

    @Transactional
    public void changePassword(CustomUserDetails currentUser, ChangePasswordRequest request) {
        User user = userRepository.findActiveAuthUserById(currentUser.getUserId(), UserStatus.ACTIVE)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        validateChangePasswordEligibleRole(user);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new ConflictException("New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Đổi mật khẩu xong phải revoke toàn bộ refresh token để buộc các phiên đăng nhập lại.
        refreshTokenRepository.revokeAllActiveByUserId(user.getId(), LocalDateTime.now());
    }

    private IssuedTokens issueTokens(User user) {
        CustomUserDetails userDetails = CustomUserDetails.from(user);
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        refreshTokenService.store(user, refreshToken, jwtService.extractRefreshTokenExpiry(refreshToken));

        return new IssuedTokens(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpiry(),
                userDetails.getUsername(),
                userDetails.getRoleName()
        );
    }

    private void validateChangePasswordEligibleRole(User user) {
        if (user.getRole() == null || user.getRole().getName() == null) {
            throw new ForbiddenException("Role is not allowed to change password");
        }

        String normalizedRole = user.getRole().getName().toUpperCase();
        if (!CHANGE_PASSWORD_ALLOWED_ROLES.contains(normalizedRole)) {
            throw new ForbiddenException("Role is not allowed to change password");
        }
    }
}
