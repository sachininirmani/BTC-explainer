package com.nirmani.btcexplainer.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EventDto(
    Long id,
    LocalDate date,
    String direction,
    BigDecimal pctChange,
    int severity,
    String thresholdUsed) {}
