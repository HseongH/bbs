package com.board.bbs.member.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.board.bbs.common.error.BusinessException;
import com.board.bbs.member.application.port.out.LoadMemberPort;
import com.board.bbs.member.application.port.out.SaveMemberPort;
import com.board.bbs.member.domain.Member;
import com.board.bbs.member.domain.MemberId;
import com.board.bbs.member.domain.Nickname;
import com.board.bbs.support.IntegrationTestBase;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class MemberPersistenceAdapterTest extends IntegrationTestBase {

  @Autowired private LoadMemberPort loadMemberPort;
  @Autowired private SaveMemberPort saveMemberPort;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void 데이터를_비운다() {
    jdbcTemplate.update("DELETE FROM post_like");
    jdbcTemplate.update("DELETE FROM comment");
    jdbcTemplate.update("DELETE FROM post");
    jdbcTemplate.update("DELETE FROM member");
  }

  @Test
  void 저장한_회원을_식별자로_읽을_수_있다() {
    Member saved =
        saveMemberPort.save(Member.provision("sub-1", new Nickname("테스터"), "t@example.com"));
    MemberId id = Objects.requireNonNull(saved.getId());

    Member loaded = loadMemberPort.loadById(id);

    assertThat(loaded.getSubject()).isEqualTo("sub-1");
    assertThat(loaded.getNickname().value()).isEqualTo("테스터");
    assertThat(loaded.getEmail()).isEqualTo("t@example.com");
  }

  @Test
  void 사용자_식별자로_회원을_찾을_수_있다() {
    saveMemberPort.save(Member.provision("sub-2", new Nickname("둘째"), "t2@example.com"));

    assertThat(loadMemberPort.findBySubject("sub-2")).isPresent();
    assertThat(loadMemberPort.findBySubject("없는-sub")).isEmpty();
  }

  @Test
  void 존재하지_않는_회원을_읽으면_예외가_발생한다() {
    assertThatThrownBy(() -> loadMemberPort.loadById(new MemberId(999_999L)))
        .isInstanceOf(BusinessException.class);
  }
}
