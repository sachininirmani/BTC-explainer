package com.nirmani.btcexplainer.client.coingecko;

import com.nirmani.btcexplainer.util.HttpJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CoinGeckoClient {

  private final HttpJson http;
  private final ObjectMapper om = new ObjectMapper();

  public CoinGeckoClient(HttpJson http) {
    this.http = http;
  }

  /**
   * Fetch OHLC candles for bitcoin.
   * Endpoint: /coins/bitcoin/ohlc?vs_currency=usd&days=180
   * Response rows: [timestamp_ms, open, high, low, close]
   */
  public List<OhlcRow> fetchBtcOhlc(int days) {
    String url = "https://api.coingecko.com/api/v3/coins/bitcoin/ohlc?vs_currency=usd&days=" + days;
    try {
      String body = http.get("coingecko", url).getBody();
      JsonNode arr = om.readTree(body);
      List<OhlcRow> out = new ArrayList<>();
      for (JsonNode row : arr) {
        long ts = row.get(0).asLong();
        out.add(new OhlcRow(
            Instant.ofEpochMilli(ts),
            new BigDecimal(row.get(1).asText()),
            new BigDecimal(row.get(2).asText()),
            new BigDecimal(row.get(3).asText()),
            new BigDecimal(row.get(4).asText())
        ));
      }
      return out;
    } catch (Exception e) {
      return List.of();
    }
  }

  public record OhlcRow(Instant timestamp, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close) {}
}
