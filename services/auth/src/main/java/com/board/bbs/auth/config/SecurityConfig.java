package com.board.bbs.auth.config;

import com.board.bbs.auth.error.AuthErrorCode;
import com.board.bbs.auth.error.ProblemResponses;
import com.board.bbs.auth.token.JwksController;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/** 인증·인가 규칙. */
@Configuration
public class SecurityConfig {

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
                    .csrfTokenRequestHandler(eagerCsrfTokenHandler()))
        // 화면이 SPA이므로 로그인 페이지로 보내지 않고 상태 코드와 ProblemDetail만 돌려준다.
        .exceptionHandling(
            handling ->
                handling
                    .authenticationEntryPoint(
                        (request, response, exception) ->
                            ProblemResponses.write(
                                response, AuthErrorCode.UNAUTHENTICATED, request.getRequestURI()))
                    .accessDeniedHandler(
                        (request, response, exception) ->
                            ProblemResponses.write(
                                response, AuthErrorCode.ACCESS_DENIED, request.getRequestURI())))
        .build();
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
