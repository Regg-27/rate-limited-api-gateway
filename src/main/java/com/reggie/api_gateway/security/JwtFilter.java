package com.reggie.api_gateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private RateLimiter limit;

    public JwtFilter(JwtService jwtService, RateLimiter limit) {
        this.jwtService = jwtService;
        this.limit = limit;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        if (Objects.equals(request.getServletPath(), "/login")) {
            filterChain.doFilter(request, response);
            return;
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            return;
        }
        String token = authHeader.substring(7);
        try {
            String username = jwtService.validateToken(token);
            if (!limit.isAllowed(username)) {
                response.setStatus(429);
                return;
            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            response.setStatus(401);
        }
    }
}