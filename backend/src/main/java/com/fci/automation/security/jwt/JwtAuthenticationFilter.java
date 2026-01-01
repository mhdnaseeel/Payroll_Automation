package com.fci.automation.security.jwt;

import com.fci.automation.security.services.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {

                // 1. EXTRACT REALM from Token
                String realmStr = jwtUtils.getRealmFromJwtToken(jwt);
                com.fci.automation.config.RealmEnum realm = com.fci.automation.config.RealmEnum.valueOf(realmStr);

                // 2. SET CONTEXT (Crucial: Database Switching happens here)
                com.fci.automation.config.RealmContext.setRealm(realm);

                String username = jwtUtils.getUserNameFromJwtToken(jwt);
                String role = jwtUtils.getRoleFromJwtToken(jwt); // Extract Role

                // OPTIMIZED: Create Principal directly without DB call
                java.util.List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities = java.util.Collections
                        .singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority(role));

                // Use a simple UserDetails implementation or just the required principal info
                org.springframework.security.core.userdetails.User principal = new org.springframework.security.core.userdetails.User(
                        username, "", authorities);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
        } finally {
            // Safety: Ensure we don't leak context to next request in pool
            // Note: We cannot clear here because the Controller needs the context
            // downstream to query DB.
            // Ideally, clear AFTER the request is fully processed.
            // However, for OnePerRequestFilter, the chain.doFilter happens inside.
            // So we should try-finally around the chain.
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            com.fci.automation.config.RealmContext.clear();
        }
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}
