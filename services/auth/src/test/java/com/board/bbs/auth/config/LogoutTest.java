package com.board.bbs.auth.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.board.bbs.auth.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class LogoutTest extends IntegrationTestBase {

  @Autowired private MockMvc mockMvc;

  @Test
  void 로그아웃은_토큰과_함께_POST하면_204다() throws Exception {
    mockMvc
        .perform(post("/logout").with(oidcLogin()).with(csrf()))
        .andExpect(status().isNoContent());
  }

  @Test
  void 토큰_없는_로그아웃은_403_ProblemDetail이다() throws Exception {
    mockMvc
        .perform(post("/logout").with(oidcLogin()))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }
}
