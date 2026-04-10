package org.example.goldenheartrestaurant.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
/**
 * Turns Authorization Bearer token into Spring Authentication once per request.
 *
 * After this filter runs successfully, controller methods can use @AuthenticationPrincipal and
 * @PreAuthorize against the populated SecurityContext.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Access token được client gửi theo dạng:
        // Authorization: Bearer eyJhbGciOi...
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String accessToken = authorizationHeader.substring(7);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                // JwtService handles signature validation, token-type checking and DB-backed user reload.
                Authentication authentication = jwtService.buildAuthentication(accessToken);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // These endpoints are responsible for issuing/refreshing tokens, so they stay publicly reachable.
        return request.getServletPath().startsWith("/api/v1/auth/");
    }
}
