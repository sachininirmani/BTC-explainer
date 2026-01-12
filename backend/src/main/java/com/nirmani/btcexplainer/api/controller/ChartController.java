package com.nirmani.btcexplainer.api.controller;

import com.nirmani.btcexplainer.api.dto.ChartPointDto;
import com.nirmani.btcexplainer.domain.asset.Asset;
import com.nirmani.btcexplainer.domain.asset.AssetRepository;
import com.nirmani.btcexplainer.domain.price.DailyCandle;
import com.nirmani.btcexplainer.domain.price.DailyCandleRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChartController {

  private final AssetRepository assetRepo;
  private final DailyCandleRepository candleRepo;

  public ChartController(AssetRepository assetRepo, DailyCandleRepository candleRepo) {
    this.assetRepo = assetRepo;
    this.candleRepo = candleRepo;
  }

  @GetMapping("/api/chart")
  @Cacheable("chart")
  public List<ChartPointDto> chart(@RequestParam(defaultValue = "180") int days) {
    Asset btc = assetRepo.findBySymbol("BTC").orElseThrow();
    LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(days);
    List<DailyCandle> candles = candleRepo.findByAssetFrom(btc.getId(), start);
    return candles.stream()
        .map(c -> new ChartPointDto(c.getCandleDate(), c.getOpen(), c.getHigh(), c.getLow(), c.getClose()))
        .toList();
  }
}
