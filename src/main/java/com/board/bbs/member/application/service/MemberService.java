package com.board.bbs.member.application.service;

import com.board.bbs.member.application.port.in.ProvisionMemberUseCase;
import com.board.bbs.member.application.port.out.LoadMemberPort;
import com.board.bbs.member.application.port.out.SaveMemberPort;
import com.board.bbs.member.domain.Member;
import com.board.bbs.member.domain.MemberId;
import com.board.bbs.member.domain.Nickname;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 회원 유스케이스 구현. */
@Service
@RequiredArgsConstructor
public class MemberService implements ProvisionMemberUseCase {

  private final LoadMemberPort loadMemberPort;
  private final SaveMemberPort saveMemberPort;

  @Override
  @Transactional
  public MemberId provision(String subject, String nickname, String email) {
    return loadMemberPort
        .findBySubject(subject)
        .map(Member::getId)
        .orElseGet(
            () ->
                saveMemberPort
                    .save(Member.provision(subject, new Nickname(nickname), email))
                    .getId());
  }

  /**
   * 식별자로 회원을 조회한다.
   *
   * @param id 회원 식별자
   * @return 회원
   */
  @Transactional(readOnly = true)
  public Member getById(MemberId id) {
    return loadMemberPort.loadById(id);
  }
}
