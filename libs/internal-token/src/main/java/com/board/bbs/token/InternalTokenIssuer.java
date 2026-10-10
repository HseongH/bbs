package com.board.bbs.token;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** 내부 토큰을 서명한다. auth만 사용한다. */
public final class InternalTokenIssuer {

  private final JwtEncoder encoder;
  private final Clock clock;

  /**
   * 발급기를 만든다.
   *
   * @param keys 서명 키. 키 식별자(kid)가 토큰 헤더에 실린다
   * @param clock 발급 시각의 기준
   */
  public InternalTokenIssuer(JWKSource<SecurityContext> keys, Clock clock) {
    this.encoder = new NimbusJwtEncoder(keys);
    this.clock = clock;
  }

  /**
   * 사용자를 담은 토큰을 발급한다.
   *
   * @param user 토큰에 담을 사용자
   * @return 서명된 토큰 문자열
   */
  public String issue(InternalUser user) {
    Instant now = clock.instant();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(InternalTokens.ISSUER)
            .audience(List.of(InternalTokens.AUDIENCE))
            .subject(user.subject())
            .issuedAt(now)
            .expiresAt(now.plus(InternalTokens.LIFETIME))
            .claim(InternalTokens.NICKNAME, user.nickname())
            .claim(InternalTokens.EMAIL, user.email())
            .claim(InternalTokens.ROLES, user.roles().stream().sorted().toList())
            .build();
    JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
    return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }
}
