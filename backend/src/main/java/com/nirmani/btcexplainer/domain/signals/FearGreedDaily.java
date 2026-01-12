package com.nirmani.btcexplainer.domain.signals;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "sentiment_fng_daily")
public class FearGreedDaily {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "sentiment_date", nullable = false, unique = true)
  private LocalDate sentimentDate;

  @Column(nullable = false)
  private int value;

  private String classification;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public LocalDate getSentimentDate() { return sentimentDate; }
  public void setSentimentDate(LocalDate sentimentDate) { this.sentimentDate = sentimentDate; }
  public int getValue() { return value; }
  public void setValue(int value) { this.value = value; }
  public String getClassification() { return classification; }
  public void setClassification(String classification) { this.classification = classification; }
}
