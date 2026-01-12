package com.nirmani.btcexplainer.client.gdelt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nirmani.btcexplainer.util.HttpJson;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Minimal GDELT DOC 2.1 client used for the educational BTC explainer.
 *
 * <p>Important semantics:</p>
 * <ul>
 *   <li>{@code mode=TimelineVol} returns a *coverage share* (percentage of all global news) and can be fractional.</li>
 *   <li>{@code mode=TimelineVolRaw} returns the *raw count* of matching articles per time-bucket.</li>
 *   <li>{@code mode=ArtList} returns article records (titles/urls) for a window.</li>
 * </ul>
 */
@Component
public class GdeltClient {

  private final HttpJson http;
  private final ObjectMapper om = new ObjectMapper();

  /** GDELT expects UTC datetimes in yyyyMMddHHmmss */
  private static final DateTimeFormatter GDELT_DT =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

  /** Some GDELT fields (e.g., seendate) can come as yyyyMMddHHmmss */
  private static final DateTimeFormatter GDELT_SEENDATE_COMPACT =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

  public GdeltClient(HttpJson http) {
    this.http = http;
  }

  /**
   * Fetch the raw number of matching articles for a single-day window (UTC day).
   *
   * <p>This uses {@code mode=TimelineVolRaw} and sums all buckets for the window.</p>
   */
  public int fetchNewsCount(LocalDate date, String query) {
    Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).minusSeconds(1).toInstant();

    String url =
        "https://api.gdeltproject.org/api/v2/doc/doc"
            + "?query=" + urlenc(query)
            + "&mode=TimelineVolRaw"
            + "&format=json"
            + "&startdatetime=" + GDELT_DT.format(start)
            + "&enddatetime=" + GDELT_DT.format(end);

    try {
      String body = http.get("gdelt", url).getBody();
      JsonNode root = om.readTree(body);
      JsonNode timeline = root.path("timeline");

      if (!timeline.isArray() || timeline.isEmpty()) return 0;

      long sum = 0;
      for (JsonNode bucket : timeline) {
        // In TimelineVolRaw, "value" is the raw count for that bucket
        sum += bucket.path("value").asLong(0);
      }
      if (sum > Integer.MAX_VALUE) return Integer.MAX_VALUE;
      return (int) sum;
    } catch (Exception e) {
      return 0;
    }
  }

  /**
   * Fetch the coverage percentage (share of all global news) for a single-day window.
   *
   * <p>This is helpful as a qualitative "intensity" metric, but should not be treated as article count.</p>
   */
  public double fetchNewsCoveragePct(LocalDate date, String query) {
    Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).minusSeconds(1).toInstant();

    String url =
        "https://api.gdeltproject.org/api/v2/doc/doc"
            + "?query=" + urlenc(query)
            + "&mode=TimelineVol"
            + "&format=json"
            + "&startdatetime=" + GDELT_DT.format(start)
            + "&enddatetime=" + GDELT_DT.format(end);

    try {
      String body = http.get("gdelt", url).getBody();
      JsonNode root = om.readTree(body);
      JsonNode timeline = root.path("timeline");
      if (!timeline.isArray() || timeline.isEmpty()) return 0.0;
      // TimelineVol "value" can be fractional (coverage share)
      return timeline.get(0).path("value").asDouble(0.0);
    } catch (Exception e) {
      return 0.0;
    }
  }

  public List<NewsItem> fetchTopArticles(LocalDate date, String query, int maxRecords) {
    Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).minusSeconds(1).toInstant();

    String url =
        "https://api.gdeltproject.org/api/v2/doc/doc"
            + "?query=" + urlenc(query)
            + "&mode=ArtList"
            + "&format=json"
            + "&maxrecords=" + maxRecords
            + "&startdatetime=" + GDELT_DT.format(start)
            + "&enddatetime=" + GDELT_DT.format(end)
            + "&sort=hybridrel";

    try {
      String body = http.get("gdelt", url).getBody();
      JsonNode root = om.readTree(body);
      JsonNode arts = root.path("articles");

      List<NewsItem> out = new ArrayList<>();
      if (arts.isArray()) {
        for (JsonNode a : arts) {
          String title = a.path("title").asText("");
          String source = a.path("sourceCountry").asText("");
          String urlLink = a.path("url").asText("");
          String seendate = a.path("seendate").asText("");

          Instant published = parseGdeltInstant(seendate);

          out.add(new NewsItem(title, source, urlLink, published));
        }
      }
      return out;
    } catch (Exception e) {
      return List.of();
    }
  }

  private Instant parseGdeltInstant(String raw) {
    if (raw == null || raw.isBlank()) return null;

    String s = raw.trim();

    // Case 1: ISO 8601 already (e.g., 2025-12-10T00:15:00Z)
    try {
      if (s.contains("T") && (s.endsWith("Z") || s.contains("+"))) {
        return Instant.parse(s);
      }
    } catch (Exception ignore) {}

    // Case 2: "yyyy-MM-dd HH:mm:ss" (often used by APIs)
    try {
      if (s.contains("-") && s.contains(" ")) {
        String iso = s.replace(" ", "T");
        if (!iso.endsWith("Z") && !iso.contains("+")) iso = iso + "Z";
        return Instant.parse(iso);
      }
    } catch (Exception ignore) {}

    // Case 3: compact "yyyyMMddHHmmss"
    try {
      if (s.matches("\\d{14}")) {
        return ZonedDateTime.parse(s, GDELT_SEENDATE_COMPACT).toInstant();
      }
    } catch (Exception ignore) {}

    return null;
  }

  private String urlenc(String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8);
  }

  public record NewsItem(String title, String source, String url, Instant publishedAt) {}
}
