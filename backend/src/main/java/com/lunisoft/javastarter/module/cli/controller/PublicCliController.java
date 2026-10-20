package com.lunisoft.javastarter.module.cli.controller;

import com.lunisoft.javastarter.core.security.PublicEndpoint;
import com.lunisoft.javastarter.module.cli.usecase.TestUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@PublicEndpoint
@RequestMapping("/api/cli")
public class PublicCliController {

    private final TestUseCase testUseCase;

    public PublicCliController(TestUseCase testUseCase) {
        this.testUseCase = testUseCase;
    }

    public ResponseEntity<Map<String, String>> getTest() {
        testUseCase.execute();

        return ResponseEntity.ok(Map.of("message", "OK"));
    }
}
