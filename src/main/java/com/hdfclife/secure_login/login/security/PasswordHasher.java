package com.hdfclife.secure_login.login.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHasher {
    /**
     * Run this main method to generate BCrypt hashed passwords.
     */
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10); // 10 is a good cost factor

        String rawPassword1 = "userpass";
        String encodedPassword1 = encoder.encode(rawPassword1);

        String rawPassword2 = "adminpass";
        String encodedPassword2 = encoder.encode(rawPassword2);

        System.out.println("--- Generated BCrypt Hashes ---");
        System.out.println("Username: user");
        System.out.println("Raw Password: " + rawPassword1);
        System.out.println("BCrypt Hashed Password: " + encodedPassword1);
        System.out.println();
        System.out.println("Username: admin");
        System.out.println("Raw Password: " + rawPassword2);
        System.out.println("BCrypt Hashed Password: " + encodedPassword2);
        System.out.println("---------------------------------");
        System.out.println("Copy the hashed password strings (starting with $2a$...) into your users.json file.");
    }
}