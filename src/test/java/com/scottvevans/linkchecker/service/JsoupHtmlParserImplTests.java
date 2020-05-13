package com.scottvevans.linkchecker.service;

import com.scottvevans.linkchecker.service.impl.JsoupHtmlParserImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class JsoupHtmlParserImplTests {
  private HtmlParser parser;

  private static final String EBAY_INC_BASE = "https://www.ebayinc.com/";
  private static final String COMPANY = "company/";
  private static final String COMPANY_ABS = EBAY_INC_BASE + "company/";
  private static final String COMPANY_FRAGMENT = "company/#main-content";
  private static final String COMPANY_FRAGMENT_ABS = EBAY_INC_BASE + "company/#main-content";
  private static final String COMPANY_QUERY_1 = "company/?a=b&c=d&y=z";
  private static final String COMPANY_QUERY_2 = "company/?a=b&y=z&c=d";
  private static final String COMPANY_QUERY_3 = "company/?c=d&a=b&y=z";
  private static final String COMPANY_QUERY_FRAGMENT = "company/?y=z&c=d&a=b#main-content";
  private static final String COMPANY_QUERY_SORTED_ABS = EBAY_INC_BASE + "company/?a=b&c=d&y=z";
  private static final String GOOGLE_ABOUT_US = "https://about.google/stories";

  @BeforeEach
  private void setup() {
    parser = new JsoupHtmlParserImpl();
  }

  @AfterEach
  private void tearDown() {
    parser = null;
  }

  @Test void testFindUniqueStrings() {
    var links = List.of(COMPANY, COMPANY_FRAGMENT);
    var html = fragment(links);
    var expected = Set.of(COMPANY_ABS);
    var actual = parser.findUniqueLinks(EBAY_INC_BASE, html);
    assertEquals(expected, actual, "ebay our company overview without fragment");

    links = List.of(COMPANY_QUERY_1, COMPANY_QUERY_2, COMPANY_QUERY_3, COMPANY_QUERY_FRAGMENT);
    html = page(EBAY_INC_BASE, links);
    expected = Set.of(COMPANY_QUERY_SORTED_ABS);
    actual = parser.findUniqueLinks(EBAY_INC_BASE, html);
    assertEquals(expected, actual, "ebay our company overview with sorted query string");
  }

  @Test
  void shouldFindAndResolveRelativeInternalStringInHtml() {
    var links = List.of(COMPANY, COMPANY_FRAGMENT);
    var html = fragment(links);
    var expected = List.of(COMPANY_ABS, COMPANY_FRAGMENT_ABS);
    var actual = parser.findLinks(EBAY_INC_BASE, html);
    assertEquals(expected, actual, "ebay our company overview relative link");
  }
  @Test
  void shouldFindInternalAbsoluteStringInHtml() {
    var expected = List.of(COMPANY_ABS);
    var html = fragment(expected);
    var actual = parser.findLinks(EBAY_INC_BASE, html);
    assertEquals(expected, actual, "ebay our company overview link");
  }

  @Test
  void shouldFindExternalStringInHtml() {
    var expected = List.of(GOOGLE_ABOUT_US);
    var html = fragment(expected);
    var actual = parser.findLinks(EBAY_INC_BASE, html);
    assertEquals(expected, actual, "google about us stories link");
  }

  @Test
  void shouldThrowIllegalArgumentExceptionsForInvalidUrl() {
    var links = List.of(COMPANY);
    var html = fragment(links);
    try {
      parser.findLinks(null, html);
      fail("should throw an IllegalArgumentException if url is null");
    } catch (IllegalArgumentException ignored) {}

    try {
      parser.findLinks("", html);
      fail("should throw an IllegalArgumentException if url is empty String");
    } catch (IllegalArgumentException ignored) {}

    try {
      parser.findLinks("badurl.html", html);
      fail("should throw an IllegalArgumentException if url is malformed");
    } catch (IllegalArgumentException ignored) {}
  }

  @Test
  void shouldFindMultipleStringsInPage() {
    var expected = List.of(GOOGLE_ABOUT_US, COMPANY_ABS);

    var pageNoBaseHref = page(null, expected);
    assertEquals(expected, parser.findLinks(EBAY_INC_BASE, pageNoBaseHref), "multiple links, no base href");

    var pageWithBaseHref = page(EBAY_INC_BASE, expected);
    assertEquals(expected, parser.findLinks(EBAY_INC_BASE, pageWithBaseHref), "multiple links, with base href");
  }

  private static String page(String baseHref, List<String> links) {
    var html = new StringBuilder();
    html.append("<html>");
    html.append("<head>");
    if (baseHref != null) {
      html.append("<base href=\"");
      html.append(baseHref);
      html.append("\" />");
    }
    html.append("</head>");
    html.append("<body>");
    html.append(fragment(links));
    html.append("</body>");
    html.append("/<html>");

    return html.toString();
  }

  private static String fragment(List<String> links) {
    var html = new StringBuilder();

    html.append("<div>");
    for (var link : links) {
      html.append("<a href=\"");
      html.append(link);
      html.append("\">link text</a>");
    }
    html.append("</div>");

    return html.toString();
  }
}