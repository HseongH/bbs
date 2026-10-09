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
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
    Comment saved = adapter.save(Comment.write(post, author, new CommentBody("댓글"), null));

    Comment loaded = adapter.load(Objects.requireNonNull(saved.getId()));

    assertThat(loaded.getBody().value()).isEqualTo("댓글");
    assertThat(loaded.getDepth()).isZero();
    assertThat(loaded.getPostId()).isEqualTo(post);
  }

  @Test
  void 대댓글은_부모_식별자와_깊이를_유지한다() {
    Comment parent = adapter.save(Comment.write(post, author, new CommentBody("원댓글"), null));
    CommentId parentId = Objects.requireNonNull(parent.getId());

    Comment reply = adapter.save(Comment.write(post, author, new CommentBody("답글"), parent));

    Comment loaded = adapter.load(Objects.requireNonNull(reply.getId()));
    assertThat(loaded.getParentId()).isEqualTo(parentId);
    assertThat(loaded.getDepth()).isEqualTo(1);
  }

  @Test
  void 삭제된_댓글은_읽을_수_없다() {
    Comment saved = adapter.save(Comment.write(post, author, new CommentBody("댓글"), null));
    CommentId id = Objects.requireNonNull(saved.getId());
    jdbcTemplate.update("UPDATE comment SET deleted_at = now() WHERE id = ?", id.value());

    assertThatThrownBy(() -> adapter.load(id)).isInstanceOf(BusinessException.class);
  }

  @Test
  void 게시글의_댓글이_한꺼번에_삭제된다() {
    원댓글("첫 댓글");
    원댓글("둘째 댓글");
    assertThat(adapter.listRoots(post, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(2);

    adapter.softDeleteAllByPost(post, Instant.now());

    assertThat(adapter.listRoots(post, PageRequest.of(0, 10)).getTotalElements()).isZero();
  }

  @Test
  void 원댓글_목록에는_대댓글이_포함되지_않는다() {
    Comment root = 원댓글("원댓글");
    답글(root, "답글");

    assertThat(본문들(adapter.listRoots(post, PageRequest.of(0, 10)).getContent()))
        .containsExactly("원댓글");
  }

  @Test
  void 삭제된_원댓글은_살아있는_대댓글이_있을_때만_포함된다() {
    Comment withLiveReply = 원댓글("A");
    답글(withLiveReply, "a1");
    Comment withDeletedReply = 원댓글("B");
    삭제한다(답글(withDeletedReply, "b1"));
    Comment withoutReply = 원댓글("C");
    삭제한다(withLiveReply);
    삭제한다(withDeletedReply);
    삭제한다(withoutReply);

    List<Comment> roots = adapter.listRoots(post, PageRequest.of(0, 10)).getContent();

    assertThat(roots).extracting(Comment::getId).containsExactly(withLiveReply.getId());
    assertThat(roots.getFirst().isDeleted()).isTrue();
  }

  @Test
  void 원댓글_목록은_작성_순서이고_원댓글_수로_페이지를_나눈다() {
    Comment first = 원댓글("첫째");
    원댓글("둘째");
    원댓글("셋째");
    답글(first, "답글1");
    답글(first, "답글2");

    Page<Comment> firstPage = adapter.listRoots(post, PageRequest.of(0, 2));
    Page<Comment> secondPage = adapter.listRoots(post, PageRequest.of(1, 2));

    assertThat(본문들(firstPage.getContent())).containsExactly("첫째", "둘째");
    assertThat(firstPage.getTotalElements()).isEqualTo(3);
    assertThat(본문들(secondPage.getContent())).containsExactly("셋째");
  }

  @Test
  void 대댓글은_원댓글별로_작성_순서대로_반환되고_삭제된_대댓글은_빠진다() {
    Comment a = 원댓글("A");
    Comment b = 원댓글("B");
    답글(a, "a1");
    삭제한다(답글(a, "a2"));
    답글(b, "b1");
    답글(a, "a3");

    List<Comment> replies = adapter.listRepliesOf(List.of(식별자(a), 식별자(b)));

    assertThat(replies)
        .extracting(reply -> reply.getBody().value() + "@" + reply.getParentId())
        .containsExactlyInAnyOrder("a1@" + a.getId(), "a3@" + a.getId(), "b1@" + b.getId());
    assertThat(본문들(replies.stream().filter(r -> 식별자(a).equals(r.getParentId())).toList()))
        .containsExactly("a1", "a3");
    assertThat(adapter.listRepliesOf(List.of())).isEmpty();
  }

  private Comment 원댓글(String body) {
    return adapter.save(Comment.write(post, author, new CommentBody(body), null));
  }

  private Comment 답글(Comment parent, String body) {
    return adapter.save(Comment.write(post, author, new CommentBody(body), parent));
  }

  /** 같은 트랜잭션의 영속성 컨텍스트와 어긋나지 않도록 도메인을 통해 삭제한다. */
  private Comment 삭제한다(Comment comment) {
    comment.deleteBy(author, false, Instant.now());
    return adapter.save(comment);
  }

  private static CommentId 식별자(Comment comment) {
    return Objects.requireNonNull(comment.getId());
  }

  private static List<String> 본문들(List<Comment> comments) {
    return comments.stream().map(comment -> comment.getBody().value()).toList();
  }
}
