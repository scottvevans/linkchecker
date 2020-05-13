package com.scottvevans.linkchecker.util;

import java.net.URI;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public class URIHelper {
  private static final String PARAM_REGEX = "^[a-zA-Z_][^=]*=[^&]+$";
  private static final Predicate<String> IS_VALID_PARAM = Pattern.compile(PARAM_REGEX).asMatchPredicate();

  public static String normalizeUrl(final String url) {
    String normalizedUrl = removeFragment(url);
    final int queryStringIndex = normalizedUrl.indexOf('?');

    if (queryStringIndex != -1) {
      if (queryStringIndex + 1 == normalizedUrl.length()) {
        normalizedUrl = normalizedUrl.substring(0, queryStringIndex);
      } else {
        String queryString = normalizedUrl.substring(queryStringIndex + 1);
        Optional<String> sortedQueryStringMaybe = getSortedQueryString(queryString);

        if (sortedQueryStringMaybe.isEmpty()) {
          normalizedUrl = normalizedUrl.substring(0, queryStringIndex);
        } else {
          normalizedUrl = normalizedUrl.substring(0, queryStringIndex + 1) + sortedQueryStringMaybe.get();
        }
      }
    }

    return normalizedUrl;
  }

  public static String removeFragment(String url) {
    final int index = url.indexOf('#');
    if (index != -1) {
      url = url.substring(0, index);
    }
    return url;
  }

  public static Optional<String> getSortedQueryString(final String queryString) {
    Optional<String> sortedQueryStringMaybe = Optional.empty();

    if (queryString != null && !queryString.equals("")) {
      final String[] paramPairs = queryString.split("&");
      final Stream<String> validParamsSorted = Arrays.stream(paramPairs).filter(IS_VALID_PARAM).sorted();
      final String sortedQueryString = validParamsSorted.collect(joining("&"));

      if (sortedQueryString.length() > 0) {
        sortedQueryStringMaybe = Optional.of(sortedQueryString);
      }
    }

    return sortedQueryStringMaybe;
  }

  public static String resolveRedirectURI(String originalAbsoluteURI, String redirect) {
    URI redirectURI = URI.create(redirect);

    if (!redirectURI.isAbsolute()) {
      URI originalURI = URI.create(originalAbsoluteURI);
      String authority = originalURI.getAuthority();
      int index = originalAbsoluteURI.indexOf(authority) + authority.length();
      String base = originalAbsoluteURI.substring(0, index);
      String newLocation = base + redirectURI.toString();
      redirectURI = URI.create(newLocation);
      assert redirectURI.isAbsolute();
    }

    return redirectURI.toString();
  }
}
