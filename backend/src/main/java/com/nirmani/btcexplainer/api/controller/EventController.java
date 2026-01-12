package com.nirmani.btcexplainer.api.controller;

import com.nirmani.btcexplainer.api.dto.EventDto;
import com.nirmani.btcexplainer.service.cache.EventCacheService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EventController {

  private final EventCacheService cache;

  public EventController(EventCacheService cache) {
    this.cache = cache;
  }

  @GetMapping("/api/events")
  public List<EventDto> events(@RequestParam(defaultValue = "100") int limit) {
    return cache.getLatestEvents(Math.min(limit, 200)).stream()
        .map(
            e ->
                new EventDto(
                    e.getId(),
                    e.getEventDate(),
                    e.getDirection(),   
                    e.getPctChange(),
                    e.getSeverity().intValue(),
                    e.getThresholdUsed()))
        .toList();
  }
}
