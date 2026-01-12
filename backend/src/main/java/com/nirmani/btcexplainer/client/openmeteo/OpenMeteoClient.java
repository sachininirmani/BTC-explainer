package com.nirmani.btcexplainer.client.openmeteo;

import com.nirmani.btcexplainer.util.HttpJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenMeteoClient {

  private final HttpJson http;
  private final ObjectMapper om = new ObjectMapper();

  public OpenMeteoClient(HttpJson http) {
    this.http = http;
  }

  /**
   * Fetch daily weather for a location for a single date.
   * We use this only to demonstrate "extreme weather" signals (optional factor).
   */
  public DailyWx fetchDaily(LocalDate date, double lat, double lon) {
    String url = "https://api.open-meteo.com/v1/forecast"
        + "?latitude=" + lat
        + "&longitude=" + lon
        + "&start_date=" + date
        + "&end_date=" + date
        + "&daily=temperature_2m_max,precipitation_sum,wind_speed_10m_max"
        + "&timezone=UTC";
    try {
      String body = http.get("open-meteo", url).getBody();
      JsonNode root = om.readTree(body);
      JsonNode daily = root.path("daily");
      if (daily.isMissingNode()) return null;

      double tmax = daily.path("temperature_2m_max").get(0).asDouble();
      double precip = daily.path("precipitation_sum").get(0).asDouble();
      double wind = daily.path("wind_speed_10m_max").get(0).asDouble();
      return new DailyWx(tmax, precip, wind);
    } catch (Exception ignore) {}
    return null;
  }

  public record DailyWx(double tmaxC, double precipMm, double windMaxKmh) {}
}
