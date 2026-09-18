package com.board.bbs.member.application.port.out;

import com.board.bbs.member.domain.Member;
import com.board.bbs.member.domain.MemberId;
import java.util.Optional;

/** 회원 조회 포트. */
public interface LoadMemberPort {

  /**
   * Keycloak 사용자 식별자로 회원을 찾는다.
   *
   * @param subject Keycloak 사용자 식별자
   * @return 회원. 없으면 빈 값
   */
  Optional<Member> findBySubject(String subject);

  /**
   * 식별자로 회원을 읽는다.
   *
   * @param id 회원 식별자
   * @return 회원
   * @throws com.board.bbs.common.error.BusinessException 회원이 없으면 MEMBER_NOT_FOUND
   */
  Member loadById(MemberId id);
}
