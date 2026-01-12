package com.nirmani.btcexplainer.domain.signals;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FearGreedRepository extends JpaRepository<FearGreedDaily, Long> {
  Optional<FearGreedDaily> findBySentimentDate(LocalDate date);
}
