package com.hdfclife.secure_login.login.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdfclife.secure_login.login.model.User;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class UserRepository {

    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);

    @Value("${user.data.file}")
    private String userDataFile;

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();

    public UserRepository(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try {
            // Load from classpath first, then filesystem if not found
            Resource resource = resourceLoader.getResource("classpath:" + userDataFile);
            if (!resource.exists()) {
                resource = resourceLoader.getResource("file:" + userDataFile);
            }

            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    List<User> userList = objectMapper.readValue(is, new TypeReference<List<User>>() {});
                    userList.forEach(user -> users.put(user.getUsername(), user));
                    logger.info("Loaded {} users from {}", users.size(), resource.getFilename());
                    users.values().forEach(user -> logger.debug("Loaded user: {}", user.getUsername()));
                }
            } else {
                logger.warn("User data file '{}' not found. No users loaded.", userDataFile);
            }
        } catch (IOException e) {
            logger.error("Error loading user data from {}: {}", userDataFile, e.getMessage(), e);
        }
    }

    public Optional<User> findByUsername(String username) {
        logger.debug("Attempting to find user by username: {}", username);
        return Optional.ofNullable(users.get(username));
    }
}