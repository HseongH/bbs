package com.board.bbs.auth.forward;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.board.bbs.auth.support.IntegrationTestBase;
import com.board.bbs.token.InternalTokenDecoders;
import com.board.bbs.token.InternalUser;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@AutoConfigureMockMvc
class ForwardAuthTest extends IntegrationTestBase {

  private static final String CSRF = "csrf-token";

  @Autowired private MockMvc mockMvc;
  @Autowired private JWKSource<SecurityContext> signingKeys;

  private static MockHttpServletRequestBuilder 전달(String method, String uri) {
    return get("/forward-auth").header("X-Forwarded-Method", method).header("X-Forwarded-Uri", uri);
  }

  private static MockHttpServletRequestBuilder CSRF_토큰과_함께(MockHttpServletRequestBuilder request) {
    return request.cookie(new Cookie("XSRF-TOKEN", CSRF)).header("X-XSRF-TOKEN", CSRF);
  }

  private static OidcLoginRequestPostProcessor 로그인() {
    return oidcLogin()
        .idToken(
            token ->
                token
                    .subject("sub-1")
                    .claim("preferred_username", "tester")
                    .claim("email", "tester@example.com"))
        .authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }

  private Jwt 검증한다(String authorization) {
    assertThat(authorization).startsWith("Bearer ");
    return InternalTokenDecoders.create(signingKeys).decode(authorization.substring(7));
  }

  @Test
  void 비로그인_공개_조회는_토큰_없이_통과한다() throws Exception {
    mockMvc
        .perform(전달("GET", "/api/posts?page=0"))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist(HttpHeaders.AUTHORIZATION));
    mockMvc
        .perform(전달("GET", "/api/comments/1"))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist(HttpHeaders.AUTHORIZATION));
  }

  @Test
  void 로그인한_공개_조회는_내부_토큰을_붙여_통과한다() throws Exception {
    String authorization =
        mockMvc
            .perform(전달("GET", "/api/posts").with(로그인()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getHeader(HttpHeaders.AUTHORIZATION);

    Jwt jwt = 검증한다(Objects.requireNonNull(authorization));
    assertThat(InternalUser.from(jwt))
        .isEqualTo(new InternalUser("sub-1", "tester", "tester@example.com", Set.of("USER")));
    assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()))
        .isEqualTo(Duration.ofSeconds(60));
  }

  @Test
  void 비로그인_쓰기는_401_ProblemDetail이다() throws Exception {
    mockMvc
        .perform(CSRF_토큰과_함께(전달("POST", "/api/posts/\"x\"?a=1")))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
        .andExpect(jsonPath("$.type").value("urn:bbs:error:unauthenticated"))
        .andExpect(jsonPath("$.title").value("Unauthorized"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.detail").value("인증이 필요합니다."))
        .andExpect(jsonPath("$.instance").value("/api/posts/%22x%22"));
  }

  @Test
  void 로그인했어도_CSRF_토큰_없는_쓰기는_403이다() throws Exception {
    mockMvc
        .perform(전달("POST", "/api/posts").with(로그인()))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
        .andExpect(jsonPath("$.type").value("urn:bbs:error:access_denied"))
        .andExpect(jsonPath("$.instance").value("/api/posts"))
        .andExpect(header().doesNotExist(HttpHeaders.AUTHORIZATION));
  }

  @Test
  void CSRF_토큰과_함께_보낸_쓰기는_토큰을_붙여_통과한다() throws Exception {
    for (String method : List.of("POST", "PATCH", "DELETE")) {
      String authorization =
          mockMvc
              .perform(CSRF_토큰과_함께(전달(method, "/api/posts/1")).with(로그인()))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getHeader(HttpHeaders.AUTHORIZATION);
      assertThat(검증한다(Objects.requireNonNull(authorization)).getSubject()).isEqualTo("sub-1");
    }
  }

  @Test
  void 조회는_CSRF_쿠키를_발급한다() throws Exception {
    mockMvc
        .perform(전달("GET", "/api/posts"))
        .andExpect(status().isOk())
        .andExpect(cookie().exists("XSRF-TOKEN"))
        .andExpect(cookie().httpOnly("XSRF-TOKEN", false));
  }

  @Test
  void 세션이_없어진_브라우저의_공개_조회는_200이다() throws Exception {
    mockMvc
        .perform(전달("GET", "/api/posts").cookie(new Cookie("SESSION", "expired-session")))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist(HttpHeaders.AUTHORIZATION));
  }

  @Test
  void api가_아닌_원래_경로는_거부한다() throws Exception {
    for (var request :
        List.of(전달("GET", "/actuator/metrics"), 전달("GET", "/actuator/metrics").with(로그인()))) {
      mockMvc
          .perform(request)
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
          .andExpect(header().doesNotExist(HttpHeaders.AUTHORIZATION));
    }
  }

  @Test
  void 모호한_원래_경로는_400이다() throws Exception {
    mockMvc
        .perform(전달("GET", "/api/posts/%2e%2e/members/me").with(로그인()))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        .andExpect(header().doesNotExist(HttpHeaders.AUTHORIZATION));
  }

  @Test
  void 전달_헤더_없이_직접_호출하면_400이다() throws Exception {
    mockMvc
        .perform(get("/forward-auth").with(로그인()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.instance").value("/forward-auth"))
        .andExpect(header().doesNotExist(HttpHeaders.AUTHORIZATION));
  }

  @Test
  void 전달_요청이_아니면_원래_경로_규칙을_쓰지_않는다() throws Exception {
    // 진입점을 거치지 않은 /api 요청은 auth의 경로가 아니므로 인증을 요구한다.
    mockMvc.perform(get("/api/posts")).andExpect(status().isUnauthorized());
  }
}
