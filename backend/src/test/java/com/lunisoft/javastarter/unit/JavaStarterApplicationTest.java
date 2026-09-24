package com.lunisoft.javastarter.unit;

import com.lunisoft.javastarter.JavaStarterApplication ;
import org.junit.jupiter.api.Test;

class JavaStarterApplicationTest {

    @Test
    void application_class_exists() {
        // Smoke test — full context loading requires database + JWT keys.
        // Use Testcontainers for integration tests.
        JavaStarterApplication.class.getName();
    }
}
