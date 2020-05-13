package com.scottvevans.linkchecker.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@RequiredArgsConstructor
public class CrawlerReport {
  private final String rootUri;
  private final int depth;
  private final long elaspedTimeInMillis;
  private final int totalPagesCrawled;
  private final Map<Integer, Integer> statusCounts;
  private final List<PageResponse> responses;
}
