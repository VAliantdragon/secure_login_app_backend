package com.hdfclife.secure_login.login.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryTokenStore {

    // Store username -> token. A user can have multiple active tokens if desired, or just one.
    // For simplicity, let's assume one active token per user for now, storing the token itself.
    // In a real system, you might store a token ID or just the token string.
    private final Set<String> validTokens = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void addToken(String token) {
        validTokens.add(token);
    }

    public boolean isValidToken(String token) {
        return validTokens.contains(token);
    }

    public void removeToken(String token) {
        validTokens.remove(token);
    }

    public int getTokenCount() {
        return validTokens.size();
    }
}