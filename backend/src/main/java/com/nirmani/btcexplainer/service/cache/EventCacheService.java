package com.nirmani.btcexplainer.service.cache;

import com.nirmani.btcexplainer.domain.asset.Asset;
import com.nirmani.btcexplainer.domain.asset.AssetRepository;
import com.nirmani.btcexplainer.domain.event.MarketEvent;
import com.nirmani.btcexplainer.domain.event.MarketEventRepository;
import com.nirmani.btcexplainer.domain.explanation.EventExplanation;
import com.nirmani.btcexplainer.domain.explanation.EventExplanationRepository;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class EventCacheService {

  private final AssetRepository assetRepo;
  private final MarketEventRepository eventRepo;
  private final EventExplanationRepository explRepo;

  public EventCacheService(AssetRepository assetRepo, MarketEventRepository eventRepo, EventExplanationRepository explRepo) {
    this.assetRepo = assetRepo;
    this.eventRepo = eventRepo;
    this.explRepo = explRepo;
  }

  @Cacheable("events")
  public List<MarketEvent> getLatestEvents(int limit) {
    Asset btc = assetRepo.findBySymbol("BTC").orElseThrow();
    return eventRepo.findLatestByAsset(btc.getId(), PageRequest.of(0, limit));
  }

  @Cacheable("explanations")
  public EventExplanation getExplanation(Long eventId) {
    return explRepo.findByEventId(eventId).orElse(null);
  }

  @CacheEvict(value = {"events", "chart", "explanations"}, allEntries = true)
  public void evictAll() {}
}
