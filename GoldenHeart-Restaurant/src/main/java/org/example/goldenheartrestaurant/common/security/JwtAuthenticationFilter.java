package org.example.goldenheartrestaurant.common.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Filter chạy đúng một lần cho mỗi request để biến Bearer token thành Authentication.
 *
 * Sau khi filter này chạy thành công:
 * - SecurityContextHolder có current user
 * - @AuthenticationPrincipal lấy được user hiện tại
 * - @Secured, @PreAuthorize và các rule phân quyền khác có dữ liệu để kiểm tra role
 *
 * Có thể hiểu ngắn gọn:
 * lớp này là "cầu nối" giữa JWT thô trong header và cơ chế phân quyền của Spring Security.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Danh sách endpoint auth public thật sự.
     *
     * Những endpoint này được bỏ qua JWT filter vì:
     * - người dùng chưa đăng nhập vẫn phải gọi được
     * - chúng không cần current user từ SecurityContext
     *
     * Lưu ý:
     * `change-password` không nằm ở đây vì bắt buộc phải có access token hợp lệ.
     */
    private static final Set<String> PUBLIC_AUTH_ENDPOINTS = Set.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            "/api/v1/auth/password-recovery/request-otp",
            "/api/v1/auth/password-recovery/verify-otp",
            "/api/v1/auth/password-recovery/reset-password"
    );

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Chuẩn header mong đợi:
        // Authorization: Bearer <jwt>
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String accessToken = authorizationHeader.substring(7);

            // Chỉ dựng Authentication nếu request hiện tại chưa có user trong SecurityContext.
            // Điều này tránh set đè Authentication nếu trước đó một filter khác đã xử lý.
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    Authentication authentication = jwtService.buildAuthentication(accessToken);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (JwtException | IllegalArgumentException | org.springframework.security.core.AuthenticationException exception) {
                    // Nếu token lỗi thì trả 401 ngay, không cho request đi sâu xuống controller.
                    SecurityContextHolder.clearContext();
                    writeUnauthorizedResponse(response, exception.getMessage());
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // Chỉ bỏ qua nhóm auth public.
        // Các endpoint cần current user như change-password vẫn phải đi qua filter này.
        return PUBLIC_AUTH_ENDPOINTS.contains(request.getServletPath());
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Escape dấu " để JSON trả về luôn hợp lệ.
        String safeMessage = message == null ? "Unauthorized" : message.replace("\"", "\\\"");
        response.getWriter().write("{\"success\":false,\"message\":\"" + safeMessage + "\"}");
    }
}
