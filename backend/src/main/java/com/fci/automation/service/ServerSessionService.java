package com.fci.automation.service;

import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class ServerSessionService {

    // Generated once at startup
    private final String sessionId = UUID.randomUUID().toString();

    public String getSessionId() {
        return sessionId;
    }
}
