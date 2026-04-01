// Copyright © 2026 Alan Amaral
// All rights reserved.
//
// Unauthorized copying, modification, distribution, or use of this software,
// via any medium, is strictly prohibited without prior written permission.
//
// Description:
// Spring Boot application entry point for the Mille Bornes multiplayer card game server.

package com.millebornes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MilleBornesApplication {
    public static void main(String[] args) {
        SpringApplication.run(MilleBornesApplication.class, args);
    }
}
