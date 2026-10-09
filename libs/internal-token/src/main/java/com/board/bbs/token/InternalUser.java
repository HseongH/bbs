package com.board.bbs.token;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * 내부 토큰이 나르는 사용자.
 *
 * @param subject 외부 사용자 식별자. 출처를 모르는 불투명한 문자열로 다룬다
 * @param nickname 닉네임
 * @param email 이메일
 * @param roles bbs가 정의한 역할
 */
public record InternalUser(String subject, String nickname, String email, Set<String> roles) {

  /** 역할 집합을 바꿀 수 없는 사본으로 고정한다. */
  public InternalUser {
    roles = Set.copyOf(roles);
  }

  /**
   * 검증된 토큰에서 사용자를 꺼낸다.
   *
   * @param jwt 서명과 시각, 발급자, 대상을 검증한 토큰
   * @return 사용자. 역할 클레임이 없거나 목록이 아니면 역할이 없는 사용자
   */
  public static InternalUser from(Jwt jwt) {
    return new InternalUser(
        Objects.requireNonNull(jwt.getSubject(), "내부 토큰에는 sub가 반드시 있다."),
        Objects.requireNonNullElse(jwt.getClaimAsString(InternalTokens.NICKNAME), ""),
        Objects.requireNonNullElse(jwt.getClaimAsString(InternalTokens.EMAIL), ""),
        rolesOf(jwt.getClaims().get(InternalTokens.ROLES)));
  }

  private static Set<String> rolesOf(@Nullable Object claim) {
    if (!(claim instanceof Collection<?> values)) {
      return Set.of();
    }
    return values.stream().map(String::valueOf).collect(Collectors.toUnmodifiableSet());
  }
}
