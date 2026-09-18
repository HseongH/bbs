package com.board.bbs.common.security;

import com.board.bbs.member.application.port.in.ProvisionMemberUseCase;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/** Keycloak 로그인 직후 로컬 회원을 보장한다. 이후 어디서도 회원 존재 여부를 걱정할 필요가 없다. */
@Service
@RequiredArgsConstructor
public class BbsOidcUserService extends OidcUserService {

  private static final String REALM_ACCESS_CLAIM = "realm_access";
  private static final String ROLE_PREFIX = "ROLE_";
  private static final String USER_NAME_ATTRIBUTE = "preferred_username";

  private final ProvisionMemberUseCase provisionMemberUseCase;

  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    OidcUser oidcUser = super.loadUser(userRequest);

    String subject = Objects.requireNonNull(oidcUser.getSubject(), "OIDC 토큰에는 sub가 반드시 있다.");
    String nickname = Objects.requireNonNullElse(oidcUser.getPreferredUsername(), subject);
    String email = Objects.requireNonNullElse(oidcUser.getEmail(), subject + "@unknown.local");

    provisionMemberUseCase.provision(subject, nickname, email);

    Set<GrantedAuthority> authorities = new LinkedHashSet<>(oidcUser.getAuthorities());
    authorities.addAll(realmRoles(oidcUser));

    return new DefaultOidcUser(
        authorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), USER_NAME_ATTRIBUTE);
  }

  /** Keycloak의 realm_access.roles를 스프링 시큐리티의 역할 권한으로 옮긴다. */
  private Collection<GrantedAuthority> realmRoles(OidcUser oidcUser) {
    Object realmAccess = oidcUser.getClaims().get(REALM_ACCESS_CLAIM);
    if (!(realmAccess instanceof Map<?, ?> claims)) {
      return List.of();
    }
    if (!(claims.get("roles") instanceof Collection<?> roles)) {
      return List.of();
    }
    return roles.stream()
        .map(String::valueOf)
        .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(ROLE_PREFIX + role))
        .toList();
  }
}
