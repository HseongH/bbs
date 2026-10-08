package com.board.bbs.post.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.domain.Content;
import com.board.bbs.post.domain.Post;
import com.board.bbs.post.domain.PostId;
import com.board.bbs.post.domain.Title;
import com.board.bbs.support.IntegrationTestBase;
import java.util.Objects;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 게시글을 불러온 뒤 저장하기 전에 다른 트랜잭션이 카운터를 올리는 순서를 재현한다.
 *
 * <p>카운터는 원자적 UPDATE로만 바뀌어야 하므로, 게시글 저장이 불러올 당시의 값으로 덮어쓰면 안 된다. 커밋된 데이터가 필요하므로 테스트에 트랜잭션을 걸지 않는다.
 */
class PostCounterPreservationTest extends IntegrationTestBase {

  @Autowired private PostPersistenceAdapter adapter;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private PlatformTransactionManager transactionManager;

  private MemberId author;
  private PostId postId;

  @BeforeEach
  void 데이터를_준비한다() {
    jdbcTemplate.update("DELETE FROM post_like");
    jdbcTemplate.update("DELETE FROM comment");
    jdbcTemplate.update("DELETE FROM post");
    jdbcTemplate.update("DELETE FROM member");

    jdbcTemplate.update(
        "INSERT INTO member (subject, nickname, email, created_at, updated_at)"
            + " VALUES ('sub-counter', '작성자', 'counter@example.com', now(), now())");
    author =
        new MemberId(
            Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                    "SELECT id FROM member WHERE subject = 'sub-counter'", Long.class)));

    Post saved = adapter.save(Post.write(new Title("제목"), new Content("본문"), author));
    postId = Objects.requireNonNull(saved.getId());
  }

  @Test
  void 게시글을_수정하는_사이에_오른_좋아요_수가_유지된다() {
    수정하는_사이에_실행한다(adapter::increaseLikeCount);

    assertThat(카운터("like_count")).isEqualTo(1);
  }

  @Test
  void 게시글을_수정하는_사이에_오른_조회수가_유지된다() {
    수정하는_사이에_실행한다(adapter::increaseViewCount);

    assertThat(카운터("view_count")).isEqualTo(1);
  }

  private void 수정하는_사이에_실행한다(Consumer<PostId> 다른_트랜잭션의_작업) {
    TransactionTemplate 수정_트랜잭션 = new TransactionTemplate(transactionManager);
    TransactionTemplate 다른_트랜잭션 = new TransactionTemplate(transactionManager);
    다른_트랜잭션.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    수정_트랜잭션.executeWithoutResult(
        status -> {
          Post post = adapter.load(postId);

          다른_트랜잭션.executeWithoutResult(inner -> 다른_트랜잭션의_작업.accept(postId));

          post.updateBy(author, new Title("수정한 제목"), new Content("수정한 본문"));
          adapter.save(post);
        });
  }

  private long 카운터(String column) {
    return Objects.requireNonNull(
        jdbcTemplate.queryForObject(
            "SELECT " + column + " FROM post WHERE id = ?", Long.class, postId.value()));
  }
}
