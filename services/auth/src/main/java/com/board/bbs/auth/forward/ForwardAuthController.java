package com.board.bbs.auth.forward;

import com.board.bbs.auth.login.LoginUsers;
import com.board.bbs.token.InternalTokenIssuer;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 진입점의 ForwardAuth 대상. 이 메서드까지 왔다면 보안 설정의 판정을 통과한 것이다.
 *
 * <p>로그인한 사용자면 내부 토큰을 붙이고, 비로그인 사용자의 공개 요청이면 토큰 없이 통과시킨다. 진입점은 이 응답의 {@code Authorization}만 업무 서비스에
 * 넘긴다.
 */
@RestController
public class ForwardAuthController {

  /** ForwardAuth 경로. 진입점이 원래 요청의 메서드와 무관하게 호출할 수 있으므로 모든 메서드를 받는다. */
  public static final String PATH = "/forward-auth";

  private static final String BEARER = "Bearer ";

  private final InternalTokenIssuer issuer;

  ForwardAuthController(InternalTokenIssuer issuer) {
    this.issuer = issuer;
  }

  @RequestMapping(PATH)
  ResponseEntity<Void> check(@AuthenticationPrincipal @Nullable OidcUser user) {
    if (user == null) {
      return ResponseEntity.ok().build();
    }
    String token = issuer.issue(LoginUsers.toInternalUser(user));
    return ResponseEntity.ok().header(HttpHeaders.AUTHORIZATION, BEARER + token).build();
  }
}
