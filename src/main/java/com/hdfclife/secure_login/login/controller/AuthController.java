package com.hdfclife.secure_login.login.controller;

import com.hdfclife.secure_login.login.model.LoginRequest;
import com.hdfclife.secure_login.login.service.AuthService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/auth") // Prefix all auth endpoints for clarity
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    // Simulate an external API call for resilience patterns
    // This method can simulate failure to trigger circuit breaker
    private void callExternalLoginApi(String username) {
        // For demonstration, let's make this fail for a specific user to test the circuit breaker
        if ("circuitbreakeruser".equals(username)) {
            logger.error("Simulating external API failure for user: {}", username);
            throw new RuntimeException("External login service is down!");
        }
        logger.debug("Simulating successful external API call for user: {}", username);
        // In a real system, this would make an actual HTTP call
    }

    @PostMapping("/login")
    @RateLimiter(name = "loginRateLimiter", fallbackMethod = "loginRateLimiterFallback")
    @CircuitBreaker(name = "loginCircuitBreaker", fallbackMethod = "loginCircuitBreakerFallback")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest loginRequest) {
        logger.info("Login attempt received for user: {}", loginRequest.getUsername());

        // Integrate with mock external API call before processing internal login
        try {
            callExternalLoginApi(loginRequest.getUsername());
        } catch (Exception e) {
            logger.error("External login API call failed/simulated failure for user: {}: {}", loginRequest.getUsername(), e.getMessage());
            // Re-throw to allow Circuit Breaker to intercept
            throw e;
        }

        String token = authService.login(loginRequest.getUsername(), loginRequest.getPassword());
        return ResponseEntity.ok(Collections.singletonMap("token", token));
    }

    public ResponseEntity<Map<String, String>> loginRateLimiterFallback(LoginRequest loginRequest, Throwable t) {
        logger.warn("Login request for user {} rejected by Rate Limiter: {}", loginRequest.getUsername(), t.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Collections.singletonMap("error", "Too many login attempts. Please try again later."));
    }

    public ResponseEntity<Map<String, String>> loginCircuitBreakerFallback(LoginRequest loginRequest, Throwable t) {
        logger.error("Login request for user {} rejected by Circuit Breaker: {}", loginRequest.getUsername(), t.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.singletonMap("error", "Login service is temporarily unavailable. Please try again."));
    }


    @GetMapping("/validate") // Renamed from /auth for clarity
    public ResponseEntity<Map<String, String>> validateToken(@RequestHeader("Authorization") String authorizationHeader) {
        logger.info("Token validation request received.");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            String username = authService.validateTokenAndGetUserInfo(token);
            if (username != null) {
                logger.info("User {} successfully validated token.", username);
                return ResponseEntity.ok(Collections.singletonMap("username", username)); // Return username to frontend
            }
        }
        logger.warn("Token validation failed due to invalid, missing, or expired token.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "Invalid or expired token"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String authorizationHeader) {
        logger.info("Logout attempt received.");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            if (authService.logout(token)) {
                logger.info("Token successfully invalidated.");
                return ResponseEntity.ok(Collections.singletonMap("message", "Logged out successfully"));
            }
        }
        logger.warn("Logout failed: Invalid or missing token provided.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", "Invalid or missing token"));
    }

    // A protected endpoint to demonstrate authentication
    @GetMapping("/protected")
    public ResponseEntity<Map<String, String>> protectedResource(@RequestHeader("Authorization") String authorizationHeader) {
        logger.info("Access to protected resource requested.");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            String username = authService.validateTokenAndGetUserInfo(token);
            if (username != null) {
                logger.info("User {} successfully accessed protected resource.", username);
                return ResponseEntity.ok(Collections.singletonMap("message", "Welcome to the highly secured area, " + username + "!"));
            }
        }
        logger.warn("Unauthorized access attempt to protected resource.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "Unauthorized"));
    }
}