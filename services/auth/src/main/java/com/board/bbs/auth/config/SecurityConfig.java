package com.board.bbs.auth.config;

import com.board.bbs.auth.error.AuthErrorCode;
import com.board.bbs.auth.error.ProblemResponses;
import com.board.bbs.auth.token.JwksController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** 인증·인가 규칙. */
@Configuration
public class SecurityConfig {

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
}
