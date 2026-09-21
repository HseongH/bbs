package com.board.bbs.post.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.board.bbs.comment.application.port.in.WriteCommentUseCase;
import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.domain.PostId;
import com.board.bbs.support.IntegrationTestBase;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class PostDeletionIntegrationTest extends IntegrationTestBase {

  @Autowired private PostCommandService postCommandService;
  @Autowired private WriteCommentUseCase writeCommentUseCase;
  @Autowired private JdbcTemplate jdbcTemplate;

  private MemberId author;

  @BeforeEach
  void 데이터를_비운다() {
    jdbcTemplate.update("DELETE FROM comment");
    jdbcTemplate.update("DELETE FROM post");
    jdbcTemplate.update("DELETE FROM member");

    jdbcTemplate.update(
        "INSERT INTO member (subject, nickname, email, created_at, updated_at)"
            + " VALUES ('sub-del', '삭제테스터', 'del@example.com', now(), now())");
    author =
        new MemberId(
            Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                    "SELECT id FROM member WHERE subject = 'sub-del'", Long.class)));
  }

  @Test
  void 게시글을_삭제하면_게시글과_댓글이_모두_삭제된다() {
    PostId postId = postCommandService.create(author, "삭제될 글", "본문입니다.");
    writeCommentUseCase.write(postId, author, "첫 댓글", null);
    writeCommentUseCase.write(postId, author, "둘째 댓글", null);

    postCommandService.delete(postId, author, false);

    Long 살아있는게시글 =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM post WHERE id = ? AND deleted_at IS NULL",
            Long.class,
            postId.value());
    Long 살아있는댓글 =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM comment WHERE post_id = ? AND deleted_at IS NULL",
            Long.class,
            postId.value());

    assertThat(살아있는게시글).isZero();
    assertThat(살아있는댓글).isZero();
  }
}
