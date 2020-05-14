package com.scottvevans.linkchecker.service.impl;

import com.scottvevans.linkchecker.model.CrawlerReport;
import com.scottvevans.linkchecker.model.PageResponse;
import com.scottvevans.linkchecker.service.Crawler;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static com.scottvevans.linkchecker.service.impl.HtmlHelper.page;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class CrawlerImplTests {

  private static MockWebServer mockWebServer;
  private Crawler crawler;
  private String baseUri;

  @BeforeAll
  static void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
  }

  @AfterAll
  static void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @BeforeEach
  void init() {
    crawler = new CrawlerImpl(new JsoupHtmlParserImpl());
    baseUri = String.format("http://localhost:%s", mockWebServer.getPort());
  }

  @Test
  void testSetUpAndTearDownWorks() {}

  private MockResponse okHtmlPage(String body) {
    return new MockResponse()
        .setResponseCode(HttpStatus.OK.value())
        .setBody(body)
        .setHeader("Content-Type", "text/html");
  }

  @Test
  void testDepth1() throws Exception {
    var rootPageLinks = List.of("a.html", "b.html");
    var rootPage = page(baseUri, rootPageLinks);
    var rootWebServerResponse = okHtmlPage(rootPage);
    var rootPageResponse =
        new PageResponse(baseUri, 200, "OK", Set.copyOf(rootPageLinks));

    var aPageUri = baseUri + "/a.html";
    var aPageLinks = List.of("aa.html", "ab.html");
    var aPage = page(aPageUri, aPageLinks);
    var aWebServerResponse = okHtmlPage(aPage);
    var aPageResponse =
        new PageResponse(aPageUri,200, "OK", Set.copyOf(aPageLinks));

    var bPageUri = baseUri + "/b.html";
    var bPageLinks = List.of("ba.html", "bb.html");
    var bPage = page(bPageUri, bPageLinks);
    var bWebServerResponse = okHtmlPage(bPage);
    var bPageResponse =
        new PageResponse(bPageUri, 200, "OK", Set.copyOf(bPageLinks));

    mockWebServer.enqueue(rootWebServerResponse);
    mockWebServer.enqueue(aWebServerResponse);
    mockWebServer.enqueue(bWebServerResponse);

    Mono<CrawlerReport> reportMono = crawler.crawl(1, baseUri);
    StepVerifier.create(reportMono)
        .expectNextMatches(report ->
            report.getDepth() == 1 &&
            report.getTotalPagesCrawled() == 3 &&
            report.getRootUri().equals(baseUri)
        ).verifyComplete();

    var rootRecordedRequest = mockWebServer.takeRequest();
    assertEquals("GET", rootRecordedRequest.getMethod());
    assertEquals("/", rootRecordedRequest.getPath());

    var expectedChildPaths = Set.of("/a.html", "/b.html");
    var aRecordedRequest = mockWebServer.takeRequest();
    assertEquals("GET", aRecordedRequest.getMethod());
    assertTrue(expectedChildPaths.contains(aRecordedRequest.getPath()));

    var bRecordedRequest = mockWebServer.takeRequest();
    assertEquals("GET", bRecordedRequest.getMethod());
    assertTrue(expectedChildPaths.contains(bRecordedRequest.getPath()));
  }

}
