package com.nirmani.btcexplainer.client.frankfurter;

import com.nirmani.btcexplainer.util.HttpJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class FxClient {
  private final HttpJson http;
  private final ObjectMapper om = new ObjectMapper();

  public FxClient(HttpJson http) {
    this.http = http;
  }

  public BigDecimal fetchEurUsd(LocalDate date) {
    // Frankfurter base=EUR, to=USD
    String url = "https://api.frankfurter.app/" + date + "?from=EUR&to=USD";
    try {
      String body = http.get("frankfurter", url).getBody();
      JsonNode root = om.readTree(body);
      JsonNode rate = root.path("rates").path("USD");
      if (!rate.isMissingNode()) return new BigDecimal(rate.asText());
    } catch (Exception ignore) {}
    return null;
  }
}
