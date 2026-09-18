package com.board.bbs.common.config;

import com.board.bbs.common.error.ErrorCode;
import com.board.bbs.common.security.BbsOidcUserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

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
                    .anyRequest()
                    .authenticated())
        .oauth2Login(login -> login.userInfoEndpoint(ui -> ui.oidcUserService(oidcUserService)))
        .logout(Customizer.withDefaults())
        .exceptionHandling(
            handling -> handling.authenticationEntryPoint(SecurityConfig::writeUnauthenticated))
        .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
        .build();
  }

  /** API 클라이언트에게 로그인 페이지로의 리다이렉트는 의미가 없으므로 ProblemDetail을 직접 쓴다. */
  private static void writeUnauthenticated(
      jakarta.servlet.http.HttpServletRequest request,
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
