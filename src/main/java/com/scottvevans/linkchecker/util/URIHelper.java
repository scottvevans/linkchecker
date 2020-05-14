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

  public static String normalizeURI(final String uri) {
    String normalizedURI = removeFragment(uri);
    final int queryStringIndex = normalizedURI.indexOf('?');

    if (queryStringIndex != -1) {
      if (queryStringIndex + 1 == normalizedURI.length()) {
        normalizedURI = normalizedURI.substring(0, queryStringIndex);
      } else {
        String queryString = normalizedURI.substring(queryStringIndex + 1);
        Optional<String> sortedQueryStringMaybe = getSortedQueryString(queryString);

        if (sortedQueryStringMaybe.isEmpty()) {
          normalizedURI = normalizedURI.substring(0, queryStringIndex);
        } else {
          normalizedURI = normalizedURI.substring(0, queryStringIndex + 1) + sortedQueryStringMaybe.get();
        }
      }
    }

    return normalizedURI;
  }

  public static String removeFragment(String uri) {
    final int index = uri.indexOf('#');
    if (index != -1) {
      uri = uri.substring(0, index);
    }
    return uri;
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
