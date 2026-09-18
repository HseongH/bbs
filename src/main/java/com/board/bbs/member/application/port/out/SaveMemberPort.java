package com.board.bbs.member.application.port.out;

import com.board.bbs.member.domain.Member;

/** 회원 저장 포트. */
public interface SaveMemberPort {

  /**
   * 회원을 저장하고 식별자가 부여된 회원을 반환한다.
   *
   * @param member 저장할 회원
   * @return 저장된 회원
   */
  Member save(Member member);
}
