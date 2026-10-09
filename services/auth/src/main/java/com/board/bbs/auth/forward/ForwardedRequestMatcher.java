package com.board.bbs.auth.forward;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/** 원래 요청(메서드, 경로)을 기준으로 일치를 판정한다. 그래서 스프링 시큐리티의 일반 인가 규칙을 원래 요청에 그대로 쓸 수 있다. */
public final class ForwardedRequestMatcher implements RequestMatcher {

  private final @Nullable HttpMethod method;
  private final PathPattern pattern;

  private ForwardedRequestMatcher(@Nullable HttpMethod method, String pattern) {
    this.method = method;
    this.pattern = PathPatternParser.defaultInstance.parse(pattern);
  }

  /**
   * 원래 요청 기준의 일치 조건을 만든다.
   *
   * @param method 원래 메서드. {@code null}이면 메서드를 가리지 않는다
   * @param pattern 원래 경로의 패턴
   * @return 일치 조건
   */
  public static ForwardedRequestMatcher forwarded(@Nullable HttpMethod method, String pattern) {
    return new ForwardedRequestMatcher(method, pattern);
  }

  @Override
  public boolean matches(HttpServletRequest request) {
    if (!ForwardAuthController.PATH.equals(request.getRequestURI())) {
      return false;
    }
    return ForwardedRequest.tryFrom(request)
        .filter(forwarded -> method == null || method.equals(forwarded.method()))
        .filter(forwarded -> pattern.matches(PathContainer.parsePath(forwarded.path())))
        .isPresent();
  }
}
