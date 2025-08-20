package com.example.loginservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Container Login Service Application
 *
 * This service communicates with azure-entra-auth-test to provide
 * user data with pagination support.
 *
 * 12-Factor App Principles:
 * - Stateless: No local state, all data from external service
 * - Config: Environment-based configuration
 * - Logs: All logs to stdout
 * - Port Binding: Self-contained service
 */
@SpringBootApplication
public class LoginServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoginServiceApplication.class, args);
    }
}