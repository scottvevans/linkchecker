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
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class CrawlerImplTests {

  private static MockWebServer mockWebServer;
  private Crawler crawler;
  private String baseURI;

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
    baseURI = String.format("http://localhost:%s", mockWebServer.getPort());
  }

  private String absURI(String path) {
    return String.format("%s/%s", baseURI, path);
  }

  private Set<String> absURIs(List<String> paths) {
    return paths.stream().map(this::absURI).collect(toSet());
  }

  private MockResponse okHtmlPage(String body) {
    return new MockResponse()
        .setResponseCode(HttpStatus.OK.value())
        .setBody(body)
        .setHeader("Content-Type", "text/html");
  }

  @Test
  void testSetUpAndTearDownWorks() {}

  @Test
  void testDepth1() throws Exception {
    var rootPageLinks = List.of("a.html", "b.html");
    var rootPage = page(baseURI, rootPageLinks);
    var rootWebServerResponse = okHtmlPage(rootPage);

    var aPageURI = absURI("a.html");
    var aPageLinks = List.of("aa.html", "ab.html");
    var aPage = page(aPageURI, aPageLinks);
    var aWebServerResponse = okHtmlPage(aPage);

    var bPageURI = absURI("b.html");
    var bPageLinks = List.of("ba.html", "bb.html");
    var bPage = page(bPageURI, bPageLinks);
    var bWebServerResponse = okHtmlPage(bPage);

    mockWebServer.enqueue(rootWebServerResponse);
    mockWebServer.enqueue(aWebServerResponse);
    mockWebServer.enqueue(bWebServerResponse);

    var expectedBaseURIResponse =
        new PageResponse(baseURI, 200, "OK", absURIs(rootPageLinks));

    var expectedResponses1 = Set.of(
        expectedBaseURIResponse,
        new PageResponse(aPageURI, 200, "OK", absURIs(aPageLinks)),
        new PageResponse(bPageURI, 200, "OK", absURIs(bPageLinks))
    );

    // this alternative is necessary because order of responses at a particular depth
    // in a crawl is non-determinant due to the requests executing in parallel,
    // but the mockServer always returns the page content in the order it was enqueued
    var expectedResponses2 = Set.of(
        expectedBaseURIResponse,
        new PageResponse(aPageURI, 200, "OK", absURIs(bPageLinks)),
        new PageResponse(bPageURI, 200, "OK", absURIs(aPageLinks))
    );

    Mono<CrawlerReport> reportMono = crawler.crawl(1, baseURI);
    StepVerifier.create(reportMono)
        .expectNextMatches(report ->
            report.getDepth() == 1 &&
            report.getTotalPagesCrawled() == 3 &&
            report.getRootURI().equals(baseURI) &&
            (Set.copyOf(report.getResponses()).equals(expectedResponses1) ||
             Set.copyOf(report.getResponses()).equals(expectedResponses2))
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
