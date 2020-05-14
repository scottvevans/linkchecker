package com.scottvevans.linkchecker.service.impl;

import java.util.List;

public class HtmlHelper {
  static String page(String baseHref, List<String> links) {
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

  static String fragment(List<String> links) {
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
