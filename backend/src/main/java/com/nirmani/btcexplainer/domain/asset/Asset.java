package com.nirmani.btcexplainer.domain.asset;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "assets")
public class Asset {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String symbol;

  @Column(nullable = false)
  private String name;

  @Column(name = "asset_type", nullable = false)
  private String assetType;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public String getSymbol() { return symbol; }
  public void setSymbol(String symbol) { this.symbol = symbol; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getAssetType() { return assetType; }
  public void setAssetType(String assetType) { this.assetType = assetType; }
  public Instant getCreatedAt() { return createdAt; }
}
