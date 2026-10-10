package com.board.bbs.support;

import com.board.bbs.token.InternalTokenIssuer;
import com.board.bbs.token.InternalUser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.time.Clock;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 테스트용 내부 토큰.
 *
 * <p>auth가 쓰는 것과 같은 발급기({@link InternalTokenIssuer})로 실제 서명한 토큰을 만든다. board는 {@link
 * TestInternalTokenKeys}가 제공하는 이 키의 공개키로 검증하므로, 테스트는 auth 없이 실제 검증 경로를 거친다.
 */
public final class TestInternalTokens {

  /** 테스트 서명 키. */
  public static final RSAKey KEY = generate();

  private static final InternalTokenIssuer ISSUER =
      new InternalTokenIssuer(new ImmutableJWKSet<>(new JWKSet(KEY)), Clock.systemUTC());

  private TestInternalTokens() {}

  /**
   * 사용자를 담은 토큰을 발급한다.
   *
   * @param user 사용자
   * @return 서명된 토큰
   */
  public static String issue(InternalUser user) {
    return ISSUER.issue(user);
  }

  /**
   * 내부 토큰을 실은 요청을 만든다. 닉네임은 subject, 이메일은 {@code subject@example.com}이다.
   *
   * @param subject 외부 사용자 식별자
   * @param roles 역할 (예: {@code "ADMIN"})
   * @return 요청 후처리기
   */
  public static RequestPostProcessor bearer(String subject, String... roles) {
    return withToken(
        issue(new InternalUser(subject, subject, subject + "@example.com", Set.of(roles))));
  }

  /**
   * 주어진 토큰을 실은 요청을 만든다.
   *
   * @param token 토큰
   * @return 요청 후처리기
   */
  public static RequestPostProcessor withToken(String token) {
    return request -> {
      request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
      return request;
    };
  }

  private static RSAKey generate() {
    try {
      return new RSAKeyGenerator(2048).keyID("board-test").generate();
    } catch (JOSEException e) {
      throw new IllegalStateException(e);
    }
  }
}
