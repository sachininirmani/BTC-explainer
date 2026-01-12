package com.nirmani.btcexplainer.domain.price;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface DailyCandleRepository extends JpaRepository<DailyCandle, Long> {

  @Query("select c from DailyCandle c where c.asset.id = :assetId order by c.candleDate asc")
  List<DailyCandle> findAllByAsset(@Param("assetId") Long assetId);

  @Query("select c from DailyCandle c where c.asset.id = :assetId and c.candleDate >= :startDate order by c.candleDate asc")
  List<DailyCandle> findByAssetFrom(
      @Param("assetId") Long assetId,
      @Param("startDate") LocalDate startDate
  );

  // ✅ NEW: immutable insert
  @Modifying
  @Transactional
  @Query(
    value = """
      INSERT INTO price_candles_daily
        (asset_id, candle_date, open, close, high, low, volume)
      VALUES
        (:assetId, :date, :open, :close, :high, :low, :volume)
      ON CONFLICT (asset_id, candle_date)
      DO NOTHING
    """,
    nativeQuery = true
  )
  void insertIfNotExists(
      @Param("assetId") Long assetId,
      @Param("date") LocalDate date,
      @Param("open") BigDecimal open,
      @Param("close") BigDecimal close,
      @Param("high") BigDecimal high,
      @Param("low") BigDecimal low,
      @Param("volume") BigDecimal volume
  );
}
