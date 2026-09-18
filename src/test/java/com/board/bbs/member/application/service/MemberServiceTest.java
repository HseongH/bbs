package com.board.bbs.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.board.bbs.common.error.BusinessException;
import com.board.bbs.member.application.port.out.LoadMemberPort;
import com.board.bbs.member.application.port.out.SaveMemberPort;
import com.board.bbs.member.domain.Member;
import com.board.bbs.member.domain.MemberId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class MemberServiceTest {

  private final InMemoryMemberStore store = new InMemoryMemberStore();
  private final MemberService service = new MemberService(store, store);

  @Test
  void 처음_로그인하면_회원이_생성된다() {
    MemberId id = service.provision("sub-1", "테스터", "tester@example.com");

    assertThat(id).isNotNull();
    assertThat(store.findBySubject("sub-1")).isPresent();
  }

  @Test
  void 이미_가입한_회원은_중복_생성되지_않는다() {
    MemberId first = service.provision("sub-1", "테스터", "tester@example.com");
    MemberId second = service.provision("sub-1", "테스터", "tester@example.com");

    assertThat(second).isEqualTo(first);
    assertThat(store.count()).isEqualTo(1);
  }

  @Test
  void 존재하지_않는_회원을_조회하면_예외가_발생한다() {
    assertThatThrownBy(() -> service.getById(new MemberId(999L)))
        .isInstanceOf(BusinessException.class);
  }

  static class InMemoryMemberStore implements LoadMemberPort, SaveMemberPort {

    private final Map<String, Member> bySubject = new HashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public Optional<Member> findBySubject(String subject) {
      return Optional.ofNullable(bySubject.get(subject));
    }

    @Override
    public Member loadById(MemberId id) {
      return bySubject.values().stream()
          .filter(member -> id.equals(member.getId()))
          .findFirst()
          .orElseThrow(
              () -> new BusinessException(com.board.bbs.common.error.ErrorCode.MEMBER_NOT_FOUND));
    }

    @Override
    public Member save(Member member) {
      Member saved =
          Member.restore(
              member.getId() == null ? new MemberId(sequence.incrementAndGet()) : member.getId(),
              member.getSubject(),
              member.getNickname(),
              member.getEmail());
      bySubject.put(saved.getSubject(), saved);
      return saved;
    }

    int count() {
      return bySubject.size();
    }
  }
}
