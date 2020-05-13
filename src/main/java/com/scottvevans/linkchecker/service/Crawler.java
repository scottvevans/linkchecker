package com.scottvevans.linkchecker.service;

import com.scottvevans.linkchecker.model.CrawlerReport;
import reactor.core.publisher.Mono;

/** Crawls an absolute URI looking for links and validating them in to a maximum depth */
public interface Crawler {
  Mono<CrawlerReport> crawl(int maxDepth, String uri);
}
