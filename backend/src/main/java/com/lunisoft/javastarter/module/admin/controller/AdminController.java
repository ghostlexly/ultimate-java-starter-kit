package com.lunisoft.javastarter.module.admin.controller;

import com.lunisoft.javastarter.module.admin.usecase.GetStatsUseCase;
import java.util.Map;
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
  public ResponseEntity<Map<String, Long>> getStats() {

    return ResponseEntity.ok(getStatsUseCase.execute());
  }
}
