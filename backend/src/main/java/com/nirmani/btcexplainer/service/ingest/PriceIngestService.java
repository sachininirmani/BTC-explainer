package com.nirmani.btcexplainer.service.ingest;

import com.nirmani.btcexplainer.client.coingecko.CoinGeckoClient;
import com.nirmani.btcexplainer.domain.asset.Asset;
import com.nirmani.btcexplainer.domain.asset.AssetRepository;
import com.nirmani.btcexplainer.domain.price.DailyCandle;
import com.nirmani.btcexplainer.domain.price.DailyCandleRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PriceIngestService {

  private final CoinGeckoClient coinGecko;
  private final AssetRepository assetRepo;
  private final DailyCandleRepository candleRepo;

  @Value("${app.ingest.days:180}")
  private int days;

  /**
   * How many most-recent days are treated as "mutable" and allowed to be updated
   * if the upstream data changes.
   */
  @Value("${app.ingest.mutable-days:2}")
  private int mutableDays;

  public PriceIngestService(CoinGeckoClient coinGecko, AssetRepository assetRepo, DailyCandleRepository candleRepo) {
    this.coinGecko = coinGecko;
    this.assetRepo = assetRepo;
    this.candleRepo = candleRepo;
  }

  @Transactional
  public int ingestBtcDailyOhlc() {
    Asset btc = assetRepo.findBySymbol("BTC").orElseThrow();
    List<CoinGeckoClient.OhlcRow> rows = coinGecko.fetchBtcOhlc(days);
    int inserted = 0;
    int updated = 0;

    LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
    LocalDate startDate = todayUtc.minusDays(days);

    // Preload existing candles in the same time window so we never hit a unique
    // constraint violation (asset_id, candle_date).
    Map<LocalDate, DailyCandle> existingByDate = new HashMap<>();
    for (DailyCandle existing : candleRepo.findByAssetFrom(btc.getId(), startDate)) {
      existingByDate.put(existing.getCandleDate(), existing);
    }

    for (CoinGeckoClient.OhlcRow r : rows) {
      LocalDate date = r.timestamp().atZone(ZoneOffset.UTC).toLocalDate();

      DailyCandle existing = existingByDate.get(date);
      if (existing == null) {
        DailyCandle c = new DailyCandle();
        c.setAsset(btc);
        c.setCandleDate(date);
        c.setOpen(r.open());
        c.setHigh(r.high());
        c.setLow(r.low());
        c.setClose(r.close());
        // volume not provided by OHLC endpoint; keep null
        candleRepo.save(c);
        inserted++;
        existingByDate.put(date, c);
        continue;
      }

      // Existing row: only update if it's within the mutable window.
      if (!date.isBefore(todayUtc.minusDays(mutableDays))) {
        existing.setOpen(r.open());
        existing.setHigh(r.high());
        existing.setLow(r.low());
        existing.setClose(r.close());
        candleRepo.save(existing);
        updated++;
      }
    }

    return inserted + updated;
  }
}
