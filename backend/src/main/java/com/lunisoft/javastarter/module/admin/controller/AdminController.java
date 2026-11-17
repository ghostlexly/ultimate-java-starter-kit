package com.lunisoft.javastarter.module.admin.controller;

import com.lunisoft.javastarter.module.admin.usecase.GetStatsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final GetStatsUseCase getStatsUseCase;

    @GetMapping("stats")
    public ResponseEntity<GetStatsUseCase.Output> getStats() {
        var output = this.getStatsUseCase.execute();

        return ResponseEntity.ok(output);
    }
}
