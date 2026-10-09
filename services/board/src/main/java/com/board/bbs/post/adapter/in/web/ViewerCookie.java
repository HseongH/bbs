package com.board.bbs.post.adapter.in.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

/**
 * 비회원 조회자를 구분하는 쿠키.
 *
 * <p>board는 세션을 쓰지 않으므로(COM-NFR-020) 세션 식별자 대신 이 쿠키로 같은 브라우저의 중복 조회를 구분한다 (PST-FR-003). 값은 무작위
 * UUID이고, 형식이 틀린 값은 버리고 새로 발급해서 임의의 문자열이 조회수 판정 키에 들어가지 않게 한다.
 */
final class ViewerCookie {

  static final String NAME = "BBS_VIEWER";

  /** 중복 판정 기간(24시간)보다 길게 두어, 브라우저를 다시 열어도 같은 조회자로 본다. */
  private static final Duration MAX_AGE = Duration.ofDays(365);

  private ViewerCookie() {}

  /**
   * 요청의 조회자 식별자를 돌려준다. 없거나 형식이 틀리면 새로 만들어 응답에 쿠키로 싣는다.
   *
   * @param request 요청
   * @param response 응답
   * @return 조회자 식별자
   */
  static String resolve(HttpServletRequest request, HttpServletResponse response) {
    return existing(request).orElseGet(() -> issue(response));
  }

  private static Optional<String> existing(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return Optional.empty();
    }
    return Arrays.stream(cookies)
        .filter(cookie -> NAME.equals(cookie.getName()))
        .map(Cookie::getValue)
        .filter(ViewerCookie::isUuid)
        .findFirst();
  }

  private static boolean isUuid(String value) {
    try {
      return UUID.fromString(value).toString().equals(value);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private static String issue(HttpServletResponse response) {
    String viewer = UUID.randomUUID().toString();
    ResponseCookie cookie =
        ResponseCookie.from(NAME, viewer)
            .httpOnly(true)
            .path("/")
            .sameSite("Lax")
            .maxAge(MAX_AGE)
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    return viewer;
  }
}
