package com.scottvevans.linkchecker.service;

import com.scottvevans.linkchecker.util.URIHelper;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Scans html content for links */
public interface HtmlParser {

  /** Given a list of links from an implementation, normalizes them into a set of unique URIs */
  default Set<String> findUniqueLinks(String url, String html) {
    final List<String> links = findLinks(url, html);
    final Set<String> set = new LinkedHashSet<>();

    for (var link: links) {
      set.add(URIHelper.normalizeUrl(link));
    }

    set.remove(URIHelper.normalizeUrl(url));

    return set;
  }

  /** Find all a href links in the html, including internal and external links */
  List<String> findLinks(String url, String html);
}
