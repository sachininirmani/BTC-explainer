package com.nirmani.btcexplainer.service.ingest;

import com.nirmani.btcexplainer.client.alternative.FearGreedClient;
import com.nirmani.btcexplainer.domain.signals.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SentimentIngestService {

  private final FearGreedClient client;
  private final FearGreedRepository repo;

  public SentimentIngestService(FearGreedClient client, FearGreedRepository repo) {
    this.client = client;
    this.repo = repo;
  }

  @Transactional
  public void ingestLatest() {
    FearGreedClient.FngPoint p = client.fetchLatest();
    if (p == null) return;

    FearGreedDaily d = repo.findBySentimentDate(p.date()).orElseGet(FearGreedDaily::new);
    d.setSentimentDate(p.date());
    d.setValue(p.value());
    d.setClassification(p.classification());
    repo.save(d);
  }
}
