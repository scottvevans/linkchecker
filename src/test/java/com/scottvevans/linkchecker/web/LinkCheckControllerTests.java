package com.scottvevans.linkchecker.web;

import com.scottvevans.linkchecker.model.CrawlerReport;
import com.scottvevans.linkchecker.model.PageResponse;
import com.scottvevans.linkchecker.service.Crawler;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = LinkCheckerController.class)
public class LinkCheckControllerTests {
  private static final String BAD_REQUEST = "Bad Request";
  private static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
  private static final String TYPE_MISMATCH = "Type mismatch.";
  private static final String DEPTH_SHOULD_BE = "check.depth: depth param must be a positive number <= 5";
  private static final String DEPTH_REQUIRED = "Required Integer parameter 'depth' is not present";
  private static final String URI_REQUIRED = "Required String parameter 'uri' is not present";

  @MockBean
  private Crawler crawler;

  @Autowired
  private WebTestClient webTestClient;


  @Test
  void testCallsCrawlerAndReturnsCrawlerReportMono() {
    var uri = "https://www.acme.com";
    var pageResponse = new PageResponse(uri, 200, "OK", Set.of("a.html", "b.html"));
    var crawlerReport =
        new CrawlerReport(uri, 1, 1, 1, Map.of(200, 1), List.of(pageResponse));
    Mockito.when(crawler.crawl(1, uri)).thenReturn(Mono.just(crawlerReport));

    var expectedPageResponse = new PageResponse(uri, 200, "OK", null);
    var expectedCrawlerReport =
        new CrawlerReport(uri, 1, 1, 1, Map.of(200, 1), List.of(expectedPageResponse));

    webTestClient.get().uri("/check?depth=1&uri=" + uri)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectBody(CrawlerReport.class).isEqualTo(expectedCrawlerReport);
  }


  @Test
  void testValidationFailures() {
    testValidationFails("/check", HttpStatus.BAD_REQUEST, BAD_REQUEST, DEPTH_REQUIRED);

    testValidationFails("/check?depth=1", HttpStatus.BAD_REQUEST, BAD_REQUEST, URI_REQUIRED);

    testValidationFails("/check?depth=0&uri=http://www.ebayinc.com/company/",
        //@TODO fix spring validation not returning BAD_REQUEST @see DefaultErrorWebExceptionHandler
        HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR, DEPTH_SHOULD_BE);

    testValidationFails("/check?depth=6&uri=http://www.ebayinc.com/company/",
        //@TODO fix spring validation not returning BAD_REQUEST @see DefaultErrorWebExceptionHandler
        HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR, DEPTH_SHOULD_BE);

    testValidationFails("/check?depth=scott&uri=http://www.ebayinc.com/company/",
        HttpStatus.BAD_REQUEST, BAD_REQUEST, TYPE_MISMATCH);
  }

  private void testValidationFails(String uri, HttpStatus expectedStatus, String expectedError, String expectedMessage) {
    webTestClient.get().uri(uri)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(expectedStatus)
        .expectBody()
        .jsonPath("$.error").exists()
        .jsonPath("$.error").isEqualTo(expectedError)
        .jsonPath("$.message").exists()
        .jsonPath("$.message").isEqualTo(expectedMessage);
  }

  private void logJsonResult(EntityExchangeResult<byte[]> result) {
    log.info(result.toString());
  }
}
