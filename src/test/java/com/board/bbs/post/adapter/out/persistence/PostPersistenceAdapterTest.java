package com.board.bbs.post.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.board.bbs.common.error.BusinessException;
import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.domain.Content;
import com.board.bbs.post.domain.Post;
import com.board.bbs.post.domain.PostId;
import com.board.bbs.post.domain.Title;
import com.board.bbs.support.IntegrationTestBase;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class PostPersistenceAdapterTest extends IntegrationTestBase {

  @Autowired private PostPersistenceAdapter adapter;
  @Autowired private JdbcTemplate jdbcTemplate;

  private MemberId 회원을_만든다(String subject) {
    jdbcTemplate.update(
        "INSERT INTO member (subject, nickname, email, created_at, updated_at)"
            + " VALUES (?, '테스터', 'tester@example.com', now(), now())",
        subject);
    Long id =
        jdbcTemplate.queryForObject("SELECT id FROM member WHERE subject = ?", Long.class, subject);
    return new MemberId(Objects.requireNonNull(id));
  }

  @Test
  void 저장한_게시글을_다시_읽을_수_있다() {
    MemberId author = 회원을_만든다("sub-save");

    Post saved = adapter.save(Post.write(new Title("제목"), new Content("본문"), author));
    Post loaded = adapter.load(Objects.requireNonNull(saved.getId()));

    assertThat(loaded.getTitle().value()).isEqualTo("제목");
    assertThat(loaded.getContent().value()).isEqualTo("본문");
    assertThat(loaded.getAuthorId()).isEqualTo(author);
    assertThat(loaded.getCreatedAt()).isNotNull();
  }

  @Test
  void 존재하지_않는_게시글을_읽으면_예외가_발생한다() {
    assertThatThrownBy(() -> adapter.load(new PostId(999_999L)))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void 삭제된_게시글은_읽을_수_없다() {
    MemberId author = 회원을_만든다("sub-deleted");
    Post saved = adapter.save(Post.write(new Title("삭제될 글"), new Content("본문"), author));
    PostId id = Objects.requireNonNull(saved.getId());

    jdbcTemplate.update("UPDATE post SET deleted_at = now() WHERE id = ?", id.value());

    assertThatThrownBy(() -> adapter.load(id)).isInstanceOf(BusinessException.class);
  }
}
