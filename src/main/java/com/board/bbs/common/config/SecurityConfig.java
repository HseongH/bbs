package com.board.bbs.common.config;

import com.board.bbs.common.error.ErrorCode;
import com.board.bbs.common.security.BbsOidcUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/** 인증·인가 규칙. */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final BbsOidcUserService oidcUserService;

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
                auth.requestMatchers("/actuator/health", "/actuator/info")
                    .permitAll()
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/posts/**", "/api/comments/**")
                    .permitAll()
                    .requestMatchers("/api/**")
                    .authenticated()
                    .requestMatchers(HttpMethod.GET, "/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2Login(login -> login.userInfoEndpoint(ui -> ui.oidcUserService(oidcUserService)))
        // 화면이 SPA이므로 로그아웃 후 로그인 페이지로 보내는 대신 상태 코드만 돌려준다.
        .logout(
            logout ->
                logout.logoutSuccessHandler(
                    (request, response, authentication) ->
                        response.setStatus(HttpServletResponse.SC_NO_CONTENT)))
        .exceptionHandling(
            handling -> handling.authenticationEntryPoint(SecurityConfig::writeUnauthenticated))
        .csrf(
            csrf ->
                csrf.csrfTokenRepository(cookieCsrfTokenRepository())
                    .csrfTokenRequestHandler(eagerCsrfTokenHandler()))
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

  /** API 클라이언트에게 로그인 페이지로의 리다이렉트는 의미가 없으므로 ProblemDetail을 직접 쓴다. */
  private static void writeUnauthenticated(
      HttpServletRequest request,
      HttpServletResponse response,
      org.springframework.security.core.AuthenticationException exception)
      throws java.io.IOException {

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    String body =
        ("{\"type\":\"urn:bbs:error:unauthenticated\",\"title\":\"Unauthorized\","
                + "\"status\":401,\"detail\":\"%s\",\"instance\":\"%s\",\"code\":\"%s\"}")
            .formatted(
                ErrorCode.UNAUTHENTICATED.getDefaultMessage(),
                request.getRequestURI(),
                ErrorCode.UNAUTHENTICATED.name());
    response.getWriter().write(body);
  }
}
