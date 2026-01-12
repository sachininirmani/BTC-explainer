package com.nirmani.btcexplainer.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ChartPointDto(LocalDate date, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close) {}
