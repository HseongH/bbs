package com.board.bbs.member.adapter.out.persistence;

import com.board.bbs.member.domain.Member;
import com.board.bbs.member.domain.MemberId;
import com.board.bbs.member.domain.Nickname;

/** 도메인과 영속성 모델 사이의 유일한 변환 지점. */
final class MemberMapper {

  private MemberMapper() {}

  static Member toDomain(MemberJpaEntity entity) {
    return Member.restore(
        new MemberId(entity.getId()),
        entity.getSubject(),
        new Nickname(entity.getNickname()),
        entity.getEmail());
  }

  static MemberJpaEntity toEntity(Member member) {
    return new MemberJpaEntity(
        member.getId() == null ? null : member.getId().value(),
        member.getSubject(),
        member.getNickname().value(),
        member.getEmail());
  }
}
