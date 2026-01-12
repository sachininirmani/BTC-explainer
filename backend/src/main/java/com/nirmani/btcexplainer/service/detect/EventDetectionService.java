package com.nirmani.btcexplainer.service.detect;

import com.nirmani.btcexplainer.domain.asset.Asset;
import com.nirmani.btcexplainer.domain.asset.AssetRepository;
import com.nirmani.btcexplainer.domain.event.MarketEvent;
import com.nirmani.btcexplainer.domain.event.MarketEventRepository;
import com.nirmani.btcexplainer.domain.price.DailyCandle;
import com.nirmani.btcexplainer.domain.price.DailyCandleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventDetectionService {

  private final AssetRepository assetRepo;
  private final DailyCandleRepository candleRepo;
  private final MarketEventRepository eventRepo;

  @Value("${app.events.big-move-pct:2.0}")
  private double bigMovePct;

  /** Optional comma-separated list of thresholds, e.g. "4,3,2". */
  @Value("${app.events.thresholds:}")
  private String thresholdsCsv;

  /** How far back we recompute events */
  @Value("${app.events.lookback-days:180}")
  private int lookbackDays;

  /** Recent days that are allowed to be recalculated (mutable window) */
  @Value("${app.events.mutable-days:2}")
  private int mutableDays;

  public EventDetectionService(
      AssetRepository assetRepo,
      DailyCandleRepository candleRepo,
      MarketEventRepository eventRepo) {
    this.assetRepo = assetRepo;
    this.candleRepo = candleRepo;
    this.eventRepo = eventRepo;
  }

  @Transactional
  public int detectBtcBigMoves() {
    Asset btc = assetRepo.findBySymbol("BTC").orElseThrow();
    LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
    LocalDate startDate = todayUtc.minusDays(Math.max(1, lookbackDays));

    List<DailyCandle> candles = candleRepo.findByAssetFrom(btc.getId(), startDate);

    List<Double> thresholds = parseThresholds();
    if (thresholds.isEmpty()) return 0;
    double minThreshold = thresholds.get(thresholds.size() - 1);

    Map<LocalDate, MarketEvent> existingByDate =
        eventRepo.findByAssetFrom(btc.getId(), startDate).stream()
            .collect(Collectors.toMap(MarketEvent::getEventDate, e -> e));

    int created = 0;
    int updated = 0;

    for (DailyCandle c : candles) {
      BigDecimal open = c.getOpen();
      BigDecimal close = c.getClose();
      if (open.compareTo(BigDecimal.ZERO) <= 0) continue;

      BigDecimal pct =
          close.subtract(open)
              .divide(open, 10, RoundingMode.HALF_UP)
              .multiply(new BigDecimal("100"))
              .setScale(4, RoundingMode.HALF_UP);

      double absPct = pct.abs().doubleValue();
      if (absPct < minThreshold) continue;

      Match match = matchThreshold(absPct, thresholds);
      if (match == null) continue;

      LocalDate date = c.getCandleDate();
      MarketEvent existing = existingByDate.get(date);

      boolean isMutable =
          !date.isBefore(todayUtc.minusDays(Math.max(0, mutableDays)));

      if (existing != null && !isMutable) {
        continue;
      }

      MarketEvent e = existing != null ? existing : new MarketEvent();
      e.setAsset(btc);
      e.setEventDate(date);
      e.setPctChange(pct);
      e.setDirection(pct.signum() >= 0 ? "UP" : "DOWN");
      e.setSeverity((short) match.severity());
      e.setThresholdUsed("abs(daily_return_pct) >= " + match.threshold());

      eventRepo.save(e);

      if (existing == null) {
        existingByDate.put(date, e);
        created++;
      } else {
        updated++;
      }
    }
    return created;
  }

  private List<Double> parseThresholds() {
    String raw =
        (thresholdsCsv == null || thresholdsCsv.isBlank())
            ? "4,3,2"
            : thresholdsCsv;

    Set<Double> unique =
        Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(Double::parseDouble)
            .collect(Collectors.toCollection(LinkedHashSet::new));

    return unique.stream()
        .sorted((a, b) -> Double.compare(b, a))
        .collect(Collectors.toList());
  }

  private Match matchThreshold(double absPct, List<Double> thresholdsDesc) {
    for (double t : thresholdsDesc) {
      if (absPct >= t) {
        return new Match(t, (int) Math.floor(t));
      }
    }
    return null;
  }

  private record Match(double threshold, int severity) {}
}
