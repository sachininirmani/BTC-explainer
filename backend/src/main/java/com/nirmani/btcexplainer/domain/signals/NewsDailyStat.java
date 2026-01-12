package com.nirmani.btcexplainer.domain.signals;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "news_daily_stats")
public class NewsDailyStat {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "stat_date", nullable = false, unique = true)
  private LocalDate statDate;

  @Column(name = "query_tag", nullable = false)
  private String queryTag;

  /**
   * Raw article count (or an "effective count" when using fallback headlines).
   * This is used for scoring/visibility in the UI.
   */
  @Column(name = "article_count", nullable = false)
  private int articleCount;

  /**
   * GDELT TimelineVol coverage share (percentage of all global news), if captured.
   * Nullable because older rows may not have this value.
   */
  @Column(name = "coverage_pct")
  private Double coveragePct;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }

  public LocalDate getStatDate() { return statDate; }
  public void setStatDate(LocalDate statDate) { this.statDate = statDate; }

  public String getQueryTag() { return queryTag; }
  public void setQueryTag(String queryTag) { this.queryTag = queryTag; }

  public int getArticleCount() { return articleCount; }
  public void setArticleCount(int articleCount) { this.articleCount = articleCount; }

  public Double getCoveragePct() { return coveragePct; }
  public void setCoveragePct(Double coveragePct) { this.coveragePct = coveragePct; }
}
