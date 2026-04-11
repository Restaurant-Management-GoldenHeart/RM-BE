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

@Component
@RequiredArgsConstructor
/**
 * Filter chạy 1 lần cho mỗi request để biến Bearer token thành Authentication.
 *
 * Sau khi filter này chạy xong:
 * - SecurityContextHolder sẽ có current user
 * - @AuthenticationPrincipal lấy được user hiện tại
 * - @PreAuthorize có dữ liệu để kiểm tra role
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Access token được client gửi theo dạng:
        // Authorization: Bearer <jwt>
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String accessToken = authorizationHeader.substring(7);

            // Chỉ dựng Authentication nếu request hiện tại chưa có user trong SecurityContext.
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    // JwtService sẽ:
                    // 1. kiểm tra chữ ký token
                    // 2. kiểm tra token type
                    // 3. load lại user thật từ DB
                    // 4. dựng Authentication cho Spring Security
                    Authentication authentication = jwtService.buildAuthentication(accessToken);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (JwtException | IllegalArgumentException exception) {
                    // Nếu token lỗi thì trả luôn 401, không cho request đi tiếp xuống controller.
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
        // Nhóm endpoint auth là nơi phát token / refresh token,
        // nên không đi qua bước ép xác thực của filter này.
        return request.getServletPath().startsWith("/api/v1/auth/");
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        // Escape dấu " để response JSON luôn hợp lệ.
        String safeMessage = message == null ? "Unauthorized" : message.replace("\"", "\\\"");
        response.getWriter().write("{\"success\":false,\"message\":\"" + safeMessage + "\"}");
    }
}
