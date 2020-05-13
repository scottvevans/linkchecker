package com.scottvevans.linkchecker.util;

import com.scottvevans.linkchecker.util.URIHelper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class URLHelperTests {

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
  void testNormalizeUrl() {
    var urlWithFragment = "https://www.ebayinc.com/company/#main-context";
    var urlWithQueryString = "https://www.ebayinc.com/company/?y=z&c=d&a=b&y=x";
    var urlWithBoth = "https://www.ebayinc.com/company/?y=z&c=d&a=b&y=x#main-context";
    var expected1 = "https://www.ebayinc.com/company/";
    var expected2 = "https://www.ebayinc.com/company/?a=b&c=d&y=x&y=z";

    assertEquals(expected1, URIHelper.normalizeUrl(urlWithFragment), "normalized url with fragment");
    assertEquals(expected2, URIHelper.normalizeUrl(urlWithQueryString), "normalized url with query string");
    assertEquals(expected2, URIHelper.normalizeUrl(urlWithBoth), "normalized url with fragment and query string");
  }

}
