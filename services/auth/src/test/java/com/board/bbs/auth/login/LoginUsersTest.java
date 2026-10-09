package com.board.bbs.auth.login;

import static org.assertj.core.api.Assertions.assertThat;

import com.board.bbs.token.InternalUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class LoginUsersTest {

  private static OidcUser 사용자(String preferredUsername, String email) {
    OidcIdToken token =
        OidcIdToken.withTokenValue("token")
            .subject("sub-1")
            .claim("preferred_username", preferredUsername)
            .claim("email", email)
            .build();
    return new DefaultOidcUser(List.of(), token, "sub");
  }

  @Test
  void 사용자_이름과_이메일을_그대로_쓴다() {
    InternalUser user = LoginUsers.toInternalUser(사용자("홍길동", "hong@example.com"));

    assertThat(user.subject()).isEqualTo("sub-1");
    assertThat(user.nickname()).isEqualTo("홍길동");
    assertThat(user.email()).isEqualTo("hong@example.com");
  }

  @Test
  void 사용자_이름이나_이메일이_없으면_subject로_대신한다() {
    OidcUser oidcUser =
        new DefaultOidcUser(
            List.of(), OidcIdToken.withTokenValue("token").subject("sub-1").build(), "sub");

    InternalUser user = LoginUsers.toInternalUser(oidcUser);

    assertThat(user.nickname()).isEqualTo("sub-1");
    assertThat(user.email()).isEqualTo("sub-1@unknown.local");
  }

  @Test
  void 사용자_이름이나_이메일이_공백뿐이면_subject로_대신한다() {
    InternalUser user = LoginUsers.toInternalUser(사용자("   ", " "));

    assertThat(user.nickname()).isEqualTo("sub-1");
    assertThat(user.email()).isEqualTo("sub-1@unknown.local");
  }

  @Test
  void 역할_권한만_역할로_옮긴다() {
    List<GrantedAuthority> authorities =
        List.of(
            new SimpleGrantedAuthority("OIDC_USER"),
            new SimpleGrantedAuthority("SCOPE_openid"),
            new SimpleGrantedAuthority("ROLE_USER"),
            new SimpleGrantedAuthority("ROLE_ADMIN"));
    OidcUser oidcUser =
        new DefaultOidcUser(
            authorities, OidcIdToken.withTokenValue("token").subject("sub-1").build(), "sub");

    assertThat(LoginUsers.toInternalUser(oidcUser).roles())
        .containsExactlyInAnyOrder("USER", "ADMIN");
  }
}
