package com.nirmani.btcexplainer.api.controller;

import com.nirmani.btcexplainer.api.dto.ExplanationDto;
import com.nirmani.btcexplainer.domain.event.MarketEvent;
import com.nirmani.btcexplainer.domain.event.MarketEventRepository;
import com.nirmani.btcexplainer.domain.explanation.EventExplanation;
import com.nirmani.btcexplainer.service.explain.ExplanationService;
import java.math.RoundingMode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExplainController {

  private final ExplanationService service;
  private final MarketEventRepository eventRepo;

  public ExplainController(ExplanationService service, MarketEventRepository eventRepo) {
    this.service = service;
    this.eventRepo = eventRepo;
  }

  @GetMapping("/api/explain/{eventId}")
  public ExplanationDto explain(@PathVariable Long eventId) {
    MarketEvent e = eventRepo.findById(eventId).orElseThrow();
    EventExplanation ex = service.generateOrGet(eventId);

    String pct = e.getPctChange().setScale(2, RoundingMode.HALF_UP).toPlainString();

    return new ExplanationDto(
        eventId,
        e.getEventDate(),
        e.getDirection(),
        pct,
        ex.getConfidence(),
        ex.getSummaryText(),
        ex.getAiExplanationText(),
        ex.getAiExplanationSource(),
        ex.getAiModel(),
        ex.getFactorsJson()
    );
  }
}
