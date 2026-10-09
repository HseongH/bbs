package com.board.bbs.common.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.board.bbs.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 토큰 쿠키 발급만 따로 검증한다.
 *
 * <p>{@code SecurityMockMvcRequestPostProcessors.csrf()}는 공유 컨텍스트의 필터 체인에 들어 있는 CSRF 저장소를 세션 기반
 * 저장소로 바꿔치기하고 되돌리지 않는다. 같은 컨텍스트를 쓰는 다른 테스트가 먼저 그것을 호출하면 쿠키가 나올 수 없으므로, 오염되지 않은 컨텍스트에서 혼자 돈다.
 */
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class CsrfCookieIssuanceTest extends IntegrationTestBase {

  @Autowired private MockMvc mockMvc;

  @Test
  void 조회_요청은_토큰이_필요없고_토큰_쿠키를_내려준다() throws Exception {
    mockMvc
        .perform(get("/api/posts"))
        .andExpect(status().isOk())
        .andExpect(cookie().exists("XSRF-TOKEN"));
  }
}
