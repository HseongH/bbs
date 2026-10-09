package com.board.bbs.auth.config;

import static com.board.bbs.auth.forward.ForwardedRequestMatcher.forwarded;

import com.board.bbs.auth.error.AuthErrorCode;
import com.board.bbs.auth.error.ProblemResponses;
import com.board.bbs.auth.forward.ForwardAuthController;
import com.board.bbs.auth.forward.ForwardedRequest;
import com.board.bbs.auth.forward.ForwardedRequestFilter;
import com.board.bbs.auth.token.JwksController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.savedrequest.NullRequestCache;

/** 인증·인가 규칙. */
@Configuration
public class SecurityConfig {

  /** 상태를 바꾸지 않는 메서드. 원래 요청이 이 메서드면 CSRF 토큰을 요구하지 않는다. */
  private static final Set<HttpMethod> SAFE_METHODS =
      Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.TRACE, HttpMethod.OPTIONS);

  private final OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService;

  SecurityConfig(OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService) {
    this.oidcUserService = oidcUserService;
  }

  /**
   * 보안 필터 체인을 구성한다.
   *
   * @param http 보안 설정 빌더
   * @return 보안 필터 체인
   * @throws Exception 설정에 실패한 경우
   */
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/health", "/actuator/info", JwksController.PATH)
                    .permitAll()
                    .requestMatchers("/actuator/**")
                    .hasRole("ADMIN")
                    // 진입점이 전달한 원래 요청의 규칙. 위에서부터 처음 일치하는 것이 적용된다 (PRJ-SDS §7.3).
                    .requestMatchers(
                        forwarded(HttpMethod.GET, "/api/posts/**"),
                        forwarded(HttpMethod.GET, "/api/comments/**"))
                    .permitAll()
                    .requestMatchers(forwarded(null, "/api/**"))
                    .authenticated()
                    .requestMatchers(ForwardAuthController.PATH)
                    .denyAll()
                    .anyRequest()
                    .authenticated())
        .oauth2Login(login -> login.userInfoEndpoint(ui -> ui.oidcUserService(oidcUserService)))
        // 화면이 SPA이므로 로그아웃 후 로그인 페이지로 보내는 대신 상태 코드만 돌려준다.
        .logout(
            logout ->
                logout.logoutSuccessHandler(
                    (request, response, authentication) ->
                        response.setStatus(HttpServletResponse.SC_NO_CONTENT)))
        .csrf(
            csrf ->
                csrf.csrfTokenRepository(cookieCsrfTokenRepository())
                    .csrfTokenRequestHandler(eagerCsrfTokenHandler())
                    .requireCsrfProtectionMatcher(SecurityConfig::requiresCsrfProtection))
        // 모호한 원래 요청은 인증·CSRF 판정보다 먼저 거부한다.
        .addFilterBefore(new ForwardedRequestFilter(), CsrfFilter.class)
        // 미인증 API 요청마다 세션을 만들어 요청을 저장하지 않는다. 로그인 후 돌아갈 경로는 화면이 기억한다 (MEM-FR-021).
        .requestCache(cache -> cache.requestCache(new NullRequestCache()))
        // 화면이 SPA이므로 로그인 페이지로 보내지 않고 상태 코드와 ProblemDetail만 돌려준다.
        .exceptionHandling(
            handling ->
                handling
                    .authenticationEntryPoint(
                        (request, response, exception) ->
                            ProblemResponses.write(
                                response,
                                AuthErrorCode.UNAUTHENTICATED,
                                ForwardedRequest.instancePathOf(request)))
                    .accessDeniedHandler(
                        (request, response, exception) ->
                            ProblemResponses.write(
                                response,
                                AuthErrorCode.ACCESS_DENIED,
                                ForwardedRequest.instancePathOf(request))))
        .build();
  }

  /** CSRF 토큰이 필요한 요청인지 판정한다. ForwardAuth 요청 자체는 진입점이 보내는 조회 요청이므로, 원래 요청의 메서드로 판단한다. */
  private static boolean requiresCsrfProtection(HttpServletRequest request) {
    if (ForwardAuthController.PATH.equals(request.getRequestURI())) {
      return ForwardedRequest.tryFrom(request)
          .map(forwarded -> !SAFE_METHODS.contains(forwarded.method()))
          .orElse(true);
    }
    return CsrfFilter.DEFAULT_CSRF_MATCHER.matches(request);
  }

  /** 브라우저가 읽을 수 있어야 요청 헤더에 실어 보낼 수 있으므로 HttpOnly를 끈다. */
  private static CookieCsrfTokenRepository cookieCsrfTokenRepository() {
    return CookieCsrfTokenRepository.withHttpOnlyFalse();
  }

  /**
   * 토큰을 지연 로딩하지 않고 바로 발급한다.
   *
   * <p>기본 동작은 토큰이 실제로 조회될 때까지 쿠키를 내려주지 않아, 화면이 첫 변경 요청에 쓸 토큰을 갖지 못한다. 요청 속성 이름을 비우면 지연 로딩을 끄게 되어 모든
   * 응답에 쿠키가 실린다.
   */
  private static CsrfTokenRequestAttributeHandler eagerCsrfTokenHandler() {
    CsrfTokenRequestAttributeHandler handler = new CsrfTokenRequestAttributeHandler();
    handler.setCsrfRequestAttributeName(null);
    return handler;
  }
}
