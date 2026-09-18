package com.board.bbs.member.adapter.in.web.dto;

import com.board.bbs.member.domain.Member;

/**
 * 회원 응답.
 *
 * @param id 회원 식별자
 * @param nickname 닉네임
 * @param email 이메일
 */
public record MemberResponse(Long id, String nickname, String email) {

  /**
   * 도메인 회원을 응답으로 변환한다.
   *
   * @param member 회원
   * @return 회원 응답
   */
  public static MemberResponse from(Member member) {
    return new MemberResponse(
        member.getId().value(), member.getNickname().value(), member.getEmail());
  }
}
