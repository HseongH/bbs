package com.board.bbs.auth.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.board.bbs.auth.support.IntegrationTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 로그아웃.
 *
 * <p>CSRF 토큰은 테스트 도구({@code csrf()}) 대신 브라우저처럼 쿠키와 헤더로 직접 보낸다. 그 도구는 공유 컨텍스트의 CSRF 저장소를 세션 기반으로
 * 바꿔치기해서, 같은 컨텍스트를 쓰는 다른 테스트의 쿠키 기반 CSRF 판정을 깨뜨린다.
 */
@AutoConfigureMockMvc
class LogoutTest extends IntegrationTestBase {

  @Autowired private MockMvc mockMvc;

  @Test
  void 로그아웃은_토큰과_함께_POST하면_204다() throws Exception {
    mockMvc
        .perform(
            post("/logout")
                .with(oidcLogin())
                .cookie(new Cookie("XSRF-TOKEN", "csrf-token"))
                .header("X-XSRF-TOKEN", "csrf-token"))
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
