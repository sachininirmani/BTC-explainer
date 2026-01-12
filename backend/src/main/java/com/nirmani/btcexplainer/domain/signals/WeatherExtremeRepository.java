package com.nirmani.btcexplainer.domain.signals;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeatherExtremeRepository extends JpaRepository<WeatherExtremeDaily, Long> {
  List<WeatherExtremeDaily> findByWxDate(LocalDate date);
}
