package com.board.bbs.auth.forward;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;

class ForwardedRequestTest {

  private static MockHttpServletRequest 전달(String method, String uri) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/forward-auth");
    request.addHeader("X-Forwarded-Method", method);
    request.addHeader("X-Forwarded-Uri", uri);
    return request;
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/api/posts/%2e%2e/members/me",
        "/api/posts%2F1",
        "/api/posts;x=1",
        "/api/posts/..%5C",
        "/api/posts/../members/me",
        "/api/posts/./1",
        "/api/posts/%252e%252e",
        "/api/posts\\1",
        "/api/posts/%00",
        "/api//posts",
        "api/posts",
        "/api/posts/%E0%A4",
        "/api/posts%3B/1",
        "/api/posts%3b/1"
      })
  void 모호한_경로는_거부한다(String uri) {
    assertThatThrownBy(() -> ForwardedRequest.from(전달("GET", uri)))
        .isInstanceOf(AmbiguousForwardedRequestException.class);
  }

  @ParameterizedTest
  @CsvSource({
    "/api/posts?keyword=%ED%95%9C%EA%B8%80, /api/posts",
    "/api/posts?keyword=a/../b, /api/posts",
    "/api/posts/1/comments, /api/posts/1/comments",
    "/api/posts/%ED%95%9C, /api/posts/한"
  })
  void 쿼리와_정상_경로는_허용하고_쿼리를_뺀_경로를_돌려준다(String uri, String path) {
    ForwardedRequest request = ForwardedRequest.from(전달("POST", uri));

    assertThat(request.method()).isEqualTo(HttpMethod.POST);
    assertThat(request.path()).isEqualTo(path);
  }

  @ParameterizedTest
  @ValueSource(strings = {"get", "Get", " GET", "FOO", "CONNECT"})
  void 표준_대문자_메서드가_아니면_거부한다(String method) {
    // board는 메서드를 받은 그대로 처리한다. auth가 대소문자를 고쳐서 판정하면 서로 다른 요청을 보게 된다.
    assertThatThrownBy(() -> ForwardedRequest.from(전달(method, "/api/posts")))
        .isInstanceOf(AmbiguousForwardedRequestException.class);
  }

  @Test
  void 전달_헤더가_없으면_거부한다() {
    MockHttpServletRequest withoutUri = new MockHttpServletRequest("GET", "/forward-auth");
    withoutUri.addHeader("X-Forwarded-Method", "GET");
    MockHttpServletRequest withoutMethod = new MockHttpServletRequest("GET", "/forward-auth");
    withoutMethod.addHeader("X-Forwarded-Uri", "/api/posts");

    assertThatThrownBy(() -> ForwardedRequest.from(withoutUri))
        .isInstanceOf(AmbiguousForwardedRequestException.class);
    assertThatThrownBy(() -> ForwardedRequest.from(withoutMethod))
        .isInstanceOf(AmbiguousForwardedRequestException.class);
  }

  @Test
  void 오류_응답의_경로는_원래_경로에서_쿼리를_뺀_값이다() {
    assertThat(ForwardedRequest.instancePathOf(전달("POST", "/api/posts/\"x\"?a=1")))
        .isEqualTo("/api/posts/\"x\"");
    assertThat(ForwardedRequest.instancePathOf(new MockHttpServletRequest("GET", "/logout")))
        .isEqualTo("/logout");
  }
}
