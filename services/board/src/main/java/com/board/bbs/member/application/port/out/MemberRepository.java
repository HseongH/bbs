package com.board.bbs.member.application.port.out;

import com.board.bbs.member.domain.Member;
import com.board.bbs.member.domain.MemberId;
import java.util.Optional;

/** 회원 저장소 포트. */
public interface MemberRepository {
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

  /**
   * 같은 사용자 식별자의 회원이 없을 때만 저장하고, 저장된 회원을 반환한다.
   *
   * <p>동시에 같은 사용자를 저장해도 실패하지 않는다. 중복 여부는 데이터베이스의 유니크 제약이 판정하며, 이미 있으면 기존 회원을 반환한다.
   *
   * @param member 저장할 회원
   * @return 새로 저장되었거나 이미 있던 회원
   */
  Member saveIfAbsent(Member member);
}
