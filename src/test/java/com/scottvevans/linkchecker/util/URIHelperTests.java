package com.scottvevans.linkchecker.util;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class URIHelperTests {

  @Test
  void testRemoveFragment() {
    var url = "https://www.ebayinc.com/company/#main-content";
    var expected = "https://www.ebayinc.com/company/";
    assertEquals(expected, URIHelper.removeFragment(url), "remove #main-content fragment");
    assertEquals(expected, URIHelper.removeFragment(expected), "no fragment");
  }

  @Test
  void testRemoveFragmentWithQueryString() {
    var url = "https://www.ebayinc.com/company/?a=b#main-content";
    var expected = "https://www.ebayinc.com/company/?a=b";
    assertEquals(expected, URIHelper.removeFragment(url), "remove #main-content fragment");
    assertEquals(expected, URIHelper.removeFragment(expected), "no fragment");
  }

  @Test
  void testGetSortedQueryString() {
    var queryString = "y=z&c=d&a=b&y=x";
    var expected = Optional.of("a=b&c=d&y=x&y=z");
    assertEquals(expected, URIHelper.getSortedQueryString(queryString), "sorted query string");
  }

  @Test
  void testNormalizeURI() {
    var uriWithFragment = "https://www.ebayinc.com/company/#main-context";
    var uriWithQueryString = "https://www.ebayinc.com/company/?y=z&c=d&a=b&y=x";
    var uriWithBoth = "https://www.ebayinc.com/company/?y=z&c=d&a=b&y=x#main-context";
    var expected1 = "https://www.ebayinc.com/company/";
    var expected2 = "https://www.ebayinc.com/company/?a=b&c=d&y=x&y=z";

    assertEquals(expected1, URIHelper.normalizeURI(uriWithFragment), "normalized uri with fragment");
    assertEquals(expected2, URIHelper.normalizeURI(uriWithQueryString), "normalized uri with query string");
    assertEquals(expected2, URIHelper.normalizeURI(uriWithBoth), "normalized url with fragment and query string");
  }

  @Test
  void testResolveRedirectURI() {
    var originalURI = "https://www.ebayinc.com/company";
    var relative = "/company/";
    var expected = "https://www.ebayinc.com/company/";

    assertEquals(expected, URIHelper.resolveRedirectURI(originalURI, relative), "resolved relative redirect");

    var absolute = "https://www.google.com/";
    assertEquals(absolute, URIHelper.resolveRedirectURI(originalURI, absolute));
  }

}
