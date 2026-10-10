package com.board.bbs.member.adapter.in.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class InternalTokenAuthenticationConverterTest {

  private final InternalTokenAuthenticationConverter converter =
      new InternalTokenAuthenticationConverter();

  private static Jwt.Builder 토큰() {
    return Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject("sub-1")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60));
  }

  @Test
  void 역할은_ROLE_접두사_권한이_된다() {
    Jwt jwt = 토큰().claim("roles", List.of("USER", "ADMIN")).build();

    assertThat(converter.convert(jwt).getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
  }

  @Test
  void 역할_클레임이_없으면_권한이_없다() {
    assertThat(converter.convert(토큰().build()).getAuthorities()).isEmpty();
    assertThat(converter.convert(토큰().claim("roles", "ADMIN").build()).getAuthorities()).isEmpty();
  }

  @Test
  void 인증_주체는_토큰이다() {
    Jwt jwt = 토큰().build();

    assertThat(converter.convert(jwt).getPrincipal()).isEqualTo(jwt);
  }
}
