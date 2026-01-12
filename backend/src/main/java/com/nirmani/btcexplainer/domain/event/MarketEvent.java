package com.nirmani.btcexplainer.domain.event;

import com.nirmani.btcexplainer.domain.asset.Asset;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "market_events", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"asset_id", "event_date"})
})
public class MarketEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "asset_id")
  private Asset asset;

  @Column(name = "event_date", nullable = false)
  private LocalDate eventDate;

  @Column(nullable = false)
  private String direction; // UP / DOWN

  @Column(name = "pct_change", nullable = false, precision = 10, scale = 4)
  private BigDecimal pctChange;

  @Column(name = "threshold_used", nullable = false)
  private String thresholdUsed;

  /**
   * Severity level for the event.
   *
   * <p>Convention used by this project:
   * <ul>
   *   <li>4 = very strong move (|pctChange| &gt;= 4)</li>
   *   <li>3 = strong move (|pctChange| &gt;= 3 and &lt; 4)</li>
   *   <li>2 = moderate move (|pctChange| &gt;= 2 and &lt; 3)</li>
   * </ul>
   */
  @Column(nullable = false)
  private Short severity;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public Asset getAsset() { return asset; }
  public void setAsset(Asset asset) { this.asset = asset; }
  public LocalDate getEventDate() { return eventDate; }
  public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
  public String getDirection() { return direction; }
  public void setDirection(String direction) { this.direction = direction; }
  public BigDecimal getPctChange() { return pctChange; }
  public void setPctChange(BigDecimal pctChange) { this.pctChange = pctChange; }
  public String getThresholdUsed() { return thresholdUsed; }
  public void setThresholdUsed(String thresholdUsed) { this.thresholdUsed = thresholdUsed; }
  public Short getSeverity() { return severity; }
  public void setSeverity(Short severity) { this.severity = severity; }
  public Instant getCreatedAt() { return createdAt; }
}
