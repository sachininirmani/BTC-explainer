package com.nirmani.btcexplainer.domain.signals;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "weather_extremes_daily", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"wx_date", "region_key", "extreme_type"})
})
public class WeatherExtremeDaily {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "wx_date", nullable = false)
  private LocalDate wxDate;

  @Column(name = "region_key", nullable = false)
  private String regionKey;

  @Column(name = "extreme_type", nullable = false)
  private String extremeType;

  private Double severity;

  @Column(name = "details_json", columnDefinition = "TEXT")
  private String detailsJson;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public LocalDate getWxDate() { return wxDate; }
  public void setWxDate(LocalDate wxDate) { this.wxDate = wxDate; }
  public String getRegionKey() { return regionKey; }
  public void setRegionKey(String regionKey) { this.regionKey = regionKey; }
  public String getExtremeType() { return extremeType; }
  public void setExtremeType(String extremeType) { this.extremeType = extremeType; }
  public Double getSeverity() { return severity; }
  public void setSeverity(Double severity) { this.severity = severity; }
  public String getDetailsJson() { return detailsJson; }
  public void setDetailsJson(String detailsJson) { this.detailsJson = detailsJson; }
}
