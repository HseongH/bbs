package com.board.bbs.common.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.board.bbs.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@AutoConfigureMockMvc
class SecurityCsrfTest extends IntegrationTestBase {

  @Autowired private MockMvc mockMvc;

  @Test
  void 로그아웃은_토큰과_함께_POST하면_성공한다() throws Exception {
    mockMvc
        .perform(post("/logout").with(oidcLogin()).with(csrf()))
        .andExpect(status().isNoContent());
  }

  @Test
  void 토큰_없는_변경_요청은_거부된다() throws Exception {
    mockMvc
        .perform(
            post("/api/posts")
                .with(oidcLogin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"제목\",\"content\":\"본문\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void 조회_요청은_토큰이_필요없고_토큰_쿠키를_내려준다() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/api/posts"))
        .andExpect(status().isOk())
        .andExpect(cookie().exists("XSRF-TOKEN"));
  }
}
