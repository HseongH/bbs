package com.board.bbs.auth.login;

import com.board.bbs.token.InternalTokens;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/** Keycloak 로그인 직후 realm 역할 중 게시판이 정의한 역할만 권한으로 남긴다 (MEM-FR-003). */
@Service
public class BbsOidcUserService extends OidcUserService {

  private static final String REALM_ACCESS_CLAIM = "realm_access";
  private static final String ROLE_PREFIX = "ROLE_";

  /** 사용자 이름(preferred_username)은 IdP에서 비울 수 있으므로 항상 있는 sub를 이름으로 쓴다. */
  private static final String NAME_ATTRIBUTE = "sub";

  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    return withBbsRoles(super.loadUser(userRequest));
  }

  static OidcUser withBbsRoles(OidcUser oidcUser) {
    Set<GrantedAuthority> authorities = new LinkedHashSet<>(oidcUser.getAuthorities());
    authorities.addAll(bbsRoles(oidcUser));
    return new DefaultOidcUser(
        authorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), NAME_ATTRIBUTE);
  }

  private static List<GrantedAuthority> bbsRoles(OidcUser oidcUser) {
    if (!(oidcUser.getClaims().get(REALM_ACCESS_CLAIM) instanceof Map<?, ?> realmAccess)) {
      return List.of();
    }
    if (!(realmAccess.get("roles") instanceof Collection<?> roles)) {
      return List.of();
    }
    return roles.stream()
        .map(String::valueOf)
        .filter(InternalTokens.KNOWN_ROLES::contains)
        .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(ROLE_PREFIX + role))
        .toList();
  }
}
