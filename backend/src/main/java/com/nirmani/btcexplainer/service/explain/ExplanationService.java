package com.nirmani.btcexplainer.service.explain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nirmani.btcexplainer.client.gdelt.GdeltClient;
import com.nirmani.btcexplainer.domain.event.MarketEvent;
import com.nirmani.btcexplainer.domain.event.MarketEventRepository;
import com.nirmani.btcexplainer.domain.explanation.EventExplanation;
import com.nirmani.btcexplainer.domain.explanation.EventExplanationRepository;
import com.nirmani.btcexplainer.domain.signals.*;
import com.nirmani.btcexplainer.service.ingest.FxIngestService;
import com.nirmani.btcexplainer.service.ingest.NewsIngestService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Service
public class ExplanationService {

  private final MarketEventRepository eventRepo;
  private final EventExplanationRepository explRepo;

  private final NewsDailyStatRepository newsRepo;
  private final NewsItemSampleRepository newsItemsRepo;
  private final FearGreedRepository fngRepo;
  private final FxRateRepository fxRepo;
  private final WeatherExtremeRepository wxRepo;

  // On-demand enrichment (for historical dates not ingested by the daily job)
  private final NewsIngestService newsIngest;
  private final FxIngestService fxIngest;
  private final GdeltClient gdeltClient;

  private final AiNarrativeService aiNarrativeService;

  private final ObjectMapper om =
      JsonMapper.builder()
          .addModule(new JavaTimeModule())
          .build();


  @Value("${app.events.window-days-before:2}")
  private int daysBefore;

  @Value("${app.events.window-days-after:1}")
  private int daysAfter;

  public ExplanationService(
      MarketEventRepository eventRepo,
      EventExplanationRepository explRepo,
      NewsDailyStatRepository newsRepo,
      NewsItemSampleRepository newsItemsRepo,
      FearGreedRepository fngRepo,
      FxRateRepository fxRepo,
      WeatherExtremeRepository wxRepo,
      NewsIngestService newsIngest,
      FxIngestService fxIngest,
      GdeltClient gdeltClient
      ,AiNarrativeService aiNarrativeService
  ) {
    this.eventRepo = eventRepo;
    this.explRepo = explRepo;
    this.newsRepo = newsRepo;
    this.newsItemsRepo = newsItemsRepo;
    this.fngRepo = fngRepo;
    this.fxRepo = fxRepo;
    this.wxRepo = wxRepo;
    this.newsIngest = newsIngest;
    this.fxIngest = fxIngest;
    this.gdeltClient = gdeltClient;
    this.aiNarrativeService = aiNarrativeService;
  }

  @Transactional
  public EventExplanation generateOrGet(Long eventId) {
    EventExplanation existing = explRepo.findByEventId(eventId).orElse(null);
    if (existing == null) {
      return generate(eventId);
    }

    // Backfill AI narrative if the row predates the feature or generation failed.
    if (existing.getAiExplanationText() == null || "NONE".equalsIgnoreCase(existing.getAiExplanationSource())) {
      try {
        MarketEvent e = eventRepo.findById(eventId).orElseThrow();
        LocalDate d = e.getEventDate();
        LocalDate start = d.minusDays(daysBefore);
        LocalDate end = d.plusDays(daysAfter);

        List<Map<String, Object>> factors;
        try {
          factors = om.readValue(existing.getFactorsJson(), List.class);
        } catch (Exception ignore) {
          factors = List.of();
        }

        AiNarrativeService.AiNarrativeResult ai = aiNarrativeService.generate(e, factors, start, end);
        existing.setAiExplanationText(ai.text());
        existing.setAiExplanationSource(ai.source());
        existing.setAiModel(ai.model());
        existing.setAiGeneratedAt(ai.generatedAt());
        existing.setAiErrorMessage(ai.errorMessage());
        explRepo.save(existing);
      } catch (Exception ignore) {}
    }

    return existing;
  }

  private boolean isLikelyEnglish(String text) {
    if (text == null || text.isBlank()) return false;

    // Count non-ASCII characters
    long nonAscii = text.chars().filter(c -> c > 127).count();

    // Allow at most 10% non-ASCII characters
    return nonAscii <= (text.length() * 0.1);
  }


