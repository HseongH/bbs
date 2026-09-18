package com.board.bbs.member.adapter.out.persistence;

import com.board.bbs.common.error.BusinessException;
import com.board.bbs.common.error.ErrorCode;
import com.board.bbs.member.application.port.out.LoadMemberPort;
import com.board.bbs.member.application.port.out.SaveMemberPort;
import com.board.bbs.member.domain.Member;
import com.board.bbs.member.domain.MemberId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class MemberPersistenceAdapter implements LoadMemberPort, SaveMemberPort {

  private final MemberJpaRepository repository;

  @Override
  public Optional<Member> findBySubject(String subject) {
    return repository.findBySubject(subject).map(MemberMapper::toDomain);
  }

  @Override
  public Member loadById(MemberId id) {
    return repository
        .findById(id.value())
        .map(MemberMapper::toDomain)
        .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
  }

  @Override
  public Member save(Member member) {
    return MemberMapper.toDomain(repository.save(MemberMapper.toEntity(member)));
  }
}
