package com.fci.automation.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @org.springframework.beans.factory.annotation.Autowired
    private com.fci.automation.service.ServerSessionService sessionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 1. Get Token
        String token = request.getHeader("Authorization");

        // Allow OPTIONS for pre-flight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        if (token == null || token.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing Authorization Header");
            return false;
        }

        // 2. Validate Token & Session
        String role = null;
        if (token.contains(":")) {
            // "mock-token:SERVER_SESSION_ID"
            String[] parts = token.split(":");
            String realToken = parts[0];
            String tokenSessionId = parts[1];

            // CRITICAL: Check if this token belongs to the current server run
            if (!sessionService.getSessionId().equals(tokenSessionId)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Session Expired (Server Restarted)");
                return false;
            }

            if ("mock-admin-token".equals(realToken))
                role = "ADMIN";
            else if ("mock-user-token".equals(realToken))
                role = "USER";
            else if ("mock-bill-token".equals(realToken))
                role = "BILL";
        } else {
            // Backward compatibility or legacy tokens? No, strictly enforce session.
            // If no session ID, it's invalid.
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid Token Format");
            return false;
        }

        if (role == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid Token");
            return false;
        }

        // 3. Role-Based Access Control logic
        String uri = request.getRequestURI();

        // BILLING Role Restrictions
        if ("BILL".equals(role)) {
            // Bill users can ONLY access:
            // 1. /api/billing/**
            // 2. /api/auth/** (Login/Status - handled by WebConfig exclusion mostly, but
            // good to allow)
            // 3. READ-ONLY access to necessary data for bill generation?
            // Let's assume for now they ONLY hit /api/billing endpoints which might wrap
            // other service logic
            // OR they might need strictly GET /api/payroll/entries to pick data.

            // Strict Mode: Only /api/billing
            if (!uri.startsWith("/api/billing") && !uri.startsWith("/api/auth")) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("Access Denied: Billing Users can only access Billing Module");
                return false;
            }
            return true;
        }

        // START: Protect /api/billing from ADMIN and USER
        if (uri.startsWith("/api/billing")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Access Denied: Exclusive for Billing Role");
            return false;
        }
        // END: Protect /api/billing

        // Admin-only routes
        if (uri.startsWith("/api/employees") && !request.getMethod().equals("GET")) {
            if (!"ADMIN".equals(role)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("Access Denied: Admins Only");
                return false;
            }
        }

        if (uri.startsWith("/api/employees")) {
            if (request.getMethod().equals("GET")) {
                // Allow USER/ADMIN
            } else {
                // Write operations -> Admin Only
                if (!"ADMIN".equals(role)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return false;
                }
            }
        }

        return true;
    }
}
