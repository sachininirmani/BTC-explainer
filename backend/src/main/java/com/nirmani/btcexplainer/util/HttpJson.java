package com.nirmani.btcexplainer.util;

import com.nirmani.btcexplainer.domain.ops.ApiCallLogRepository;
import java.time.Duration;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class HttpJson {

  private final RestTemplate restTemplate = new RestTemplate();
  private final ApiCallLogRepository logRepo;

  public HttpJson(ApiCallLogRepository logRepo) {
    this.logRepo = logRepo;
  }

  public ResponseEntity<String> get(String provider, String url) {
    long start = System.currentTimeMillis();
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
      HttpEntity<Void> entity = new HttpEntity<>(headers);
      ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
      logRepo.log(provider, url, resp.getStatusCode().value(), (int)(System.currentTimeMillis()-start), null);
      return resp;
    } catch (Exception e) {
      logRepo.log(provider, url, null, (int)(System.currentTimeMillis()-start), e.getMessage());
      throw e;
    }
  }
}
