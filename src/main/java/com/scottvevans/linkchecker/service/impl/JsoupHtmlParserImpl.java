package com.scottvevans.linkchecker.service.impl;

import com.scottvevans.linkchecker.service.HtmlParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static java.util.stream.Collectors.toList;

/** HtmlParser implementation based on the popular Jsoup HTML parser */
@Service
public class JsoupHtmlParserImpl implements HtmlParser {
  private static final String LINK_CSS_QUERY = "a[href]";
  private static final String HREF_KEY = "href";

  @Override
  public List<String> findLinks(String url, String html) {
    if ("".equals(url)) throw new IllegalArgumentException("url is empty");
    try {
      new URL(url);
    } catch (MalformedURLException ex) {
      throw new IllegalArgumentException("invalid url", ex);
    }
    var document = Jsoup.parse(html, url);
    var links = document.select(LINK_CSS_QUERY);
    return links
        .stream()
        .map(JsoupHtmlParserImpl::getLink)
        .filter(l -> !l.equals("") && !l.toLowerCase().startsWith("mailto:"))
        .sorted()
        .collect(toList());
  }

  /** Given a Jsoup a[href] Element, resolve it href to an absolute link uri */
  private static String getLink(Element aHref) {
    return aHref.absUrl(HREF_KEY);
  }
}
