package com.board.bbs.common.security;

import com.board.bbs.member.application.port.in.ProvisionMemberUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/** Keycloak 로그인 직후 로컬 회원을 보장한다. 이후 어디서도 회원 존재 여부를 걱정할 필요가 없다. */
@Service
@RequiredArgsConstructor
public class BbsOidcUserService extends OidcUserService {

  private final ProvisionMemberUseCase provisionMemberUseCase;

  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    OidcUser oidcUser = super.loadUser(userRequest);

    provisionMemberUseCase.provision(
        oidcUser.getSubject(),
        oidcUser.getPreferredUsername(),
        oidcUser.getEmail() == null
            ? oidcUser.getSubject() + "@unknown.local"
            : oidcUser.getEmail());

    return oidcUser;
  }
}
