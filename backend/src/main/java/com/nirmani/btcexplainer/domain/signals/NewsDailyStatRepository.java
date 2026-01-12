package com.nirmani.btcexplainer.domain.signals;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsDailyStatRepository extends JpaRepository<NewsDailyStat, Long> {
  Optional<NewsDailyStat> findByStatDate(LocalDate date);
}
