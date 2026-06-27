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
import org.example.goldenheartrestaurant.modules.auth.dto.response.ChangePasswordResponse;
import org.example.goldenheartrestaurant.modules.auth.dto.response.RegisterResponse;
import org.example.goldenheartrestaurant.modules.auth.entity.RefreshToken;
import org.example.goldenheartrestaurant.modules.auth.repository.RefreshTokenRepository;
import org.example.goldenheartrestaurant.modules.customer.entity.Customer;
import org.example.goldenheartrestaurant.modules.customer.repository.CustomerRepository;
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
import java.util.Objects;
import java.util.Optional;
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
    private final CustomerRepository customerRepository;
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

        /*
         * Với role CUSTOMER: liên kết hoặc tạo mới bản ghi Customer CRM.
         *
         * ── Luồng liên kết hồ sơ walk-in (Account Claiming) ──────────────────────────
         * Thực tế vận hành: staff tạo nhanh khách hàng tại POS chỉ bằng tên + SĐT
         * (không có email). Sau này khách muốn tạo tài khoản online để xem lịch sử
         * và giữ lại điểm tích lũy → họ cung cấp SĐT đã đăng ký với nhà hàng khi đăng ký.
         *
         * Điều kiện để liên kết thành công (phải thỏa cả hai):
         *   1. SĐT khớp với bản ghi CRM walk-in (userId == null).
         *   2. Họ tên khớp (không phân biệt hoa thường, bỏ khoảng trắng thừa) —
         *      bảo vệ tránh trường hợp ai đó vô tình claim hồ sơ chỉ vì biết SĐT.
         *
         * Nếu không cung cấp SĐT hoặc không tìm thấy hồ sơ khớp → tạo CRM mới.
         * Email được thêm vào CRM khi khách cập nhật hồ sơ sau khi đăng nhập.
         * ─────────────────────────────────────────────────────────────────────────────
         */
        boolean existingCrmLinked = false;
        int inheritedPoints = 0;

        if (DEFAULT_CUSTOMER_ROLE.equalsIgnoreCase(savedUser.getRole().getName())) {

            Optional<Customer> walkInCrm = Optional.empty();

            if (request.getPhone() != null && !request.getPhone().isBlank()) {
                walkInCrm = customerRepository
                        .findWalkInByActivePhone(request.getPhone().trim())
                        .filter(crm -> {
                            // Xác minh họ tên để ngăn claim nhầm hồ sơ người khác
                            String nameFromRequest = request.getFullName().trim();
                            String nameInCrm       = crm.getName().trim();
                            return nameInCrm.equalsIgnoreCase(nameFromRequest);
                        });
            }

            if (walkInCrm.isPresent()) {
                // ── Claim thành công: link tài khoản vào hồ sơ walk-in, giữ nguyên điểm ──
                Customer crm = walkInCrm.get();
                inheritedPoints = crm.getLoyaltyPoints() != null ? crm.getLoyaltyPoints() : 0;
                crm.setUserId(savedUser.getId());
                // Không ghi email vào CRM ở đây — khách tự cập nhật trong phần hồ sơ sau
                customerRepository.save(crm);
                existingCrmLinked = true;
            } else {
                // ── Khách mới hoặc không tìm thấy hồ sơ khớp → tạo CRM trắng ──
                Customer newCrm = Customer.builder()
                        .userId(savedUser.getId())
                        .name(request.getFullName())
                        .email(request.getEmail())
                        .activeEmail(request.getEmail())
                        .loyaltyPoints(0)
                        .build();
                customerRepository.save(newCrm);
            }
        }

        return RegisterResponse.builder()
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getProfile().getEmail())
                .fullName(savedUser.getProfile().getFullName())
                .role(savedUser.getRole().getName())
                .createdAt(savedUser.getCreatedAt())
                .existingCrmLinked(existingCrmLinked)
                .inheritedLoyaltyPoints(existingCrmLinked ? inheritedPoints : null)
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
    public ChangePasswordResponse changePassword(CustomUserDetails currentUser, ChangePasswordRequest request) {
        User user = userRepository.findActiveAuthUserById(currentUser.getUserId(), UserStatus.ACTIVE)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        validateChangePasswordEligibleRole(user);
        validatePasswordConfirmation(request);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new ConflictException("New password must be different from current password");
        }

        LocalDateTime changedAt = LocalDateTime.now();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(changedAt);
        userRepository.save(user);

        // Đổi mật khẩu xong phải revoke toàn bộ refresh token để buộc các phiên đăng nhập lại.
        int revokedSessionCount = refreshTokenRepository.revokeAllActiveByUserId(user.getId(), changedAt);

        return ChangePasswordResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().getName())
                .changedAt(changedAt)
                .revokedSessionCount(revokedSessionCount)
                .requiresLoginAgain(true)
                .build();
    }

    /**
     * Tạo Customer CRM cho tất cả User có role CUSTOMER nhưng chưa có bản ghi CRM.
     * Endpoint admin gọi một lần để đồng bộ dữ liệu sau khi nâng cấp hệ thống.
     *
     * @return số lượng bản ghi Customer được tạo mới
     */
    @Transactional
    public int syncOrphanedCustomers() {
        var orphaned = userRepository.findCustomerUsersWithoutCrmRecord();

        for (User user : orphaned) {
            String name  = user.getProfile() != null ? user.getProfile().getFullName() : user.getUsername();
            String email = user.getProfile() != null ? user.getProfile().getEmail() : null;

            Customer customerCrm = Customer.builder()
                    .userId(user.getId())
                    .name(name != null ? name : user.getUsername())
                    .email(email)
                    .activeEmail(email)
                    .loyaltyPoints(0)
                    .build();
            customerRepository.save(customerCrm);
        }

        return orphaned.size();
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

    private void validatePasswordConfirmation(ChangePasswordRequest request) {
        if (!Objects.equals(request.getNewPassword(), request.getConfirmNewPassword())) {
            throw new BadRequestException("Password confirmation does not match");
        }
    }
}
