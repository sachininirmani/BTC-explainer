package com.nirmani.btcexplainer.service.jobs;

import com.nirmani.btcexplainer.service.cache.EventCacheService;
import com.nirmani.btcexplainer.service.detect.EventDetectionService;
import com.nirmani.btcexplainer.service.explain.ExplanationService;
import com.nirmani.btcexplainer.service.ingest.*;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DailyRefreshJob {

  private static final Logger log = LoggerFactory.getLogger(DailyRefreshJob.class);

  private final PriceIngestService priceIngest;
  private final EventDetectionService detector;
  private final NewsIngestService newsIngest;
  private final SentimentIngestService sentimentIngest;
  private final FxIngestService fxIngest;
  private final WeatherIngestService wxIngest;
  private final ExplanationService explainer;
  private final EventCacheService cache;

  @Value("${app.jobs.enabled:true}")
  private boolean enabled;

  @Value("${app.events.keep-last:100}")
  private int keepLast;

  public DailyRefreshJob(
      PriceIngestService priceIngest,
      EventDetectionService detector,
      NewsIngestService newsIngest,
      SentimentIngestService sentimentIngest,
      FxIngestService fxIngest,
      WeatherIngestService wxIngest,
      ExplanationService explainer,
      EventCacheService cache
  ) {
    this.priceIngest = priceIngest;
    this.detector = detector;
    this.newsIngest = newsIngest;
    this.sentimentIngest = sentimentIngest;
    this.fxIngest = fxIngest;
    this.wxIngest = wxIngest;
    this.explainer = explainer;
    this.cache = cache;
  }

  /** Runs on startup via controller call (see AdminController) and daily by cron. */
  @Scheduled(cron = "${app.jobs.daily-cron}")
  public void runDaily() {
    if (!enabled) return;
    log.info("Daily refresh started");

    int upserts = priceIngest.ingestBtcDailyOhlc();
    int created = detector.detectBtcBigMoves();

    // We ingest "yesterday" stats for signals to keep things stable
    LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
    newsIngest.ingestForDate(yesterday);
    sentimentIngest.ingestLatest();
    fxIngest.ingestEurUsd(yesterday);
    wxIngest.ingestExtremes(yesterday);

    // Regenerate explanations for newest events (best-effort)
    try {
      cache.getLatestEvents(keepLast).forEach(ev -> {
        try { explainer.generateOrGet(ev.getId()); } catch (Exception ignore) {}
      });
    } catch (Exception ignore) {}

    cache.evictAll();
    log.info("Daily refresh done. priceUpserts={}, eventsCreated={}", upserts, created);
  }
}
