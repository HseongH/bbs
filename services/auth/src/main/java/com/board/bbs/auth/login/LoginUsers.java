package com.board.bbs.auth.login;

import com.board.bbs.token.InternalTokens;
import com.board.bbs.token.InternalUser;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/** 로그인한 사용자를 내부 토큰이 나르는 사용자로 바꾼다. */
public final class LoginUsers {

  private static final String ROLE_PREFIX = "ROLE_";

  private LoginUsers() {}

  /**
   * 세션의 로그인 사용자에서 내부 토큰의 사용자를 만든다.
   *
   * <p>사용자 이름이나 이메일이 없거나 공백뿐이면 subject로 대신한다. 공백뿐인 이름을 그대로 넘기면 board의 닉네임 규칙에 걸려 회원을 만들 수 없다.
   *
   * @param oidcUser 로그인한 사용자
   * @return 내부 토큰의 사용자
   */
  public static InternalUser toInternalUser(OidcUser oidcUser) {
    String subject = Objects.requireNonNull(oidcUser.getSubject(), "OIDC 토큰에는 sub가 반드시 있다.");
    return new InternalUser(
        subject,
        Objects.requireNonNullElse(nonBlankOrNull(oidcUser.getPreferredUsername()), subject),
        Objects.requireNonNullElse(nonBlankOrNull(oidcUser.getEmail()), subject + "@unknown.local"),
        rolesOf(oidcUser));
  }

  private static Set<String> rolesOf(OidcUser oidcUser) {
    return oidcUser.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .filter(authority -> authority != null && authority.startsWith(ROLE_PREFIX))
        .map(authority -> authority.substring(ROLE_PREFIX.length()))
        .filter(InternalTokens.KNOWN_ROLES::contains)
        .collect(Collectors.toUnmodifiableSet());
  }

  private static @Nullable String nonBlankOrNull(@Nullable String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
