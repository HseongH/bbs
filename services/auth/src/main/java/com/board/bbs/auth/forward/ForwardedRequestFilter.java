package com.board.bbs.auth.forward;

import com.board.bbs.auth.error.AuthErrorCode;
import com.board.bbs.auth.error.ProblemResponses;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * ForwardAuth 요청 중 판정할 수 없는 것을 인증·CSRF 검사보다 먼저 거부한다.
 *
 * <ul>
 *   <li>원래 요청을 복원할 수 없거나 경로가 모호하면 {@code 400}.
 *   <li>원래 경로가 {@code /api}가 아니면 {@code 403}. 진입점은 {@code /api}에만 ForwardAuth를 건다. 다른 경로가 오면 진입점 설정
 *       오류이므로, 로그인 여부와 관계없이 열지 않는다.
 * </ul>
 */
public class ForwardedRequestFilter extends OncePerRequestFilter {

  private static final String API_PREFIX = "/api/";

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !ForwardAuthController.PATH.equals(request.getRequestURI());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Optional<ForwardedRequest> forwarded = ForwardedRequest.tryFrom(request);
    String instancePath = ForwardedRequest.instancePathOf(request);
    if (forwarded.isEmpty()) {
      ProblemResponses.write(response, AuthErrorCode.INVALID_REQUEST, instancePath);
      return;
    }
    String path = forwarded.get().path();
    if (!path.equals("/api") && !path.startsWith(API_PREFIX)) {
      ProblemResponses.write(response, AuthErrorCode.ACCESS_DENIED, instancePath);
      return;
    }
    chain.doFilter(request, response);
  }
}
