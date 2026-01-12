package com.nirmani.btcexplainer.client.alternative;

import com.nirmani.btcexplainer.util.HttpJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import org.springframework.stereotype.Component;

@Component
public class FearGreedClient {
  private final HttpJson http;
  private final ObjectMapper om = new ObjectMapper();

  public FearGreedClient(HttpJson http) {
    this.http = http;
  }

  public FngPoint fetchLatest() {
    String url = "https://api.alternative.me/fng/?limit=1&format=json";
    try {
      String body = http.get("alternative", url).getBody();
      JsonNode root = om.readTree(body);
      JsonNode data = root.path("data");
      if (data.isArray() && data.size() > 0) {
        JsonNode d = data.get(0);
        int value = Integer.parseInt(d.path("value").asText("0"));
        String classification = d.path("value_classification").asText("");
        long ts = Long.parseLong(d.path("timestamp").asText("0"));
        LocalDate date = Instant.ofEpochSecond(ts).atZone(ZoneOffset.UTC).toLocalDate();
        return new FngPoint(date, value, classification);
      }
    } catch (Exception ignore) {}
    return null;
  }

  public record FngPoint(LocalDate date, int value, String classification) {}
}
