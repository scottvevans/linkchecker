package com.scottvevans.linkchecker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Data
@RequiredArgsConstructor
public class PageResponse {
  private final String uri;
  private final int httpStatus;
  private final String message;
  @JsonIgnore
  private final Set<String> links;
}
