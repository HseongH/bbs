package com.board.bbs.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** auth가 발급한 토큰을 board가 쓰는 검증기로 검증하는 계약 테스트. 두 서비스 사이의 약속이 깨지면 여기서 실패한다. */
class InternalTokenContractTest {

  private static final InternalUser USER =
      new InternalUser("sub-1", "닉네임", "a@example.com", Set.of("USER", "ADMIN"));

  private static RSAKey key;
  private static JwtDecoder decoder;

  @BeforeAll
  static void 키를_만든다() throws JOSEException {
    key = new RSAKeyGenerator(2048).keyID("test").generate();
    decoder = InternalTokenDecoders.create(keys(key));
  }

  private static JWKSource<SecurityContext> keys(RSAKey rsaKey) {
    return new ImmutableJWKSet<>(new JWKSet(rsaKey));
  }

  private static InternalTokenIssuer issuerAt(RSAKey rsaKey, Instant now) {
    return new InternalTokenIssuer(keys(rsaKey), Clock.fixed(now, ZoneOffset.UTC));
  }

  /** 검증기의 시각 검사가 실제 현재 시각을 쓰므로, 유효한 토큰은 지금 시각으로 발급한다. */
  private static String validToken() {
    return new InternalTokenIssuer(keys(key), Clock.systemUTC()).issue(USER);
  }

  @Test
  void 발급한_토큰을_검증하면_같은_사용자가_나온다() {
    Jwt jwt = decoder.decode(validToken());

    assertThat(InternalUser.from(jwt)).isEqualTo(USER);
    assertThat(jwt.getClaimAsString("iss")).isEqualTo("urn:bbs:auth");
    assertThat(jwt.getAudience()).containsExactly("bbs");
    assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()))
        .isEqualTo(Duration.ofSeconds(60));
    assertThat(jwt.getHeaders()).containsEntry("alg", "RS256").containsEntry("kid", "test");
  }

  @Test
  void 다른_키로_서명한_토큰은_거부한다() throws JOSEException {
    RSAKey other = new RSAKeyGenerator(2048).keyID("test").generate();
    String token = new InternalTokenIssuer(keys(other), Clock.systemUTC()).issue(USER);

    assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
  }

  @Test
  void 만료된_토큰은_거부한다() {
    String token = issuerAt(key, Instant.now().minus(Duration.ofMinutes(10))).issue(USER);

    assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
  }

  @Test
  void 만료된_지_얼마_안_된_토큰도_거부한다() {
    // 수명은 60초다. 시각 오차 허용 때문에 만료 뒤에도 오래 받아들이면 안 된다.
    String token = issuerAt(key, Instant.now().minus(Duration.ofSeconds(90))).issue(USER);

    assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
  }

  @Test
  void 만료_시각이_없는_토큰은_거부한다() {
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer("urn:bbs:auth")
            .audience(List.of("bbs"))
            .subject("sub-1")
            .issuedAt(now)
            .build();

    assertThatThrownBy(() -> decoder.decode(sign(claims))).isInstanceOf(JwtException.class);
  }

  @Test
  void 발급자가_다른_토큰은_거부한다() {
    String token = sign(claims().issuer("urn:other").audience(List.of("bbs")).build());

    assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
  }

  @Test
  void 대상이_다른_토큰은_거부한다() {
    String token = sign(claims().issuer("urn:bbs:auth").audience(List.of("other")).build());

    assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
  }

  @Test
  void 역할_클레임이_없거나_목록이_아니면_역할이_없다() {
    Jwt withoutRoles =
        decoder.decode(sign(claims().issuer("urn:bbs:auth").audience(List.of("bbs")).build()));
    Jwt stringRoles =
        decoder.decode(
            sign(
                claims()
                    .issuer("urn:bbs:auth")
                    .audience(List.of("bbs"))
                    .claim("roles", "ADMIN")
                    .build()));

    assertThat(InternalUser.from(withoutRoles).roles()).isEmpty();
    assertThat(InternalUser.from(stringRoles).roles()).isEmpty();
  }

  private static JwtClaimsSet.Builder claims() {
    Instant now = Instant.now();
    return JwtClaimsSet.builder()
        .subject("sub-1")
        .issuedAt(now)
        .expiresAt(now.plusSeconds(60))
        .claim("nickname", "닉네임")
        .claim("email", "a@example.com");
  }

  private static String sign(JwtClaimsSet claims) {
    return new NimbusJwtEncoder(keys(key))
        .encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
        .getTokenValue();
  }
}
