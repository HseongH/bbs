package com.board.bbs.member.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.board.bbs.support.IntegrationTestBase;
import com.board.bbs.support.TestInternalTokens;
import com.board.bbs.token.InternalUser;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class MemberControllerTest extends IntegrationTestBase {

  private static final String SUBJECT = "sub-me";

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void 데이터를_준비한다() {
    jdbcTemplate.update("DELETE FROM post_like");
    jdbcTemplate.update("DELETE FROM comment");
    jdbcTemplate.update("DELETE FROM post");
    jdbcTemplate.update("DELETE FROM member");
    jdbcTemplate.update(
        "INSERT INTO member (subject, nickname, email, created_at, updated_at)"
            + " VALUES (?, '나자신', 'me@example.com', now(), now())",
        SUBJECT);
  }

  @Test
  void 로그인한_회원의_정보를_반환한다() throws Exception {
    mockMvc
        .perform(get("/api/members/me").with(TestInternalTokens.bearer(SUBJECT)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nickname").value("나자신"))
        .andExpect(jsonPath("$.email").value("me@example.com"));
  }

  @Test
  void 미인증_요청은_401을_반환한다() throws Exception {
    mockMvc
        .perform(get("/api/members/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void 처음_보는_사용자는_첫_요청에서_회원이_생긴다() throws Exception {
    String token =
        TestInternalTokens.issue(
            new InternalUser("sub-new", "새회원", "new@example.com", Set.of("USER")));

    mockMvc
        .perform(get("/api/members/me").with(TestInternalTokens.withToken(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nickname").value("새회원"))
        .andExpect(jsonPath("$.email").value("new@example.com"));

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM member WHERE subject = 'sub-new'", Integer.class))
        .isEqualTo(1);
  }
}
