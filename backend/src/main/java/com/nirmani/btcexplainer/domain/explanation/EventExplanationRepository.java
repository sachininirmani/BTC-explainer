package com.nirmani.btcexplainer.domain.explanation;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventExplanationRepository extends JpaRepository<EventExplanation, Long> {
  Optional<EventExplanation> findByEventId(Long eventId);
  void deleteByEventId(Long eventId);
}
