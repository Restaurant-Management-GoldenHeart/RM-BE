package org.example.goldenheartrestaurant.modules.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.common.security.JwtService;
import org.example.goldenheartrestaurant.modules.auth.entity.RefreshToken;
import org.example.goldenheartrestaurant.modules.auth.dto.request.LoginRequest;
import org.example.goldenheartrestaurant.modules.auth.dto.request.RegisterRequest;
import org.example.goldenheartrestaurant.modules.auth.dto.response.RegisterResponse;
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

@Service
@RequiredArgsConstructor
/**
 * Service điều phối toàn bộ nghiệp vụ xác thực:
 * - register
 * - login
 * - refresh
 * - logout
 *
 * Đây là nơi nối 4 phần lại với nhau:
 * - User / UserProfile trong DB
 * - Spring Security AuthenticationManager
 * - JwtService
 * - RefreshTokenService
 */
public class AuthService {

    private static final String DEFAULT_CUSTOMER_ROLE = "CUSTOMER";

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    /**
     * Luồng đăng ký:
     * 1. kiểm tra username/email đã tồn tại hay chưa
     * 2. hash password bằng BCrypt
     * 3. gán role mặc định CUSTOMER
     * 4. lưu cả User và UserProfile trong cùng transaction
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        // Đăng ký phải tạo đồng thời:
        // - bản ghi đăng nhập (User)
        // - bản ghi hồ sơ cơ bản (UserProfile)
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

    /**
     * AuthenticationManager sẽ dùng UserDetailsService + PasswordEncoder
     * để xác thực username/password theo chuẩn của Spring Security.
     */
    @Transactional
    public IssuedTokens login(LoginRequest request) {
        // Cho Spring Security tự xử lý so sánh password
        // thay vì tự so thủ công trong service.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userRepository.findActiveAuthUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return issueTokens(user);
    }

    /**
     * Luồng refresh token:
     * - validate refresh token ở cả tầng JWT lẫn DB
     * - lấy username từ token
     * - load user mới nhất từ DB
     * - revoke refresh token cũ
     * - phát access token mới + refresh token mới
     */
    @Transactional
    public IssuedTokens refresh(String refreshToken) {
        // Kiểm tra 2 lớp:
        // 1. token phải parse và verify được
        // 2. token phải còn active trong DB
        String username = jwtService.extractUsernameFromRefreshToken(refreshToken);
        RefreshToken storedToken = refreshTokenService.requireActiveToken(refreshToken, username);
        User user = storedToken.getUser();

        // Mỗi lần refresh thành công sẽ vô hiệu hóa refresh token cũ.
        // Đây là kỹ thuật rotation để giảm rủi ro nếu refresh token bị lộ.
        refreshTokenService.revoke(storedToken);

        return issueTokens(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        // Logout chỉ cần revoke refresh token hiện tại là đủ.
        refreshTokenService.revokeByRawToken(refreshToken);
    }

    private IssuedTokens issueTokens(User user) {
        CustomUserDetails userDetails = CustomUserDetails.from(user);
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // Lưu trạng thái refresh token ở DB để còn:
        // - logout
        // - revoke
        // - rotate
        // - audit session
        refreshTokenService.store(user, refreshToken, jwtService.extractRefreshTokenExpiry(refreshToken));

        return new IssuedTokens(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpiry(),
                userDetails.getUsername(),
                userDetails.getRoleName()
        );
    }
}
