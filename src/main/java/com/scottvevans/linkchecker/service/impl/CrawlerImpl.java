package com.scottvevans.linkchecker.service.impl;

import com.scottvevans.linkchecker.model.CrawlerReport;
import com.scottvevans.linkchecker.model.PageResponse;
import com.scottvevans.linkchecker.service.Crawler;
import com.scottvevans.linkchecker.service.HtmlParser;
import com.scottvevans.linkchecker.util.URIHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CrawlerImpl implements Crawler {

  private final HtmlParser parser;
  private final WebClient webClient;

  @Autowired
  public CrawlerImpl(HtmlParser parser) {
    this.parser = parser;
    this.webClient = WebClient.create();
  }

  @Override
  public Mono<CrawlerReport> crawl(int maxDepth, String uri) {
    if (maxDepth < 1 || maxDepth > 5)
      throw new IllegalArgumentException("maxDepth must be a positive integer <= 5");
    log.info("crawling uri: {} depth: {}", uri, maxDepth);
    long startTime = System.currentTimeMillis();
    Mono<List<PageResponse>> responsesMono =
        crawl(new LinkedList<>(), new HashSet<>(), maxDepth, Set.of(uri), 0);
    return responsesMono.map(responses -> toCrawlerReport(maxDepth, uri, startTime, responses));
  }

  /** Converts the generated list of all PageResponses into the final CrawlerReport, including basic stats */
  private CrawlerReport toCrawlerReport(int maxDepth, String uri, long startTime, List<PageResponse> responses) {
    int pages = responses.size();

    Map<Integer, List<PageResponse>> groupedByStatus =
        responses.stream().collect(Collectors.groupingBy(PageResponse::getHttpStatus));

    Map<Integer, Integer> counts = new HashMap<>();
    groupedByStatus.forEach((key, list) -> counts.put(key, list.size()));

    long elapsedTime = System.currentTimeMillis() - startTime;

    return new CrawlerReport(uri, maxDepth, elapsedTime, pages, counts, responses);
  }

  /** Recursive method to crawl to successive depths while avoiding any duplicates encountered between the pages/levels */
  private Mono<List<PageResponse>> crawl(
      List<PageResponse> responses, Set<String> visited, int maxDepth, Set<String> toVisit, int currentDepth) {
    log.debug("crawl currentDepth: {}", currentDepth);
    return getAll(toVisit)
        .collectList()
        .flatMap(list -> {
          responses.addAll(list);
          if (currentDepth == maxDepth) {
            return Mono.just(responses);
          } else {
            visited.addAll(toVisit);
            Set<String> nextToVisit = list
                .stream()
                .flatMap(pr -> pr.getLinks().stream())
                .filter(uri -> !visited.contains(uri))
                .collect(Collectors.toSet());
            return crawl(responses, visited, maxDepth, nextToVisit, currentDepth + 1);
          }
        });
  }

  /** Retrieves all URIs in the set asynchronously in parallel */
  private Flux<PageResponse> getAll(Set<String> URIs) {
    log.debug("getAll {}", URIs);
    return Flux.fromIterable(URIs).flatMap(uri -> getPageResponse(uri).subscribeOn(Schedulers.parallel()));
  }

  /** Attempts to retrieve the uri, and populates a PageResponse with the outcome */
  private Mono<PageResponse> getPageResponse(String uri) {
    log.info("getPageResponse uri: {}", uri);
    return webClient
        .get()
        .uri(uri)
        .exchange()
        .flatMap(clientResponse -> toPageResponse(uri, clientResponse))
        .onErrorResume(ex -> {
          var msg = String.format("ERROR: processing failed due to %s: %s",
              ex.getClass().getSimpleName(), ex.getMessage());
          return Mono.just(new PageResponse(uri, -1, msg, Collections.emptySet()));
        });

  }

  /** Converts a raw ClientRequest from the WebClient into our custom PageResponse value object */
  private Mono<PageResponse> toPageResponse(String uri, ClientResponse response) {
    log.debug("toPageResponse: {}", uri);
    try {
      HttpStatus status = response.statusCode();
      HttpHeaders headers = response.headers().asHttpHeaders();
      log.debug("toPageResponse uri: {} status: {} headers: {}", uri, status, headers);

      if (status.is2xxSuccessful())
        return handleSuccessful(uri, response);
      else if (status.is3xxRedirection())
        return handleRedirect(uri, response);
      else
        return handleError(uri, response);

    } catch (RuntimeException ex) {
      String msg = String.format("processing %s failed due to unexpected error %s", uri, ex);
      log.error(msg);
      return Mono.just(
          new PageResponse(uri, -1, msg, Collections.emptySet()));
    }
  }

  /** Populates a Mono with a PageResponse for success, parsing the response body for links to check further */
  private Mono<PageResponse> handleSuccessful(String uri, ClientResponse response) {
    log.debug("handleSuccessful");
    HttpStatus status = response.statusCode();
    HttpHeaders headers = response.headers().asHttpHeaders();
    if (headers.getContentType() != null && headers.getContentType().isCompatibleWith(MediaType.TEXT_HTML)) {
      return response
          .bodyToMono(String.class)
          .map(html ->
              new PageResponse(uri, status.value(), status.getReasonPhrase(), parser.findUniqueLinks(uri, html)))
          .onErrorResume(ex -> {
            var msg = String.format("ERROR: processing failed due to %s: %s",
                ex.getClass().getSimpleName(), ex.getMessage());
            return Mono.just(new PageResponse(uri, status.value(), msg, Collections.emptySet()));
          });
    } else {
      response.releaseBody();
      return Mono.just(new PageResponse(uri, status.value(),
          "not an html page; Content-Type: " + headers.getContentType(), Collections.emptySet()));
    }
  }

  /** Populates a Mono with a PageResponse for a redirect, and if location is present includes it as a link to follow */
  private Mono<PageResponse> handleRedirect(String uri, ClientResponse response) {
    log.debug("handleRedirect");
    URI redirectLocation = response.headers().asHttpHeaders().getLocation();

    if (redirectLocation != null) {
      String redirectURI = URIHelper.resolveRedirectURI(uri, redirectLocation.toString());
      String message = String.format("%s -> %s", response.statusCode().getReasonPhrase(), redirectURI);
      return Mono.just(
          new PageResponse(uri, response.statusCode().value(), message, Set.of(redirectURI))
      );
    } else {
      String message = String.format("%s -> new location unknown", response.statusCode().getReasonPhrase());
      return Mono.just(
          new PageResponse(uri, response.statusCode().value(), message, Collections.emptySet())
      );
    }
  }

  /** Populates a Mono with a PageResponse for status codes outside the 200 and 300 ranges */
  private Mono<PageResponse> handleError(String uri, ClientResponse response) {
    log.debug("handleError");
    return Mono.just(
        new PageResponse(uri, response.statusCode().value(),
            response.statusCode().getReasonPhrase(), Collections.emptySet()));
  }
}
