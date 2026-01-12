package com.nirmani.btcexplainer.service.ingest;

import com.nirmani.btcexplainer.client.gdelt.GdeltClient;
import com.nirmani.btcexplainer.domain.signals.NewsDailyStat;
import com.nirmani.btcexplainer.domain.signals.NewsDailyStatRepository;
import com.nirmani.btcexplainer.domain.signals.NewsItemSample;
import com.nirmani.btcexplainer.domain.signals.NewsItemSampleRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NewsIngestService {

  /**
   * Primary query tries to capture BTC / crypto content while keeping query simple.
   * We keep fallbacks below because some days can be noisy or return empty depending on query.
   */
  private static final String PRIMARY_QUERY = "(bitcoin OR btc OR cryptocurrency OR crypto)";

  private static final List<String> FALLBACK_QUERIES = List.of(
      "(bitcoin OR btc)",
      "(cryptocurrency OR crypto)",
      "bitcoin",
      "btc"
  );

  private final GdeltClient gdelt;
  private final NewsDailyStatRepository statRepo;
  private final NewsItemSampleRepository itemRepo;

  public NewsIngestService(GdeltClient gdelt, NewsDailyStatRepository statRepo, NewsItemSampleRepository itemRepo) {
    this.gdelt = gdelt;
    this.statRepo = statRepo;
    this.itemRepo = itemRepo;
  }

  /**
   * Ingest raw article counts + a small sample of headlines for a given day (UTC date).
   *
   * <p>Notes:</p>
   * <ul>
   *   <li>Counts are fetched via TimelineVolRaw (raw article counts).</li>
   *   <li>Headlines use ArtList and may be empty for some queries; we retry with fallbacks.</li>
   * </ul>
   */
  @Transactional
  public void ingestForDate(LocalDate date) {
    // Count using the primary query (raw article count)
    int count = gdelt.fetchNewsCount(date, PRIMARY_QUERY);

    // Fetch headlines using a query strategy:
    // 1) primary query
    // 2) fallback queries until we get something
    List<GdeltClient.NewsItem> top = new ArrayList<>();
    String usedQuery = PRIMARY_QUERY;

    top = gdelt.fetchTopArticles(date, PRIMARY_QUERY, 20);
    if (top.isEmpty()) {
      for (String q : FALLBACK_QUERIES) {
        top = gdelt.fetchTopArticles(date, q, 20);
        if (!top.isEmpty()) {
          usedQuery = q;
          break;
        }
      }
    }

    // If we found titles but the raw count is zero, use a conservative effective count
    // so that UI doesn't show an all-zero "News activity".
    int effectiveCount = Math.max(count, top.size());

    NewsDailyStat stat = statRepo.findByStatDate(date).orElseGet(NewsDailyStat::new);
    stat.setStatDate(date);
    stat.setQueryTag(usedQuery);
    stat.setArticleCount(effectiveCount);
    statRepo.save(stat);

    // Store sample headlines for human-readable context
    itemRepo.deleteByItemDate(date);
    for (GdeltClient.NewsItem n : top) {
      if (n.title() == null || n.title().isBlank()) continue;

      NewsItemSample item = new NewsItemSample();
      item.setItemDate(date);
      item.setTitle(n.title());
      item.setSource(n.source());
      item.setUrl(n.url());
      item.setPublishedAt(n.publishedAt());
      itemRepo.save(item);
    }
  }
}
