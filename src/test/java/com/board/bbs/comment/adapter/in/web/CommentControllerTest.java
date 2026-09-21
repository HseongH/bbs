package com.board.bbs.comment.adapter.in.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.board.bbs.support.IntegrationTestBase;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class CommentControllerTest extends IntegrationTestBase {

  private static final String AUTHOR_SUBJECT = "sub-comment-author";
  private static final String OTHER_SUBJECT = "sub-comment-other";

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  private Long postId;

  @BeforeEach
  void 데이터를_준비한다() {
    jdbcTemplate.update("DELETE FROM post_like");
    jdbcTemplate.update("DELETE FROM comment");
    jdbcTemplate.update("DELETE FROM post");
    jdbcTemplate.update("DELETE FROM member");

    Long authorId = 회원을_만든다(AUTHOR_SUBJECT);
    회원을_만든다(OTHER_SUBJECT);

    jdbcTemplate.update(
        "INSERT INTO post (title, content, author_id, view_count, like_count,"
            + " created_at, updated_at) VALUES ('글', '본문', ?, 0, 0, now(), now())",
        authorId);
    postId =
        Objects.requireNonNull(jdbcTemplate.queryForObject("SELECT max(id) FROM post", Long.class));
  }

  private Long 회원을_만든다(String subject) {
    jdbcTemplate.update(
        "INSERT INTO member (subject, nickname, email, created_at, updated_at)"
            + " VALUES (?, ?, ?, now(), now())",
        subject,
        subject,
        subject + "@example.com");
    return Objects.requireNonNull(
        jdbcTemplate.queryForObject(
            "SELECT id FROM member WHERE subject = ?", Long.class, subject));
  }

  private OidcLoginRequestPostProcessor 로그인(String subject) {
    return oidcLogin()
        .idToken(token -> token.subject(subject).claim("preferred_username", subject));
  }

  private Long 댓글을_만든다(String body, Long parentId) throws Exception {
    String json =
        parentId == null
            ? "{\"body\":\"%s\"}".formatted(body)
            : "{\"body\":\"%s\",\"parentCommentId\":%d}".formatted(body, parentId);

    String location =
        mockMvc
            .perform(
                post("/api/posts/{postId}/comments", postId)
                    .with(로그인(AUTHOR_SUBJECT))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    return Long.valueOf(Objects.requireNonNull(location).replaceAll(".*/", ""));
  }

  @Test
  void 댓글을_작성하고_목록에서_확인할_수_있다() throws Exception {
    댓글을_만든다("첫 댓글", null);

    mockMvc
        .perform(get("/api/posts/{postId}/comments", postId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].body").value("첫 댓글"))
        .andExpect(jsonPath("$.content[0].depth").value(0));
  }

  @Test
  void 대댓글은_깊이_1로_기록된다() throws Exception {
    Long parentId = 댓글을_만든다("원댓글", null);
    댓글을_만든다("답글", parentId);

    mockMvc
        .perform(get("/api/posts/{postId}/comments", postId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[1].depth").value(1))
        .andExpect(jsonPath("$.content[1].parentCommentId").value(parentId));
  }

  @Test
  void 대댓글에는_답글을_달_수_없다() throws Exception {
    Long parentId = 댓글을_만든다("원댓글", null);
    Long replyId = 댓글을_만든다("답글", parentId);

    mockMvc
        .perform(
            post("/api/posts/{postId}/comments", postId)
                .with(로그인(AUTHOR_SUBJECT))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"답답글\",\"parentCommentId\":%d}".formatted(replyId)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMENT_DEPTH_EXCEEDED"));
  }

  @Test
  void 존재하지_않는_게시글에는_댓글을_달_수_없다() throws Exception {
    mockMvc
        .perform(
            post("/api/posts/{postId}/comments", 999_999L)
                .with(로그인(AUTHOR_SUBJECT))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"댓글\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
  }

  @Test
  void 본문이_비면_400이다() throws Exception {
    mockMvc
        .perform(
            post("/api/posts/{postId}/comments", postId)
                .with(로그인(AUTHOR_SUBJECT))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.body").exists());
  }

  @Test
  void 작성자만_수정하고_삭제할_수_있다() throws Exception {
    Long id = 댓글을_만든다("수정 대상", null);

    mockMvc
        .perform(
            patch("/api/comments/{id}", id)
                .with(로그인(OTHER_SUBJECT))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"탈취\"}"))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            patch("/api/comments/{id}", id)
                .with(로그인(AUTHOR_SUBJECT))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"수정됨\"}"))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(delete("/api/comments/{id}", id).with(로그인(AUTHOR_SUBJECT)).with(csrf()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/posts/{postId}/comments", postId))
        .andExpect(jsonPath("$.totalElements").value(0));
  }
}
