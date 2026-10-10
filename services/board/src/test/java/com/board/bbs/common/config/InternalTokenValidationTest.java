package com.board.bbs.common.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.board.bbs.support.IntegrationTestBase;
import com.board.bbs.support.TestInternalTokens;
import com.board.bbs.token.InternalTokenIssuer;
import com.board.bbs.token.InternalUser;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** board는 auth가 서명한 내부 토큰만 믿는다 (COM-NFR-007). 검증에 실패한 토큰은 공개 조회에서도 거부한다. */
@AutoConfigureMockMvc
class InternalTokenValidationTest extends IntegrationTestBase {

  private static final InternalUser USER =
      new InternalUser("sub-token", "토큰", "token@example.com", Set.of("USER"));

  @Autowired private MockMvc mockMvc;

  private ResultActions 내_정보를_조회한다(String token) throws Exception {
    return mockMvc.perform(get("/api/members/me").with(TestInternalTokens.withToken(token)));
  }

  private static void 거부된다(ResultActions result) throws Exception {
    result
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void 다른_키로_서명한_토큰은_401이다() throws Exception {
    RSAKey other = new RSAKeyGenerator(2048).keyID("board-test").generate();
    String token =
        new InternalTokenIssuer(new ImmutableJWKSet<>(new JWKSet(other)), Clock.systemUTC())
            .issue(USER);

    거부된다(내_정보를_조회한다(token));
  }

  @Test
  void 만료된_토큰은_401이다() throws Exception {
    String token =
        new InternalTokenIssuer(
                new ImmutableJWKSet<>(new JWKSet(TestInternalTokens.KEY)),
                Clock.fixed(Instant.now().minus(Duration.ofMinutes(10)), ZoneOffset.UTC))
            .issue(USER);

    거부된다(내_정보를_조회한다(token));
  }

  @Test
  void 발급자나_대상이_다른_토큰은_401이다() throws Exception {
    거부된다(내_정보를_조회한다(sign("urn:other", "bbs")));
    거부된다(내_정보를_조회한다(sign("urn:bbs:auth", "other")));
  }

  @Test
  void 공개_조회에_잘못된_토큰이_오면_401이다() throws Exception {
    거부된다(mockMvc.perform(get("/api/posts").with(TestInternalTokens.withToken("forged"))));
  }

  private static String sign(String issuer, String audience) {
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(issuer)
            .audience(List.of(audience))
            .subject(USER.subject())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(60))
            .claim("nickname", USER.nickname())
            .claim("email", USER.email())
            .build();
    return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(TestInternalTokens.KEY)))
        .encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
        .getTokenValue();
  }
}
