package com.nirmani.btcexplainer.domain.signals;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsItemSampleRepository extends JpaRepository<NewsItemSample, Long> {
  List<NewsItemSample> findTop20ByItemDateOrderByPublishedAtDesc(LocalDate date);
  void deleteByItemDate(LocalDate date);
}
