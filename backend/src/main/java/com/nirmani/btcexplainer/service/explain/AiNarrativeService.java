package com.nirmani.btcexplainer.service.explain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nirmani.btcexplainer.client.openai.OpenAiClient;
import com.nirmani.btcexplainer.domain.event.MarketEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class AiNarrativeService {

  public record AiNarrativeResult(
      String text,
      String source,
      String model,
      Instant generatedAt,
      String errorMessage
  ) {}

  private final OpenAiClient openAi;

  private final ObjectMapper om =
      JsonMapper.builder()
          .addModule(new JavaTimeModule())
          .build();

  public AiNarrativeService(OpenAiClient openAi) {
    this.openAi = openAi;
  }

  public AiNarrativeResult generate(MarketEvent e, List<Map<String, Object>> factors, LocalDate start, LocalDate end) {
    // Always attempt OpenAI first; fall back to deterministic text.
    String fallback = fallbackNarrative(e, factors);

    try {
      String prompt = buildPrompt(e, factors, start, end, fallback);
      OpenAiClient.ChatResult r = openAi.chat(prompt, 0.35);
      String cleaned = clean(r.content());
      if (cleaned == null || cleaned.isBlank()) {
        return new AiNarrativeResult(fallback, "FALLBACK", null, Instant.now(), "OpenAI returned empty text");
      }
      return new AiNarrativeResult(cleaned, "OPENAI", r.modelUsed(), Instant.now(), null);
    } catch (Exception ex) {
      // Do not fail the request; store fallback.
      String msg = ex.getMessage();
      if (msg != null && msg.length() > 600) msg = msg.substring(0, 600);
      return new AiNarrativeResult(fallback, "FALLBACK", null, Instant.now(), msg);
    }
  }

  private String buildPrompt(
    MarketEvent e,
    List<Map<String, Object>> factors,
    LocalDate start,
    LocalDate end,
    String fallback
  ) {
    BigDecimal pct = e.getPctChange().setScale(2, RoundingMode.HALF_UP);
    String dirWord = "UP".equalsIgnoreCase(e.getDirection()) ? "rose" : "fell";

    String factorsJson;
    try {
      factorsJson = om.writerWithDefaultPrettyPrinter().writeValueAsString(factors);
    } catch (Exception ignore) {
      factorsJson = String.valueOf(factors);
    }

    return """
  You are a market-explanation assistant writing for everyday users.

  TASK:
  Write ONE clear, readable paragraph (6–8 sentences) explaining why Bitcoin moved on the given date.

  LANGUAGE & TONE:
  - Use simple, plain English
  - Neutral and explanatory (not persuasive)
  - No financial advice
  - No predictions about the future

  STRUCTURE (IMPORTANT):
  - Sentence 1: State what happened to the price.
  - Sentences 2–4: Explain the MAIN NEWS THEMES found in the headlines.
    • If headlines mention institutional activity (e.g., BlackRock, Strategy, large holders),
      clearly explain what those actions were and why markets might react.
    • Do not list headlines, but explain their meaning in plain language.
  - Sentence 5: Briefly mention other signals (sentiment, FX, etc.) if relevant.
  - Sentence 6–7: Explain uncertainty or weak signals if confidence is LOW.
  - Final sentence: State that this is an educational, correlation-based explanation, not advice.

  CONTEXT:
  - BTC %s %s%% on %s.
  - Analysis window: %s to %s.

  SIGNALS & NEWS DATA (use ONLY this information):
  %s

  If news coverage is limited or mixed, explain that clearly rather than filling with generic text.

  Do NOT output bullet points.
  Do NOT output JSON.
  Do NOT mention being an AI.

  Example fallback style (do NOT copy text):
  %s
  """.formatted(
        dirWord,
        pct.abs().toPlainString(),
        e.getEventDate(),
        start,
        end,
        factorsJson,
        fallback
    );
  }

  private String clean(String text) {
    if (text == null) return null;
    // Remove excessive whitespace; keep as a single paragraph.
    String t = text.replaceAll("\\s+", " ").trim();
    // Avoid very long generations.
    if (t.length() > 900) t = t.substring(0, 900).trim();
    return t;
  }

  private String fallbackNarrative(MarketEvent e, List<Map<String, Object>> factors) {
    BigDecimal pct = e.getPctChange().setScale(2, RoundingMode.HALF_UP);
    String dirWord = "UP".equalsIgnoreCase(e.getDirection()) ? "rose" : "fell";

    Map<String, Object> news = findFactor("News activity", factors);
    Map<String, Object> fng = findFactor("Market sentiment (Fear & Greed)", factors);
    Map<String, Object> fx = findFactor("FX context (EUR/USD)", factors);

    StringBuilder sb = new StringBuilder();
    sb.append("Bitcoin ").append(dirWord).append(" ")
        .append(pct.abs().toPlainString()).append("% on ").append(e.getEventDate()).append(". ");

    // Sentiment
    if (fng != null) {
      Map<String, Object> ev = safeEvidence(fng);
      Object cls = ev.get("classification");
      Object val = ev.get("value");
      if (cls != null && val != null) {
        sb.append("The Fear & Greed index was in the ‘").append(cls).append("’ range (value ").append(val).append("), suggesting a cautious backdrop. ");
      }
    }

    // News themes
    List<String> headlines = extractHeadlines(news);
    if (!headlines.isEmpty()) {
      String themes = inferThemes(headlines);
      sb.append("News coverage around the date mainly focused on ").append(themes).append(". ");
    } else {
      sb.append("There were few strong news signals tied to this exact date, so the move could be more about short-term positioning and technical trading. ");
    }

    // FX
    if (fx != null) {
      Map<String, Object> ev = safeEvidence(fx);
      Object dayChange = ev.get("dayChangePct");
      if (dayChange != null) {
        sb.append("EUR/USD also moved ").append(dayChange).append("% on the day, which can slightly influence broader risk sentiment. ");
      }
    }

    // Confidence
    sb.append("Overall confidence is ").append(bestConfidenceFromFactors(factors)).append(", so treat this as an educational, correlation-based summary rather than advice.");

    return sb.toString().replaceAll("\\s+", " ").trim();
  }

  private Map<String, Object> findFactor(String name, List<Map<String, Object>> factors) {
    if (factors == null) return null;
    for (Map<String, Object> f : factors) {
      if (f == null) continue;
      Object n = f.get("name");
      if (name.equals(String.valueOf(n))) return f;
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> safeEvidence(Map<String, Object> factor) {
    Object ev = factor.get("evidence");
    if (ev instanceof Map<?, ?> m) {
      return (Map<String, Object>) m;
    }
    return Collections.emptyMap();
  }

  @SuppressWarnings("unchecked")
  private List<String> extractHeadlines(Map<String, Object> newsFactor) {
    if (newsFactor == null) return List.of();
    Map<String, Object> ev = safeEvidence(newsFactor);
    Object sh = ev.get("sampleHeadlines");
    if (sh instanceof List<?> list) {
      return list.stream().map(String::valueOf).filter(s -> !s.isBlank()).limit(8).toList();
    }
    return List.of();
  }

  private String inferThemes(List<String> headlines) {
    // Very small, deterministic theme extractor.
    boolean hasEtf = containsAny(headlines, "etf", "etfs");
    boolean hasLiquidations = containsAny(headlines, "liquidation", "liquidations");
    boolean hasInstitutional = containsAny(headlines, "strategy", "microstrategy", "bernstein", "analyst", "analysts", "pads cash", "reserve");
    boolean hasSecurity = containsAny(headlines, "quantum", "wallet", "cold wallet", "security");
    boolean hasAltcoins = containsAny(headlines, "altcoin", "xrp", "dogecoin");

    List<String> bits = new ArrayList<>();
    if (hasInstitutional) bits.add("institutional and analyst commentary");
    if (hasLiquidations) bits.add("derivatives and liquidation dynamics");
    if (hasEtf) bits.add("ETF flows and broader market participation");
    if (hasSecurity) bits.add("security and longer-term protocol concerns");
    if (hasAltcoins) bits.add("spillover discussions across major crypto assets");
    if (bits.isEmpty()) bits.add("general crypto market developments");

    if (bits.size() == 1) return bits.get(0);
    if (bits.size() == 2) return bits.get(0) + " and " + bits.get(1);
    return String.join(", ", bits.subList(0, bits.size() - 1)) + ", and " + bits.get(bits.size() - 1);
  }

  private boolean containsAny(List<String> lines, String... needles) {
    for (String line : lines) {
      String l = line.toLowerCase(Locale.ROOT);
      for (String n : needles) {
        if (l.contains(n)) return true;
      }
    }
    return false;
  }

  private String bestConfidenceFromFactors(List<Map<String, Object>> factors) {
    if (factors == null || factors.isEmpty()) return "LOW";
    int total = 0;
    for (Map<String, Object> f : factors) {
      Object s = f.get("score");
      if (s instanceof Number n) total += n.intValue();
    }
    return total >= 130 ? "MED" : "LOW";
  }
}
