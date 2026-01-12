package com.nirmani.btcexplainer.api.controller;

import com.nirmani.btcexplainer.service.jobs.DailyRefreshJob;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {

  private final DailyRefreshJob job;

  @Value("${app.admin-token}")
  private String adminToken;

  public AdminController(DailyRefreshJob job) {
    this.job = job;
  }

  @PostMapping("/api/admin/refresh")
  public ResponseEntity<?> refresh(HttpServletRequest req) {
    String token = req.getHeader("X-Admin-Token");
    if (token == null || !token.equals(adminToken)) {
      return ResponseEntity.status(401).body(java.util.Map.of("error", "unauthorized"));
    }
    job.runDaily();
    return ResponseEntity.ok(java.util.Map.of("status", "ok"));
  }
}
