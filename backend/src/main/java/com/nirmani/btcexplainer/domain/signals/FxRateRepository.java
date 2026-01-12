package com.nirmani.btcexplainer.domain.signals;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FxRateRepository extends JpaRepository<FxRateDaily, Long> {
  Optional<FxRateDaily> findByRateDateAndBaseAndQuote(LocalDate date, String base, String quote);
}
