package com.nirmani.btcexplainer.service.ingest;

import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class WeatherIngestService {

    /**
     * Educational placeholder.
     * In a real system this would call a weather API and store extreme events.
     */
    public void ingestExtremes(LocalDate date) {
        // No-op for now (kept simple and safe)
    }
}
