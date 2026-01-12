package com.nirmani.btcexplainer.domain.ops;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ApiCallLogRepository {
  private final JdbcTemplate jdbc;

  public ApiCallLogRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void log(String provider, String endpoint, Integer status, Integer latencyMs, String error) {
    jdbc.update("INSERT INTO api_call_log(provider, endpoint, http_status, latency_ms, error_message) VALUES (?,?,?,?,?)",
        provider, endpoint, status, latencyMs, error);
  }
}
