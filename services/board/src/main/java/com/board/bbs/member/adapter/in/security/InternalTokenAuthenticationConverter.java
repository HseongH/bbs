package com.board.bbs.member.adapter.in.security;

import com.board.bbs.token.InternalUser;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** 내부 토큰의 역할을 {@code ROLE_} 접두사의 권한으로 바꾼다 (MEM-FR-003). 역할 클레임이 없으면 권한이 없는 사용자다. */
@Component
public class InternalTokenAuthenticationConverter
    implements Converter<Jwt, AbstractAuthenticationToken> {

  private static final String ROLE_PREFIX = "ROLE_";

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    InternalUser user = InternalUser.from(jwt);
    return new JwtAuthenticationToken(
        jwt,
        user.roles().stream()
            .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(ROLE_PREFIX + role))
            .toList(),
        user.subject());
  }
}
