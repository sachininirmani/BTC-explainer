package com.nirmani.btcexplainer.domain.price;

import com.nirmani.btcexplainer.domain.asset.Asset;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "price_candles_daily", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"asset_id", "candle_date"})
})
public class DailyCandle {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "asset_id")
  private Asset asset;

  @Column(name = "candle_date", nullable = false)
  private LocalDate candleDate;

  @Column(nullable = false, precision = 18, scale = 8)
  private BigDecimal open;

  @Column(nullable = false, precision = 18, scale = 8)
  private BigDecimal high;

  @Column(nullable = false, precision = 18, scale = 8)
  private BigDecimal low;

  @Column(nullable = false, precision = 18, scale = 8)
  private BigDecimal close;

  @Column(precision = 24, scale = 8)
  private BigDecimal volume;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public Asset getAsset() { return asset; }
  public void setAsset(Asset asset) { this.asset = asset; }
  public LocalDate getCandleDate() { return candleDate; }
  public void setCandleDate(LocalDate candleDate) { this.candleDate = candleDate; }
  public BigDecimal getOpen() { return open; }
  public void setOpen(BigDecimal open) { this.open = open; }
  public BigDecimal getHigh() { return high; }
  public void setHigh(BigDecimal high) { this.high = high; }
  public BigDecimal getLow() { return low; }
  public void setLow(BigDecimal low) { this.low = low; }
  public BigDecimal getClose() { return close; }
  public void setClose(BigDecimal close) { this.close = close; }
  public BigDecimal getVolume() { return volume; }
  public void setVolume(BigDecimal volume) { this.volume = volume; }
  public Instant getCreatedAt() { return createdAt; }
}
