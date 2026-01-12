package com.nirmani.btcexplainer.domain.event;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketEventRepository extends JpaRepository<MarketEvent, Long> {

  @Query("""
      select e
      from MarketEvent e
      where e.asset.id = :assetId
      order by e.eventDate desc
      """)
  List<MarketEvent> findLatestByAsset(
      @Param("assetId") Long assetId,
      Pageable pageable
  );

  @Query("""
      select e
      from MarketEvent e
      where e.asset.id = :assetId
        and e.eventDate = :eventDate
      """)
  List<MarketEvent> findByAssetAndDate(
      @Param("assetId") Long assetId,
      @Param("eventDate") LocalDate eventDate
  );

  @Query("""
      select e
      from MarketEvent e
      where e.asset.id = :assetId
        and e.eventDate >= :startDate
      order by e.eventDate asc
      """)
  List<MarketEvent> findByAssetFrom(
      @Param("assetId") Long assetId,
      @Param("startDate") LocalDate startDate
  );

  long deleteByAssetIdAndEventDateBefore(Long assetId, LocalDate cutoffDate);
}
