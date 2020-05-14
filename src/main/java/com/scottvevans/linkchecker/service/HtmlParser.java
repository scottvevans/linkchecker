package com.scottvevans.linkchecker.service;

import com.scottvevans.linkchecker.util.URIHelper;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Scans html content for links */
public interface HtmlParser {

  /** Given a list of links from an implementation, normalizes them into a set of unique URIs */
  default Set<String> findUniqueLinks(String uri, String html) {
    final List<String> links = findLinks(uri, html);
    final Set<String> set = new LinkedHashSet<>();

    for (var link: links) {
      set.add(URIHelper.normalizeURI(link));
    }

    set.remove(URIHelper.normalizeURI(uri));

    return set;
  }

  /** Find all a href links in the html, including internal and external links */
  List<String> findLinks(String uri, String html);
}
