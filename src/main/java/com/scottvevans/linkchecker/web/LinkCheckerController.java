package com.scottvevans.linkchecker.web;

import com.scottvevans.linkchecker.model.CrawlerReport;
import com.scottvevans.linkchecker.service.Crawler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@RestController
@RequestMapping(path="/check", produces="application/json")
@Validated
@Slf4j
public class LinkCheckerController {
  private final Crawler crawler;

  @Autowired
  public LinkCheckerController(Crawler crawler) {
    log.info("LinkCheckerController created");
    this.crawler = crawler;
  }

  @GetMapping
  public Mono<CrawlerReport> check(
      @RequestParam
      @Min(value = 1, message = "depth param must be a positive number <= 5")
      @Max(value = 5, message = "depth param must be a positive number <= 5") Integer depth,
      @RequestParam @NotBlank(message = "uri param is required") String uri) {
    log.info("check depth: {} uri: {}", depth, uri);
    return crawler.crawl(depth, uri);
  }
}
