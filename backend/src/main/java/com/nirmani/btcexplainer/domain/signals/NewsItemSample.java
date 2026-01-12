package com.nirmani.btcexplainer.domain.signals;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "news_items_sample")
public class NewsItemSample {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "item_date", nullable = false)
  private LocalDate itemDate;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String title;

  private String source;
  private String url;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public LocalDate getItemDate() { return itemDate; }
  public void setItemDate(LocalDate itemDate) { this.itemDate = itemDate; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getSource() { return source; }
  public void setSource(String source) { this.source = source; }
  public String getUrl() { return url; }
  public void setUrl(String url) { this.url = url; }
  public Instant getPublishedAt() { return publishedAt; }
  public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
}
