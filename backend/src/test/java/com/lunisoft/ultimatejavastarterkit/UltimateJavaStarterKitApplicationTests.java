package com.lunisoft.ultimatejavastarterkit;

import org.junit.jupiter.api.Test;

class UltimateJavaStarterKitApplicationTests {

    @Test
    void applicationClassExists() {
        // Smoke test — full context loading requires database + JWT keys.
        // Use Testcontainers for integration tests.
        UltimateJavaStarterKitApplication.class.getName();
    }
}
