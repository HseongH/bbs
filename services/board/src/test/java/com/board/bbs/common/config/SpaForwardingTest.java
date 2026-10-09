package com.board.bbs.common.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.board.bbs.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class SpaForwardingTest extends IntegrationTestBase {

  @Autowired private MockMvc mockMvc;

  @Test
  void 화면_경로는_로그인_없이_접근할_수_있다() throws Exception {
    mockMvc.perform(get("/posts/1")).andExpect(status().isNotFound());
  }

  @Test
  void 화면_진입점도_로그인_없이_접근할_수_있다() throws Exception {
    mockMvc.perform(get("/")).andExpect(status().isNotFound());
  }

  @Test
  void 존재하지_않는_API_경로는_화면을_반환하지_않는다() throws Exception {
    mockMvc
        .perform(get("/api/does-not-exist").with(oidcLogin()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void 미인증_API_요청은_여전히_401이다() throws Exception {
    mockMvc
        .perform(get("/api/members/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void 액추에이터_경로는_포워딩되지_않는다() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }
}
