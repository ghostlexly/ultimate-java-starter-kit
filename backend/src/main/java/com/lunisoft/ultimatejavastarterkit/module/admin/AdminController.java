package com.lunisoft.ultimatejavastarterkit.module.admin;

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

  private final AdminService adminService;

  public AdminController(AdminService adminService) {
    this.adminService = adminService;
  }

  @GetMapping("/stats")
  public ResponseEntity<Map<String, Long>> getStats() {
    return ResponseEntity.ok(
        Map.of(
            "accounts", adminService.getAccountCount(),
            "activeSessions", adminService.getActiveSessionCount()));
  }
}
