package com.board.bbs.post.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.domain.PostId;
import com.board.bbs.support.IntegrationTestBase;
import java.util.List;
import java.util.Objects;
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
class PostLikeConcurrencyTest extends IntegrationTestBase {

  private static final int 동시_시도_횟수 = 16;

  @Autowired private PostLikeService postLikeService;
  @Autowired private JdbcTemplate jdbcTemplate;

  private MemberId member;
  private PostId post;

  @BeforeEach
  void 데이터를_준비한다() {
    jdbcTemplate.update("DELETE FROM post_like");
    jdbcTemplate.update("DELETE FROM comment");
    jdbcTemplate.update("DELETE FROM post");
    jdbcTemplate.update("DELETE FROM member");

    jdbcTemplate.update(
        "INSERT INTO member (subject, nickname, email, created_at, updated_at)"
            + " VALUES ('sub-like', '좋아요테스터', 'like@example.com', now(), now())");
    member =
        new MemberId(
            Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                    "SELECT id FROM member WHERE subject = 'sub-like'", Long.class)));

    jdbcTemplate.update(
        "INSERT INTO post (title, content, author_id, view_count, like_count,"
            + " created_at, updated_at) VALUES ('글', '본문', ?, 0, 0, now(), now())",
        member.value());
    post =
        new PostId(
            Objects.requireNonNull(
                jdbcTemplate.queryForObject("SELECT max(id) FROM post", Long.class)));
  }

  @Test
  void 동시에_좋아요를_눌러도_한_번만_반영된다() throws Exception {
    long 성공횟수 = 동시에_실행한다(() -> postLikeService.like(post, member));

    assertThat(성공횟수).isEqualTo(1);
    assertThat(좋아요_행수()).isEqualTo(1);
    assertThat(좋아요_카운터()).isEqualTo(1);
  }

  @Test
  void 좋아요를_누르고_취소하면_카운터가_0으로_돌아온다() {
    postLikeService.like(post, member);
    postLikeService.unlike(post, member);

    assertThat(좋아요_행수()).isZero();
    assertThat(좋아요_카운터()).isZero();
  }

  @Test
  void 동시에_취소해도_카운터가_음수로_내려가지_않는다() throws Exception {
    postLikeService.like(post, member);

    long 성공횟수 = 동시에_실행한다(() -> postLikeService.unlike(post, member));

    assertThat(성공횟수).isEqualTo(1);
    assertThat(좋아요_카운터()).isZero();
  }

  private long 동시에_실행한다(Runnable 작업) throws Exception {
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Callable<Boolean>> tasks =
          IntStream.range(0, 동시_시도_횟수)
              .mapToObj(
                  i ->
                      (Callable<Boolean>)
                          () -> {
                            작업.run();
                            return true;
                          })
              .toList();

      long 성공횟수 = 0;
      for (Future<Boolean> future : executor.invokeAll(tasks)) {
        try {
          future.get();
          성공횟수++;
        } catch (Exception e) {
          // 이미 처리된 요청은 예외로 거부되는 것이 정상이다.
        }
      }
      return 성공횟수;
    }
  }

  private Long 좋아요_행수() {
    return jdbcTemplate.queryForObject(
        "SELECT count(*) FROM post_like WHERE post_id = ?", Long.class, post.value());
  }

  private Long 좋아요_카운터() {
    return jdbcTemplate.queryForObject(
        "SELECT like_count FROM post WHERE id = ?", Long.class, post.value());
  }
}
