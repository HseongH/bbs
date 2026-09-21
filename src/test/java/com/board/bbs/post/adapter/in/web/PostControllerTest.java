package com.board.bbs.post.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class PostControllerTest extends IntegrationTestBase {

  private static final String AUTHOR_SUBJECT = "sub-web-author";
  private static final String OTHER_SUBJECT = "sub-web-other";

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  private Long authorId;

  @BeforeEach
  void 데이터를_준비한다() {
    jdbcTemplate.update("DELETE FROM post_like");
    jdbcTemplate.update("DELETE FROM comment");
    jdbcTemplate.update("DELETE FROM post");
    jdbcTemplate.update("DELETE FROM member");

    authorId = 회원을_만든다(AUTHOR_SUBJECT, "작성자");
    회원을_만든다(OTHER_SUBJECT, "다른사람");
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

  private OidcLoginRequestPostProcessor 로그인(String subject, String... roles) {
    OidcLoginRequestPostProcessor login =
        oidcLogin().idToken(token -> token.subject(subject).claim("preferred_username", subject));
    for (String role : roles) {
      login = login.authorities(new SimpleGrantedAuthority(role));
    }
    return login;
  }

  private Long 게시글을_만든다(String title) throws Exception {
    String location =
        mockMvc
            .perform(
                post("/api/posts")
                    .with(로그인(AUTHOR_SUBJECT))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"%s\",\"content\":\"본문입니다.\"}".formatted(title)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    return Long.valueOf(Objects.requireNonNull(location).replaceAll(".*/", ""));
  }

  @Test
  void 게시글을_작성하면_201과_위치를_반환한다() throws Exception {
    mockMvc
        .perform(
            post("/api/posts")
                .with(로그인(AUTHOR_SUBJECT))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"제목\",\"content\":\"본문\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"));
  }

  @Test
  void 미인증_사용자는_작성할_수_없다() throws Exception {
    mockMvc
        .perform(
            post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"제목\",\"content\":\"본문\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void 제목이_비면_400과_필드_오류를_반환한다() throws Exception {
    mockMvc
        .perform(
            post("/api/posts")
                .with(로그인(AUTHOR_SUBJECT))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"\",\"content\":\"본문\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.errors.title").exists());
  }

  @Test
  void 게시글을_조회하면_조회수가_올라간다() throws Exception {
    Long id = 게시글을_만든다("조회수 확인");

    mockMvc
        .perform(get("/api/posts/{id}", id).with(로그인(OTHER_SUBJECT)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("조회수 확인"))
        .andExpect(jsonPath("$.authorId").value(authorId));

    Long viewCount =
        jdbcTemplate.queryForObject("SELECT view_count FROM post WHERE id = ?", Long.class, id);
    assertThat(viewCount).isEqualTo(1);
  }

  @Test
  void 존재하지_않는_게시글은_404다() throws Exception {
    mockMvc
        .perform(get("/api/posts/{id}", 999_999L))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
  }

  @Test
  void 목록은_비로그인으로도_조회할_수_있다() throws Exception {
    게시글을_만든다("첫 글");
    게시글을_만든다("둘째 글");

    mockMvc
        .perform(get("/api/posts").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.content[0].authorNickname").value("작성자"));
  }

  @Test
  void 작성자는_수정할_수_있고_다른_사람은_할_수_없다() throws Exception {
    Long id = 게시글을_만든다("수정 대상");

    mockMvc
        .perform(
            patch("/api/posts/{id}", id)
                .with(로그인(AUTHOR_SUBJECT))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"바뀐 제목\",\"content\":\"바뀐 본문\"}"))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            patch("/api/posts/{id}", id)
                .with(로그인(OTHER_SUBJECT))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"탈취\",\"content\":\"탈취\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }

  @Test
  void 관리자는_다른_사람의_글을_삭제할_수_있다() throws Exception {
    Long id = 게시글을_만든다("관리자 삭제 대상");

    mockMvc
        .perform(delete("/api/posts/{id}", id).with(로그인(OTHER_SUBJECT, "ROLE_ADMIN")))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/posts/{id}", id)).andExpect(status().isNotFound());
  }

  @Test
  void 좋아요는_한_번만_가능하고_취소할_수_있다() throws Exception {
    Long id = 게시글을_만든다("좋아요 대상");

    mockMvc
        .perform(post("/api/posts/{id}/likes", id).with(로그인(OTHER_SUBJECT)))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(post("/api/posts/{id}/likes", id).with(로그인(OTHER_SUBJECT)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ALREADY_LIKED"));
    mockMvc
        .perform(delete("/api/posts/{id}/likes", id).with(로그인(OTHER_SUBJECT)))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(delete("/api/posts/{id}/likes", id).with(로그인(OTHER_SUBJECT)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("NOT_LIKED"));
  }
}