  @Transactional
  public EventExplanation generate(Long eventId) {
    MarketEvent e = eventRepo.findById(eventId).orElseThrow();
    LocalDate d = e.getEventDate();
    LocalDate start = d.minusDays(daysBefore);
    LocalDate end = d.plusDays(daysAfter);

    // Ensure signal rows exist for this event date (important for older events).
    ensureNewsForDate(d);
    ensureFxForDate(d);

    // Signals (after ensuring)
    NewsDailyStat newsStat = newsRepo.findByStatDate(d).orElse(null);
    int newsCount = (newsStat == null) ? 0 : newsStat.getArticleCount();
    String newsQuery = (newsStat == null) ? null : newsStat.getQueryTag();
    Double coveragePct = (newsStat == null) ? null : newsStat.getCoveragePct();

    List<NewsItemSample> headlines = newsItemsRepo.findTop20ByItemDateOrderByPublishedAtDesc(d);

    // Backfill coverage_pct if missing and we do have a query tag
    if (newsStat != null && newsQuery != null && (coveragePct == null)) {
      double cov = gdeltClient.fetchNewsCoveragePct(d, newsQuery);
      newsStat.setCoveragePct(cov);
      // persist best-effort
      try { newsRepo.save(newsStat); } catch (Exception ignore) {}
      coveragePct = cov;
    }

    FearGreedDaily fng = fngRepo.findBySentimentDate(d).orElse(null);

    FxRateDaily fx = fxRepo.findByRateDateAndBaseAndQuote(d, "EUR", "USD").orElse(null);
    FxRateDaily fxPrev = fxRepo.findByRateDateAndBaseAndQuote(d.minusDays(1), "EUR", "USD").orElse(null);

    List<WeatherExtremeDaily> wx = wxRepo.findByWxDate(d);

    // Factor scoring (0-100), intentionally simple.
    List<Map<String, Object>> factors = new ArrayList<>();

    // --- NEWS ---
    if (newsCount > 0 || (headlines != null && !headlines.isEmpty())) {
      List<String> sample = (headlines == null) ? List.of() :
        headlines.stream()
            // 1️⃣ English-only headline filter (MOST IMPORTANT)
            .filter(h -> isLikelyEnglish(h.getTitle()))

            // 2️⃣ URL blocklist (keep your existing logic)
            .filter(h -> {
              String url = h.getUrl();
              if (url == null) return true;

              url = url.toLowerCase();

              return !(
                  url.contains(".jp/")
                      || url.contains(".jp?")
                      || url.contains(".kr/")
                      || url.contains(".kr?")
                      || url.contains(".cn/")
                      || url.contains(".cn?")
                      || url.contains(".ru/")
                      || url.contains(".ru?")
                      || url.contains(".ir/")
                      || url.contains(".tr/")
                      || url.contains(".br/")
                      || url.contains("segye.com")
                      || url.contains("asahi.com")
                      || url.contains("yomiuri.co.jp")
                      || url.contains("nikkei.com")
                      || url.contains("chosun.com")
                      || url.contains("baidu.com")
                      || url.contains("naver.com")
              );
            })

            // 3️⃣ De-duplicate & limit AFTER filtering
            .map(NewsItemSample::getTitle)
            .distinct()
            .limit(20)
            .toList();



      Map<String, Object> evidence = new LinkedHashMap<>();
      evidence.put("articlesCount", newsCount);
      if (coveragePct != null) evidence.put("coveragePct", coveragePct);
      if (newsQuery != null) evidence.put("queryTag", newsQuery);
      evidence.put("sampleHeadlines", sample);

      // If the count is low (or derived), be transparent
      if (newsCount == 0 && !sample.isEmpty()) {
        evidence.put("note", "No raw count was available, but related headlines were found; showing titles as context.");
      }

      factors.add(factor("News activity", scoreClamp(newsCount, 200), evidence));
    } else {
      factors.add(factor("News activity", 10, Map.of(
          "articlesCount", 0,
          "explanation",
          "No matching crypto headlines were found for this exact date using the current query set. " +
              "BTC can still move due to technical trading (liquidations, stops), broader risk sentiment, or macro events.",
          "confidence", "low"
      )));
    }

    // --- SENTIMENT ---
    if (fng != null) {
      factors.add(factor("Market sentiment (Fear & Greed)", 40, Map.of(
          "value", fng.getValue(),
          "classification", fng.getClassification()
      )));
    } else {
      factors.add(factor("Market sentiment (Fear & Greed)", 0, Map.of("status", "unavailable")));
    }

    // --- FX ---
    if (fx != null) {
      BigDecimal eurusd = fx.getRate();

      BigDecimal changePct = null;
      if (fxPrev != null && fxPrev.getRate() != null && fxPrev.getRate().compareTo(BigDecimal.ZERO) != 0) {
        changePct = eurusd.subtract(fxPrev.getRate())
            .divide(fxPrev.getRate(), 8, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
      }

      Map<String, Object> evidence = new LinkedHashMap<>();
      evidence.put("eurUsdRate", eurusd);
      if (fx.getSourceDate() != null) evidence.put("sourceDate", fx.getSourceDate());
      if (changePct != null) evidence.put("dayChangePct", changePct.setScale(4, RoundingMode.HALF_UP));

      int fxScore = 20;
      if (changePct != null) {
        // Score based on magnitude; >= 1% daily move is notable for FX
        fxScore = scoreClamp(changePct.abs().multiply(new BigDecimal("100")).intValue(), 200); // (abs% * 100) capped
      }

      factors.add(factor("FX context (EUR/USD)", fxScore, evidence));
    } else {
      factors.add(factor("FX context (EUR/USD)", 5, Map.of(
          "status", "unavailable",
          "explanation", "FX rate data wasn't available for this date from the provider (possibly a holiday/weekend) and couldn't be backfilled.",
          "confidence", "low"
      )));
    }

    // --- WEATHER (DEMO) ---
    if (wx != null && !wx.isEmpty()) {
      factors.add(factor("Extreme weather signals (demo)", 30, Map.of(
          "signals", wx.stream().map(x -> Map.of(
              "region", x.getRegionKey(),
              "type", x.getExtremeType(),
              "severity", x.getSeverity()
          )).toList()
      )));
    } else {
      factors.add(factor("Extreme weather signals (demo)", 0, Map.of("signals", List.of())));
    }

    factors.sort((a, b) -> Integer.compare((int) b.get("score"), (int) a.get("score")));

    int totalScore = factors.stream().mapToInt(m -> (int) m.get("score")).sum();
    String confidence = totalScore >= 130 ? "MED" : "LOW"; // intentionally conservative

    String summary = renderSummary(e, factors, start, end);

    AiNarrativeService.AiNarrativeResult ai = aiNarrativeService.generate(e, factors, start, end);

    EventExplanation ex = new EventExplanation();
    ex.setEvent(e);
    ex.setConfidence(confidence);
    ex.setSummaryText(summary);
    ex.setAiExplanationText(ai.text());
    ex.setAiExplanationSource(ai.source());
    ex.setAiModel(ai.model());
    ex.setAiGeneratedAt(ai.generatedAt());
    ex.setAiErrorMessage(ai.errorMessage());
    try {
      ex.setFactorsJson(om.writeValueAsString(factors));
    } catch (Exception err) {
      throw new RuntimeException("Failed to serialize factors JSON", err);
    }


    explRepo.save(ex);
    return ex;
  }

  private void ensureNewsForDate(LocalDate date) {
    try {
      boolean missingStat = newsRepo.findByStatDate(date).isEmpty();
      boolean missingHeadlines = newsItemsRepo.findTop20ByItemDateOrderByPublishedAtDesc(date).isEmpty();
      if (missingStat || missingHeadlines) {
        newsIngest.ingestForDate(date);
      }
    } catch (Exception ignore) {}
  }

  private void ensureFxForDate(LocalDate date) {
    try {
      boolean missingFx = fxRepo.findByRateDateAndBaseAndQuote(date, "EUR", "USD").isEmpty();
      if (missingFx) {
        fxIngest.ingestEurUsd(date);
      }
    } catch (Exception ignore) {}
  }

  private Map<String, Object> factor(String name, int score, Map<String, Object> evidence) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("name", name);
    m.put("score", score);
    m.put("evidence", evidence);
    return m;
  }

  private int scoreClamp(int value, int cap) {
    int v = Math.min(Math.max(value, 0), cap);
    return (int) Math.round((v / (double) cap) * 100.0);
  }

  private String renderSummary(MarketEvent e, List<Map<String, Object>> factors, LocalDate start, LocalDate end) {
    BigDecimal pct = e.getPctChange().setScale(2, RoundingMode.HALF_UP);
    String dirWord = e.getDirection().equals("UP") ? "rose" : "fell";
    String top = factors.isEmpty() ? "No signals were available." : (String) factors.get(0).get("name");

    return "BTC " + dirWord + " " + pct.abs() + "% on " + e.getEventDate()
        + ". This explanation looks for signals within " + start + " to " + end
        + ". Top observed factor: " + top
        + ". Note: this is an educational correlation-based explanation, not financial advice.";
  }
}
