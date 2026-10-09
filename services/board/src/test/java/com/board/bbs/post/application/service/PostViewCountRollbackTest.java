package com.board.bbs.post.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.application.port.out.PostRepository;
import com.board.bbs.post.domain.PostId;
import com.board.bbs.support.IntegrationTestBase;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class PostViewCountRollbackTest extends IntegrationTestBase {

  private static final String VIEWER = "m-rollback";

  @Autowired private PostQueryService postQueryService;
  @Autowired private PostCommandService postCommandService;
  @Autowired private JdbcTemplate jdbcTemplate;

  @MockitoSpyBean private PostRepository postRepository;

  private PostId postId;

  @BeforeEach
  void 데이터를_준비한다() {
    jdbcTemplate.update("DELETE FROM post_like");
    jdbcTemplate.update("DELETE FROM comment");
    jdbcTemplate.update("DELETE FROM post");
    jdbcTemplate.update("DELETE FROM member");
    jdbcTemplate.update(
        "INSERT INTO member (subject, nickname, email, created_at, updated_at)"
            + " VALUES ('sub-view', '조회테스터', 'view@example.com', now(), now())");
    MemberId author =
        new MemberId(
            Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                    "SELECT id FROM member WHERE subject = 'sub-view'", Long.class)));
    postId = postCommandService.create(author, "조회수 롤백", "본문입니다.");
  }

  @Test
  void 조회수_증가가_실패해서_롤백되면_다음_조회에서_다시_센다() {
    doThrow(new TransientDataAccessResourceException("증가 실패"))
        .when(postRepository)
        .increaseViewCount(any());

    assertThatThrownBy(() -> postQueryService.getAndCountView(postId, VIEWER))
        .isInstanceOf(TransientDataAccessResourceException.class);

    reset(postRepository);
    postQueryService.getAndCountView(postId, VIEWER);

    assertThat(조회수()).isEqualTo(1);
  }

  @Test
  void 같은_조회자의_두_번째_조회는_세지_않는다() {
    postQueryService.getAndCountView(postId, VIEWER);
    postQueryService.getAndCountView(postId, VIEWER);

    assertThat(조회수()).isEqualTo(1);
  }

  private Long 조회수() {
    return jdbcTemplate.queryForObject(
        "SELECT view_count FROM post WHERE id = ?", Long.class, postId.value());
  }
}
