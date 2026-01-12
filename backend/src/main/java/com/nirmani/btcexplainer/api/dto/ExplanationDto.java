package com.nirmani.btcexplainer.api.dto;

import java.time.LocalDate;

public record ExplanationDto(
    Long eventId,
    LocalDate eventDate,
    String direction,
    String pctChange,
    String confidence,
    String summary,
    String aiExplanation,
    String aiSource,
    String aiModel,
    String factorsJson
) {}
