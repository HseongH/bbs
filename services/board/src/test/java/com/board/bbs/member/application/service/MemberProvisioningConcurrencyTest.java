package com.board.bbs.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.board.bbs.member.domain.MemberId;
import com.board.bbs.support.IntegrationTestBase;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** 동시성을 재현해야 하므로 테스트에 트랜잭션을 걸지 않는다. */
class MemberProvisioningConcurrencyTest extends IntegrationTestBase {

  private static final int 동시_로그인_횟수 = 16;

  @Autowired private MemberService memberService;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void 데이터를_비운다() {
    jdbcTemplate.update("DELETE FROM post_like");
    jdbcTemplate.update("DELETE FROM comment");
    jdbcTemplate.update("DELETE FROM post");
    jdbcTemplate.update("DELETE FROM member");
  }

  @Test
  void 같은_사용자가_동시에_처음_로그인해도_모두_성공하고_회원은_하나다() throws Exception {
    Set<MemberId> ids = new HashSet<>();
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Callable<MemberId>> logins =
          IntStream.range(0, 동시_로그인_횟수)
              .mapToObj(
                  i ->
                      (Callable<MemberId>)
                          () -> memberService.provision("sub-race", "동시", "race@example.com"))
              .toList();
      for (Future<MemberId> login : executor.invokeAll(logins)) {
        ids.add(login.get());
      }
    }

    assertThat(ids).hasSize(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM member WHERE subject = 'sub-race'", Long.class))
        .isEqualTo(1);
  }
}
