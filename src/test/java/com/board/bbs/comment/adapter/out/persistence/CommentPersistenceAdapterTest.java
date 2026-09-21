package com.board.bbs.comment.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.board.bbs.comment.domain.Comment;
import com.board.bbs.comment.domain.CommentBody;
import com.board.bbs.comment.domain.CommentId;
import com.board.bbs.common.error.BusinessException;
import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.domain.PostId;
import com.board.bbs.support.IntegrationTestBase;
import java.time.Instant;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class CommentPersistenceAdapterTest extends IntegrationTestBase {

  @Autowired private CommentPersistenceAdapter adapter;
  @Autowired private JdbcTemplate jdbcTemplate;

  private MemberId author;
  private PostId post;

  @BeforeEach
  void 데이터를_준비한다() {
    jdbcTemplate.update("DELETE FROM comment");
    jdbcTemplate.update("DELETE FROM post");
    jdbcTemplate.update("DELETE FROM member");

    jdbcTemplate.update(
        "INSERT INTO member (subject, nickname, email, created_at, updated_at)"
            + " VALUES ('sub-c', '댓글테스터', 'c@example.com', now(), now())");
    author =
        new MemberId(
            Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                    "SELECT id FROM member WHERE subject = 'sub-c'", Long.class)));

    jdbcTemplate.update(
        "INSERT INTO post (title, content, author_id, view_count, like_count,"
            + " created_at, updated_at) VALUES ('글', '본문', ?, 0, 0, now(), now())",
        author.value());
    post =
        new PostId(
            Objects.requireNonNull(
                jdbcTemplate.queryForObject("SELECT max(id) FROM post", Long.class)));
  }

  @Test
  void 저장한_댓글을_다시_읽을_수_있다() {
    Comment saved = adapter.save(Comment.write(post, author, new CommentBody("댓글"), null, 0));

    Comment loaded = adapter.load(Objects.requireNonNull(saved.getId()));

    assertThat(loaded.getBody().value()).isEqualTo("댓글");
    assertThat(loaded.getDepth()).isZero();
    assertThat(loaded.getPostId()).isEqualTo(post);
  }

  @Test
  void 대댓글은_부모_식별자와_깊이를_유지한다() {
    Comment parent = adapter.save(Comment.write(post, author, new CommentBody("원댓글"), null, 0));
    CommentId parentId = Objects.requireNonNull(parent.getId());

    Comment reply = adapter.save(Comment.write(post, author, new CommentBody("답글"), parentId, 0));

    Comment loaded = adapter.load(Objects.requireNonNull(reply.getId()));
    assertThat(loaded.getParentId()).isEqualTo(parentId);
    assertThat(loaded.getDepth()).isEqualTo(1);
  }

  @Test
  void 삭제된_댓글은_읽을_수_없다() {
    Comment saved = adapter.save(Comment.write(post, author, new CommentBody("댓글"), null, 0));
    CommentId id = Objects.requireNonNull(saved.getId());
    jdbcTemplate.update("UPDATE comment SET deleted_at = now() WHERE id = ?", id.value());

    assertThatThrownBy(() -> adapter.load(id)).isInstanceOf(BusinessException.class);
  }

  @Test
  void 게시글의_댓글이_한꺼번에_삭제된다() {
    adapter.save(Comment.write(post, author, new CommentBody("첫 댓글"), null, 0));
    adapter.save(Comment.write(post, author, new CommentBody("둘째 댓글"), null, 0));
    assertThat(adapter.listByPost(post, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(2);

    adapter.softDeleteAllByPost(post, Instant.now());

    assertThat(adapter.listByPost(post, PageRequest.of(0, 10)).getTotalElements()).isZero();
  }

  @Test
  void 댓글_목록은_작성_순서대로_반환된다() {
    adapter.save(Comment.write(post, author, new CommentBody("첫째"), null, 0));
    adapter.save(Comment.write(post, author, new CommentBody("둘째"), null, 0));
    adapter.save(Comment.write(post, author, new CommentBody("셋째"), null, 0));

    assertThat(adapter.listByPost(post, PageRequest.of(0, 10)).getContent())
        .extracting(comment -> comment.getBody().value())
        .containsExactly("첫째", "둘째", "셋째");
  }
}
