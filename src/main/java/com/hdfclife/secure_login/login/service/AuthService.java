package com.hdfclife.secure_login.login.service;

import com.hdfclife.secure_login.login.exception.InvalidCredentialsException;
import com.hdfclife.secure_login.login.exception.UserNotFoundException;
import com.hdfclife.secure_login.login.model.User;
import com.hdfclife.secure_login.login.repository.UserRepository;
import com.hdfclife.secure_login.login.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private InMemoryTokenStore tokenStore;

    @Autowired
    private UserRepository userRepository; // Inject our user repository

    @Autowired
    private PasswordEncoder passwordEncoder; // Inject BCrypt password encoder

    public String login(String username, String rawPassword) {
        logger.debug("Login attempt for user: {}", username);

        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            logger.warn("Login failed for user {}: User not found.", username);
            throw new UserNotFoundException("Invalid username or password"); // Use generic message for security
        }

        User user = userOptional.get();

        // Use BCrypt to compare raw password with hashed password
        if (passwordEncoder.matches(rawPassword, user.getPassword())) {
            String token = jwtUtil.generateToken(username);
            tokenStore.addToken(token);
            logger.info("User {} logged in successfully. Token generated.", username);
            return token;
        } else {
            logger.warn("Login failed for user {}: Incorrect password.", username);
            throw new InvalidCredentialsException("Invalid username or password"); // Generic message
        }
    }

    public String validateTokenAndGetUserInfo(String token) {
        if (token == null || token.isEmpty()) {
            logger.warn("Attempted validation with null or empty token.");
            return null;
        }

        String username = jwtUtil.extractUsername(token);

        if (username == null) {
            logger.warn("Token does not contain a valid username. Token: {}", token);
            return null;
        }

        if (!tokenStore.isValidToken(token)) {
            logger.warn("Attempted validation with blacklisted or non-existent token for user: {}", username);
            return null;
        }

        if (jwtUtil.validateToken(token, username)) {
            logger.debug("Token for user {} is valid and active.", username);
            return username; // Return decoded user info (username)
        } else {
            logger.warn("Token validation failed for user {}. Token: {}", username, token);
            // Optionally remove expired token from store if it somehow remained
            tokenStore.removeToken(token);
            return null;
        }
    }

    public boolean logout(String token) {
        if (token == null || token.isEmpty()) {
            logger.warn("Logout attempted with null or empty token.");
            return false;
        }
        if (tokenStore.isValidToken(token)) {
            tokenStore.removeToken(token);
            String username = null;
            try {
                username = jwtUtil.extractUsername(token); // Attempt to extract username for logging
            } catch (Exception e) {
                logger.debug("Could not extract username from token during logout (may be malformed): {}", e.getMessage());
            }
            logger.info("User {} logged out successfully. Token removed from in-memory store.", username != null ? username : "unknown");
            return true;
        }
        logger.warn("Logout attempted with an invalid or already invalidated token.");
        return false;
    }
}