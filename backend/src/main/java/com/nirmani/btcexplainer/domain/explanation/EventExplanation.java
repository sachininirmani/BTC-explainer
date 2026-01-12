package com.nirmani.btcexplainer.domain.explanation;

import com.nirmani.btcexplainer.domain.event.MarketEvent;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "event_explanations")
public class EventExplanation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id", unique = true)
  private MarketEvent event;

  @Column(name = "summary_text", nullable = false, columnDefinition = "TEXT")
  private String summaryText;

  @Column(nullable = false)
  private String confidence;

  @Column(name = "factors_json", nullable = false, columnDefinition = "TEXT")
  private String factorsJson;

  @Column(name = "generated_at", nullable = false)
  private Instant generatedAt = Instant.now();

  @Column(name = "ai_explanation_text", columnDefinition = "TEXT")
  private String aiExplanationText;

  @Column(name = "ai_explanation_source", nullable = false)
  private String aiExplanationSource = "NONE"; // OPENAI | FALLBACK | NONE

  @Column(name = "ai_model")
  private String aiModel;

  @Column(name = "ai_generated_at")
  private Instant aiGeneratedAt;

  @Column(name = "ai_error_message", columnDefinition = "TEXT")
  private String aiErrorMessage;

  public Long getId() { return id; }
  public MarketEvent getEvent() { return event; }
  public void setEvent(MarketEvent event) { this.event = event; }
  public String getSummaryText() { return summaryText; }
  public void setSummaryText(String summaryText) { this.summaryText = summaryText; }
  public String getConfidence() { return confidence; }
  public void setConfidence(String confidence) { this.confidence = confidence; }
  public String getFactorsJson() { return factorsJson; }
  public void setFactorsJson(String factorsJson) { this.factorsJson = factorsJson; }
  public Instant getGeneratedAt() { return generatedAt; }

  public String getAiExplanationText() { return aiExplanationText; }
  public void setAiExplanationText(String aiExplanationText) { this.aiExplanationText = aiExplanationText; }

  public String getAiExplanationSource() { return aiExplanationSource; }
  public void setAiExplanationSource(String aiExplanationSource) { this.aiExplanationSource = aiExplanationSource; }

  public String getAiModel() { return aiModel; }
  public void setAiModel(String aiModel) { this.aiModel = aiModel; }

  public Instant getAiGeneratedAt() { return aiGeneratedAt; }
  public void setAiGeneratedAt(Instant aiGeneratedAt) { this.aiGeneratedAt = aiGeneratedAt; }

  public String getAiErrorMessage() { return aiErrorMessage; }
  public void setAiErrorMessage(String aiErrorMessage) { this.aiErrorMessage = aiErrorMessage; }
}
