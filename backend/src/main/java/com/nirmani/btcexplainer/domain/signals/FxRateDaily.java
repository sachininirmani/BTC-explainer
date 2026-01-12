package com.nirmani.btcexplainer.domain.signals;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "fx_rates_daily", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"rate_date", "base", "quote"})
})
public class FxRateDaily {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "rate_date", nullable = false)
  private LocalDate rateDate;

  /**
   * If the requested date is a weekend/holiday, data providers can return null.
   * In that case we may backfill using the nearest previous business day and store it here.
   * Nullable for older rows; backfilled via migration.
   */
  @Column(name = "source_date")
  private LocalDate sourceDate;

  @Column(nullable = false)
  private String base;

  @Column(nullable = false)
  private String quote;

  @Column(nullable = false, precision = 18, scale = 8)
  private BigDecimal rate;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }

  public LocalDate getRateDate() { return rateDate; }
  public void setRateDate(LocalDate rateDate) { this.rateDate = rateDate; }

  public LocalDate getSourceDate() { return sourceDate; }
  public void setSourceDate(LocalDate sourceDate) { this.sourceDate = sourceDate; }

  public String getBase() { return base; }
  public void setBase(String base) { this.base = base; }

  public String getQuote() { return quote; }
  public void setQuote(String quote) { this.quote = quote; }

  public BigDecimal getRate() { return rate; }
  public void setRate(BigDecimal rate) { this.rate = rate; }
}
