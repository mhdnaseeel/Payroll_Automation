package com.fci.automation.controller;

import com.fci.automation.dto.LoginRequest;
import com.fci.automation.dto.LoginResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @org.springframework.beans.factory.annotation.Autowired
    private com.fci.automation.service.ServerSessionService sessionService;

    // Mock Login for Prototype
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        LoginResponse response = new LoginResponse();

        if ("admin".equals(request.getUsername()) && "admin123".equals(request.getPassword())) {
            response.setRole("ADMIN");
            response.setToken("mock-admin-token");
            response.setMessage("Success");
        } else if ("user".equals(request.getUsername()) && "user123".equals(request.getPassword())) {
            response.setRole("USER");
            response.setToken("mock-user-token");
            response.setMessage("Success");
        } else if ("bill".equals(request.getUsername()) && "bill123".equals(request.getPassword())) {
            response.setRole("BILL");
            response.setToken("mock-bill-token");
            response.setMessage("Success");
        } else {
            throw new RuntimeException("Invalid Credentials");
        }

        // Return the current server session ID appended to token
        // This makes the token unique to this server run
        response.setToken(response.getToken() + ":" + sessionService.getSessionId());

        return response;
    }

    @GetMapping("/status")
    public java.util.Map<String, String> getStatus() {
        return java.util.Map.of("status", "UP", "sessionId", sessionService.getSessionId());
    }
}
