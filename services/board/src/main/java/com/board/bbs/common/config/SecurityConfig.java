package com.board.bbs.common.config;

import com.board.bbs.common.error.ErrorCode;
import com.board.bbs.common.error.ProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 인증·인가 규칙.
 *
 * <p>board는 auth가 서명한 내부 토큰만 검증한다 (ADR-0016). 로그인, 세션, CSRF는 auth의 일이다. 인증이 필요한지는 auth가 원래 요청 기준으로
 * 판정하므로 여기서 같은 규칙을 반복하지 않는다. 진입점을 우회한 요청은 현재 회원이 필요한 API가 {@code @CurrentMember}로 거부하고, 위조된 토큰은 서명
 * 검증에서 거부된다 (PRJ-SDS §7.3).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  /** 필터 단계에는 MVC의 메시지 변환이 없으므로, 같은 변환기로 직접 직렬화해서 문자열 조립을 피한다. */
  private static final JacksonJsonHttpMessageConverter PROBLEM_WRITER =
      new JacksonJsonHttpMessageConverter();

  private final Converter<Jwt, ? extends AbstractAuthenticationToken> authenticationConverter;

  SecurityConfig(Converter<Jwt, ? extends AbstractAuthenticationToken> authenticationConverter) {
    this.authenticationConverter = authenticationConverter;
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
                auth.requestMatchers("/actuator/health", "/actuator/info")
                    .permitAll()
                    // 나머지 액추에이터는 내부 운영 정보다.
                    .requestMatchers("/actuator/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .permitAll())
        .oauth2ResourceServer(
            resourceServer ->
                resourceServer
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter))
                    .authenticationEntryPoint(
                        (request, response, exception) ->
                            writeProblem(request, response, ErrorCode.UNAUTHENTICATED))
                    .accessDeniedHandler(
                        (request, response, exception) ->
                            writeProblem(request, response, ErrorCode.ACCESS_DENIED)))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // 세션 쿠키를 쓰지 않으므로 CSRF 공격 대상이 아니다. 브라우저 쪽 CSRF는 auth가 판정한다.
        .csrf(AbstractHttpConfigurer::disable)
        .exceptionHandling(
            handling ->
                handling
                    .authenticationEntryPoint(
                        (request, response, exception) ->
                            writeProblem(request, response, ErrorCode.UNAUTHENTICATED))
                    .accessDeniedHandler(
                        (request, response, exception) ->
                            writeProblem(request, response, ErrorCode.ACCESS_DENIED)))
        .build();
  }

  /** API 클라이언트에게 로그인 페이지로의 리다이렉트는 의미가 없으므로 ProblemDetail을 직접 쓴다. */
  private static void writeProblem(
      HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode)
      throws IOException {

    response.setStatus(errorCode.getStatus().value());
    PROBLEM_WRITER.write(
        ProblemDetails.of(errorCode, request.getRequestURI()),
        MediaType.APPLICATION_PROBLEM_JSON,
        new ServletServerHttpResponse(response));
  }
}
