package com.lunisoft.javastarter;

import org.junit.jupiter.api.Test;

class JavaStarterApplicationTests {

    @Test
    void application_class_exists() {
        // Smoke test — full context loading requires database + JWT keys.
        // Use Testcontainers for integration tests.
        JavaStarterApplication.class.getName();
    }
}
