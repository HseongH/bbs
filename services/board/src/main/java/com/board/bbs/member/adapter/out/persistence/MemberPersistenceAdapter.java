package com.board.bbs.member.adapter.out.persistence;

import com.board.bbs.common.error.BusinessException;
import com.board.bbs.common.error.ErrorCode;
import com.board.bbs.member.application.port.out.MemberRepository;
import com.board.bbs.member.domain.Member;
import com.board.bbs.member.domain.MemberId;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class MemberPersistenceAdapter implements MemberRepository {

  private final MemberJpaRepository repository;

  MemberPersistenceAdapter(MemberJpaRepository repository) {
    this.repository = repository;
  }

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
  public Member saveIfAbsent(Member member) {
    repository.insertIfAbsent(member.getSubject(), member.getNickname().value(), member.getEmail());
    return repository
        .findBySubject(member.getSubject())
        .map(MemberMapper::toDomain)
        .orElseThrow(() -> new IllegalStateException("방금 저장한 회원을 찾을 수 없습니다."));
  }
}
