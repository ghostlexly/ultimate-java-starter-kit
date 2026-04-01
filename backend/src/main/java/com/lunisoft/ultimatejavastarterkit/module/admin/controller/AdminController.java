package com.lunisoft.ultimatejavastarterkit.module.admin.controller;

import com.lunisoft.ultimatejavastarterkit.module.admin.usecase.GetStatsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

  private final GetStatsUseCase getStatsUseCase;

  public AdminController(GetStatsUseCase getStatsUseCase) {
    this.getStatsUseCase = getStatsUseCase;
  }

  @GetMapping("/stats")
  public ResponseEntity<Map<String, Long>> getStats() {

    return ResponseEntity.ok(getStatsUseCase.execute());
  }
}
