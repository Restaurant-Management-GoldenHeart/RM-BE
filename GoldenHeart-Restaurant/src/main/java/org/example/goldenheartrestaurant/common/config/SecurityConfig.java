package org.example.goldenheartrestaurant.common.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.security.CustomUserDetailsService;
import org.example.goldenheartrestaurant.common.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Cấu hình bảo mật trung tâm của toàn bộ ứng dụng.
 *
 * Ý tưởng chính của dự án này là stateless:
 * - người dùng đăng nhập để lấy JWT
 * - các request protected tự mang JWT ở header Authorization
 * - server không giữ session đăng nhập kiểu cũ
 *
 * Vì thế lớp này quyết định ba việc rất quan trọng:
 * 1. endpoint nào được public, endpoint nào phải xác thực
 * 2. filter JWT được đặt ở đâu trong chuỗi filter của Spring Security
 * 3. hệ thống phản hồi thế nào khi token thiếu, sai hoặc hết hạn
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // REST API dùng JWT nên không cần CSRF token kiểu form submit truyền thống.
                .csrf(AbstractHttpConfigurer::disable)
                // Không dùng form login mặc định của Spring Security.
                .formLogin(AbstractHttpConfigurer::disable)
                // Không dùng HTTP Basic vì danh tính đã đi qua JWT.
                .httpBasic(AbstractHttpConfigurer::disable)
                // Cho phép frontend ở domain/port khác gọi API khi chạy local hoặc tách FE/BE.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Mỗi request tự mang token nên phía server không tạo session.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Chuẩn hóa lỗi chưa xác thực thành JSON 401 để frontend xử lý dễ hơn.
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            String safeMessage = authException.getMessage() == null
                                    ? "Unauthorized"
                                    : authException.getMessage().replace("\"", "\\\"");
                            response.getWriter().write("{\"success\":false,\"message\":\"" + safeMessage + "\"}");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // Nhóm endpoint auth public thật sự:
                        // - login/register: chưa có user hiện tại
                        // - refresh/logout: dùng refresh token cookie
                        // - password recovery: người dùng chưa đăng nhập vẫn phải thao tác được
                        .requestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout",
                                "/api/v1/auth/password-recovery/**",
                                "/api/v1/payment-gateways/payos/webhook",
                                // Endpoint public cho homepage: xem đánh giá món (không cần đăng nhập)
                                "/api/v1/public/**"
                        ).permitAll()
                        // Đổi mật khẩu là thao tác của user đã đăng nhập.
                        .requestMatchers("/api/v1/auth/change-password").authenticated()
                        .requestMatchers("/error").permitAll()
                        // Preflight CORS luôn phải đi qua được.
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        // Mọi API khác đều bắt buộc authenticated.
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                // JWT filter phải chạy sớm để SecurityContext có current user
                // trước khi tới tầng @Secured / @PreAuthorize.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        // DaoAuthenticationProvider lo hai việc:
        // 1. load user từ DB qua CustomUserDetailsService
        // 2. đối chiếu mật khẩu raw với mật khẩu đã hash bằng BCrypt
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Cost 12 là mức đủ an toàn cho backend doanh nghiệp thông thường
        // mà vẫn giữ thời gian login ở mức chấp nhận được.
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Dùng allowedOriginPatterns("*") để tiện test local với nhiều origin khác nhau.
        // Vì đang bật allowCredentials=true nên dùng allowedOriginPatterns sẽ linh hoạt hơn.
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
