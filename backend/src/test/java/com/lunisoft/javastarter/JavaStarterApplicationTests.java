package com.lunisoft.javastarter;

import org.junit.jupiter.api.Test;

class JavaStarterApplicationTests {

  @Test
  void applicationClassExists() {
    // Smoke test — full context loading requires database + JWT keys.
    // Use Testcontainers for integration tests.
    JavaStarterApplication.class.getName();
  }
}
