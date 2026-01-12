package com.nirmani.btcexplainer.client.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nirmani.btcexplainer.domain.ops.ApiCallLogRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpenAiClient {

  private final ApiCallLogRepository logRepo;
  private final ObjectMapper om = new ObjectMapper();
  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10))
      .build();

  @Value("${openai.api.key:}")
  private String apiKey;

  @Value("${openai.model:gpt-4o-mini}")
  private String model;

  @Value("${openai.enabled:true}")
  private boolean enabled;

  public OpenAiClient(ApiCallLogRepository logRepo) {
    this.logRepo = logRepo;
  }

  public record ChatResult(String content, String modelUsed) {}

  public ChatResult chat(String userPrompt, double temperature) throws Exception {
    if (!enabled) {
      throw new IllegalStateException("OpenAI is disabled");
    }
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("OPENAI_API_KEY is not configured");
    }

    String body = om.writeValueAsString(
        om.createObjectNode()
            .put("model", model)
            .put("temperature", temperature)
            .set("messages", om.createArrayNode()
                .add(om.createObjectNode().put("role", "user").put("content", userPrompt)))
    );

    long start = System.currentTimeMillis();
    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create("https://api.openai.com/v1/chat/completions"))
        .timeout(Duration.ofSeconds(25))
        .header("Authorization", "Bearer " + apiKey)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();

    try {
      HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
      logRepo.log("openai", "/v1/chat/completions", resp.statusCode(), (int) (System.currentTimeMillis() - start), null);

      if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
        throw new RuntimeException("OpenAI HTTP " + resp.statusCode() + ": " + resp.body());
      }

      JsonNode root = om.readTree(resp.body());
      String content = root.path("choices").get(0).path("message").path("content").asText();
      String usedModel = root.path("model").asText(model);
      return new ChatResult(content, usedModel);
    } catch (Exception e) {
      logRepo.log("openai", "/v1/chat/completions", null, (int) (System.currentTimeMillis() - start), e.getMessage());
      throw e;
    }
  }
}
