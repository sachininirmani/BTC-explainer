package com.nirmani.btcexplainer.domain.ops;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RateLimitViolationRepository {
  private final JdbcTemplate jdbc;

  public RateLimitViolationRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void record(String ip, String path) {
    jdbc.update("INSERT INTO rate_limit_violations(ip, path) VALUES (?,?)", ip, path);
  }
}
