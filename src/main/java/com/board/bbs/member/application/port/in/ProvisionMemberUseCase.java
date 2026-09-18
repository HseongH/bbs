package com.board.bbs.member.application.port.in;

import com.board.bbs.member.domain.MemberId;

/** Keycloak 사용자에 대응하는 로컬 회원을 보장하는 유스케이스. */
public interface ProvisionMemberUseCase {

  /**
   * 회원이 없으면 만들고, 있으면 기존 회원을 그대로 쓴다.
   *
   * @param subject Keycloak 사용자 식별자
   * @param nickname 닉네임
   * @param email 이메일
   * @return 회원 식별자
   */
  MemberId provision(String subject, String nickname, String email);
}
