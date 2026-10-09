package com.board.bbs.auth.login;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class BbsOidcUserServiceTest {

  @Test
  void realm_역할_중_bbs가_정의한_역할만_권한이_된다() {
    OidcIdToken token =
        OidcIdToken.withTokenValue("token")
            .subject("sub-1")
            .claim("preferred_username", "tester")
            .claim(
                "realm_access",
                Map.of("roles", List.of("USER", "ADMIN", "offline_access", "default-roles-bbs")))
            .build();
    OidcUser loaded =
        new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), token, "sub");

    OidcUser user = BbsOidcUserService.withBbsRoles(loaded);

    assertThat(user.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("OIDC_USER", "ROLE_USER", "ROLE_ADMIN");
  }

  @Test
  void preferred_username이_없어도_로그인할_수_있다() {
    OidcIdToken token = OidcIdToken.withTokenValue("token").subject("sub-1").build();
    OidcUser loaded = new DefaultOidcUser(List.of(), token, "sub");

    OidcUser user = BbsOidcUserService.withBbsRoles(loaded);

    assertThat(user.getName()).isEqualTo("sub-1");
    assertThat(user.getAuthorities()).isEmpty();
  }
}
