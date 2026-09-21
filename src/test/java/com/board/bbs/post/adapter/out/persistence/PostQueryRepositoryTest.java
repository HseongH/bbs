package com.board.bbs.post.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.board.bbs.post.application.PostSearchCondition;
import com.board.bbs.post.application.PostSummary;
import com.board.bbs.support.IntegrationTestBase;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class PostQueryRepositoryTest extends IntegrationTestBase {

  @Autowired private PostQueryRepository queryRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  private Long authorId;
  private Long otherAuthorId;

  @BeforeEach
  void 데이터를_준비한다() {
    jdbcTemplate.update("DELETE FROM post");
    jdbcTemplate.update("DELETE FROM member");

    authorId = 회원을_만든다("sub-q1", "질의테스터");
    otherAuthorId = 회원을_만든다("sub-q2", "다른작성자");

    게시글을_만든다("스프링 부트 입문", "본문입니다", authorId, null);
    게시글을_만든다("자바 25 톺아보기", "스프링과 무관한 본문", authorId, null);
    게시글을_만든다("다른 사람 글", "본문입니다", otherAuthorId, null);
    게시글을_만든다("삭제된 글", "본문입니다", authorId, "now()");
  }

  private Long 회원을_만든다(String subject, String nickname) {
    jdbcTemplate.update(
        "INSERT INTO member (subject, nickname, email, created_at, updated_at)"
            + " VALUES (?, ?, ?, now(), now())",
        subject,
        nickname,
        subject + "@example.com");
    return Objects.requireNonNull(
        jdbcTemplate.queryForObject(
            "SELECT id FROM member WHERE subject = ?", Long.class, subject));
  }

  private void 게시글을_만든다(String title, String content, Long author, String deletedAt) {
    jdbcTemplate.update(
        "INSERT INTO post (title, content, author_id, view_count, like_count,"
            + " created_at, updated_at, deleted_at)"
            + " VALUES (?, ?, ?, 0, 0, now(), now(), "
            + (deletedAt == null ? "NULL" : deletedAt)
            + ")",
        title,
        content,
        author);
  }

  @Test
  void 삭제된_게시글은_목록에_나오지_않는다() {
    Page<PostSummary> result =
        queryRepository.search(new PostSearchCondition(null, null), PageRequest.of(0, 10));

    assertThat(result.getTotalElements()).isEqualTo(3);
    assertThat(result.getContent()).extracting(PostSummary::title).doesNotContain("삭제된 글");
  }

  @Test
  void 키워드로_제목을_검색할_수_있다() {
    Page<PostSummary> result =
        queryRepository.search(new PostSearchCondition("부트 입문", null), PageRequest.of(0, 10));

    assertThat(result.getContent()).extracting(PostSummary::title).containsExactly("스프링 부트 입문");
  }

  @Test
  void 키워드는_본문도_함께_검색한다() {
    Page<PostSummary> result =
        queryRepository.search(new PostSearchCondition("스프링", null), PageRequest.of(0, 10));

    assertThat(result.getContent())
        .extracting(PostSummary::title)
        .containsExactlyInAnyOrder("스프링 부트 입문", "자바 25 톺아보기");
  }

  @Test
  void 키워드가_공백뿐이면_조건에서_제외된다() {
    Page<PostSummary> result =
        queryRepository.search(new PostSearchCondition("   ", null), PageRequest.of(0, 10));

    assertThat(result.getTotalElements()).isEqualTo(3);
  }

  @Test
  void 작성자로_필터링할_수_있다() {
    Page<PostSummary> result =
        queryRepository.search(new PostSearchCondition(null, otherAuthorId), PageRequest.of(0, 10));

    assertThat(result.getContent()).extracting(PostSummary::title).containsExactly("다른 사람 글");
  }

  @Test
  void 작성자_닉네임이_함께_조회된다() {
    Page<PostSummary> result =
        queryRepository.search(new PostSearchCondition(null, authorId), PageRequest.of(0, 10));

    assertThat(result.getContent())
        .allSatisfy(summary -> assertThat(summary.authorNickname()).isEqualTo("질의테스터"));
  }

  @Test
  void 페이지_크기를_넘으면_다음_페이지로_넘어간다() {
    Page<PostSummary> first =
        queryRepository.search(new PostSearchCondition(null, null), PageRequest.of(0, 2));

    assertThat(first.getContent()).hasSize(2);
    assertThat(first.getTotalElements()).isEqualTo(3);
    assertThat(first.isLast()).isFalse();
  }
}
