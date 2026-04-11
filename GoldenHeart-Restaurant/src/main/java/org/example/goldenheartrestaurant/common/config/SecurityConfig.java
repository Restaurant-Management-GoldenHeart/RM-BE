package org.example.goldenheartrestaurant.common.config;

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

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
/**
 * Cấu hình Spring Security chính cho toàn bộ hệ thống.
 *
 * Dự án đi theo hướng stateless:
 * - login một lần bằng username/password
 * - các request sau mang access token JWT
 * - server không giữ session đăng nhập kiểu truyền thống
 */
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // API dùng JWT nên không cần CSRF token kiểu form truyền thống.
                .csrf(AbstractHttpConfigurer::disable)
                // Tắt form login mặc định của Spring vì đây là REST API.
                .formLogin(AbstractHttpConfigurer::disable)
                // Không dùng HTTP Basic vì dự án đã có JWT.
                .httpBasic(AbstractHttpConfigurer::disable)
                // Mỗi request tự mang danh tính qua JWT, không tạo session phía server.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Các endpoint auth phải mở public để user còn login / refresh / logout được.
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Endpoint nào không nằm trong nhóm public thì bắt buộc phải xác thực.
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                // Filter JWT phải chạy trước filter username/password mặc định
                // để SecurityContext có dữ liệu user từ sớm.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        // Provider này làm hai việc chính:
        // - load user qua CustomUserDetailsService
        // - so sánh password raw với password hash bằng BCrypt
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
        // BCrypt cost = 12 là mức khá cân bằng:
        // - đủ an toàn cho ứng dụng web thông thường
        // - không quá nặng để login bị chậm quá mức
        return new BCryptPasswordEncoder(12);
    }
}
