package com.board.bbs.token;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** 업무 서비스가 내부 토큰을 검증하는 검증기를 만든다. */
public final class InternalTokenDecoders {

  private InternalTokenDecoders() {}

  /**
   * RS256 서명, 만료, 발급자, 대상을 검사하는 검증기를 만든다.
   *
   * @param keys auth의 공개키
   * @return 검증기
   */
  public static JwtDecoder create(JWKSource<SecurityContext> keys) {
    DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
    processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keys));
    // 시각과 대상은 아래의 스프링 검증기가 검사한다.
    processor.setJWTClaimsSetVerifier((claims, context) -> {});

    NimbusJwtDecoder decoder = new NimbusJwtDecoder(processor);
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(InternalTokens.ISSUER),
            InternalTokenDecoders::audience));
    return decoder;
  }

  private static OAuth2TokenValidatorResult audience(Jwt jwt) {
    if (jwt.getAudience() != null && jwt.getAudience().contains(InternalTokens.AUDIENCE)) {
      return OAuth2TokenValidatorResult.success();
    }
    return OAuth2TokenValidatorResult.failure(
        new OAuth2Error("invalid_token", "내부 토큰의 대상이 아닙니다.", null));
  }
}
