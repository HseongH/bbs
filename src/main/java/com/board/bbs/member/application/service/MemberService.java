package com.board.bbs.member.application.service;

import com.board.bbs.common.error.BusinessException;
import com.board.bbs.common.error.ErrorCode;
import com.board.bbs.member.application.port.out.MemberRepository;
import com.board.bbs.member.domain.Member;
import com.board.bbs.member.domain.MemberId;
import com.board.bbs.member.domain.Nickname;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 회원 유스케이스 구현. */
@Service
public class MemberService {

  private final MemberRepository memberRepository;

  MemberService(MemberRepository memberRepository) {
    this.memberRepository = memberRepository;
  }

  /**
   * 회원이 없으면 만들고, 있으면 기존 회원을 그대로 쓴다.
   *
   * @param subject Keycloak 사용자 식별자
   * @param nickname 닉네임
   * @param email 이메일
   * @return 회원 식별자
   */
  @Transactional
  public MemberId provision(String subject, String nickname, String email) {
    return memberRepository
        .findBySubject(subject)
        .map(Member::getId)
        .orElseGet(
            () ->
                memberRepository
                    .save(Member.provision(subject, new Nickname(nickname), email))
                    .getId());
  }

  /**
   * Keycloak 사용자 식별자로 회원 식별자를 찾는다.
   *
   * @param subject Keycloak 사용자 식별자
   * @return 회원 식별자
   * @throws BusinessException 회원이 없으면 MEMBER_NOT_FOUND
   */
  @Transactional(readOnly = true)
  public MemberId getIdBySubject(String subject) {
    return memberRepository
        .findBySubject(subject)
        .map(Member::getId)
        .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
  }

  /**
   * 식별자로 회원을 조회한다.
   *
   * @param id 회원 식별자
   * @return 회원
   */
  @Transactional(readOnly = true)
  public Member getById(MemberId id) {
    return memberRepository.loadById(id);
  }
}
