package com.nirmani.btcexplainer.service.ingest;

import com.nirmani.btcexplainer.client.frankfurter.FxClient;
import com.nirmani.btcexplainer.domain.signals.FxRateDaily;
import com.nirmani.btcexplainer.domain.signals.FxRateRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FxIngestService {

  private final FxClient client;
  private final FxRateRepository repo;

  public FxIngestService(FxClient client, FxRateRepository repo) {
    this.client = client;
    this.repo = repo;
  }

  /**
   * Ingest EUR/USD for the given date. If the provider has no rate for that exact date
   * (weekends / holidays), we backfill using the nearest previous day within 7 days.
   *
   * <p>We store the fetched rate for the requested date, and also keep the actual source date
   * in {@code source_date} for transparency.</p>
   */
  @Transactional
  public void ingestEurUsd(LocalDate date) {
    LocalDate sourceDate = date;
    BigDecimal rate = client.fetchEurUsd(date);

    if (rate == null) {
      for (int i = 1; i <= 7; i++) {
        LocalDate d = date.minusDays(i);
        BigDecimal r = client.fetchEurUsd(d);
        if (r != null) {
          rate = r;
          sourceDate = d;
          break;
        }
      }
    }

    if (rate == null) return;

    FxRateDaily row =
        repo.findByRateDateAndBaseAndQuote(date, "EUR", "USD").orElseGet(FxRateDaily::new);

    row.setRateDate(date);
    row.setSourceDate(sourceDate);
    row.setBase("EUR");
    row.setQuote("USD");
    row.setRate(rate);

    repo.save(row);
  }
}
