package com.board.bbs.token;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.time.Duration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** 업무 서비스가 내부 토큰을 검증하는 검증기를 만든다. */
public final class InternalTokenDecoders {

  /** 시계 차이로 허용하는 시간. auth와 업무 서비스의 시계가 조금 어긋나도 막 발급한 토큰을 거부하지 않게 한다. */
  private static final Duration CLOCK_SKEW = Duration.ofSeconds(5);

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
            timestamps(),
            new JwtIssuerValidator(InternalTokens.ISSUER),
            InternalTokenDecoders::audience));
    return decoder;
  }

  /**
   * 만료 시각이 반드시 있어야 하고, 시각 오차는 짧게만 허용한다. 기본값(60초)이면 수명 60초인 토큰을 최대 120초 동안 받아들이게 된다 (COM-NFR-008).
   */
  private static JwtTimestampValidator timestamps() {
    JwtTimestampValidator validator = new JwtTimestampValidator(CLOCK_SKEW);
    validator.setAllowEmptyExpiryClaim(false);
    return validator;
  }

  private static OAuth2TokenValidatorResult audience(Jwt jwt) {
    if (jwt.getAudience() != null && jwt.getAudience().contains(InternalTokens.AUDIENCE)) {
      return OAuth2TokenValidatorResult.success();
    }
    return OAuth2TokenValidatorResult.failure(
        new OAuth2Error("invalid_token", "내부 토큰의 대상이 아닙니다.", null));
  }
}
