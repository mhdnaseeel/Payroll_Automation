package com.fci.automation.controller;

import com.fci.automation.dto.LoginRequest;
import com.fci.automation.dto.LoginResponse;
import com.fci.automation.dto.TokenRefreshRequest;
import com.fci.automation.dto.TokenRefreshResponse;
import com.fci.automation.entity.RefreshToken;
import com.fci.automation.entity.User;
import com.fci.automation.security.jwt.JwtUtils;
import com.fci.automation.security.services.RefreshTokenService;
import com.fci.automation.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
// @CrossOrigin(origins = "http://localhost:4200") // Handled globally in
// WebSecurityConfig
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // ROUTING LOGIC: Determine Realm BEFORE Authentication
            if (loginRequest.getUsername().startsWith("test")) {
                com.fci.automation.config.RealmContext.setRealm(com.fci.automation.config.RealmEnum.TEST);
            } else {
                com.fci.automation.config.RealmContext.setRealm(com.fci.automation.config.RealmEnum.REAL);
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            String jwt = jwtUtils.generateJwtToken(userDetails);

            RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());

            List<String> roles = userDetails.getAuthorities().stream()
                    .map(item -> item.getAuthority())
                    .collect(Collectors.toList());

            return ResponseEntity.ok(LoginResponse.builder()
                    .token(jwt)
                    .refreshToken(refreshToken.getToken())
                    .role(roles.get(0)) // Assuming single role
                    .message("Success")
                    .build());
        } finally {
            com.fci.automation.config.RealmContext.clear();
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = jwtUtils.generateTokenFromUsername(user.getUsername());
                    return ResponseEntity.ok(new TokenRefreshResponse(token, requestRefreshToken));
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }

    @GetMapping("/status")
    public java.util.Map<String, String> getStatus() {
        return java.util.Map.of("status", "UP");
    }
}
