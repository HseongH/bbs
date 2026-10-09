package com.board.bbs.member.adapter.in.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class BbsOidcUserServiceTest {

  private static OidcUser 사용자(String preferredUsername, String email) {
    OidcIdToken token =
        OidcIdToken.withTokenValue("token")
            .subject("sub-1")
            .claim("preferred_username", preferredUsername)
            .claim("email", email)
            .build();
    return new DefaultOidcUser(List.of(), token);
  }

  private static OidcUser 이름과_이메일이_없는_사용자() {
    return new DefaultOidcUser(
        List.of(), OidcIdToken.withTokenValue("token").subject("sub-1").build());
  }

  @Test
  void 사용자_이름과_이메일을_그대로_쓴다() {
    OidcUser user = 사용자("홍길동", "hong@example.com");

    assertThat(BbsOidcUserService.nicknameOf(user)).isEqualTo("홍길동");
    assertThat(BbsOidcUserService.emailOf(user)).isEqualTo("hong@example.com");
  }

  @Test
  void 사용자_이름이나_이메일이_없으면_subject로_대신한다() {
    OidcUser user = 이름과_이메일이_없는_사용자();

    assertThat(BbsOidcUserService.nicknameOf(user)).isEqualTo("sub-1");
    assertThat(BbsOidcUserService.emailOf(user)).isEqualTo("sub-1@unknown.local");
  }

  @Test
  void 사용자_이름이나_이메일이_공백뿐이면_subject로_대신한다() {
    OidcUser user = 사용자("   ", " ");

    assertThat(BbsOidcUserService.nicknameOf(user)).isEqualTo("sub-1");
    assertThat(BbsOidcUserService.emailOf(user)).isEqualTo("sub-1@unknown.local");
  }
}
