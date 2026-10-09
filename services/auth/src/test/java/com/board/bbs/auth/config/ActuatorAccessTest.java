package com.board.bbs.auth.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.board.bbs.auth.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class ActuatorAccessTest extends IntegrationTestBase {

  @Autowired private MockMvc mockMvc;

  @Test
  void 상태_확인과_정보는_인증_없이_볼_수_있다() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
  }

  @Test
  void 지표는_인증_없이_볼_수_없다() throws Exception {
    mockMvc
        .perform(get("/actuator/metrics"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
        .andExpect(jsonPath("$.type").value("urn:bbs:error:unauthenticated"))
        .andExpect(jsonPath("$.instance").value("/actuator/metrics"));
  }

  @Test
  void 지표는_일반_회원이_볼_수_없다() throws Exception {
    mockMvc
        .perform(
            get("/actuator/metrics")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }

  @Test
  void 지표는_관리자가_볼_수_있다() throws Exception {
    mockMvc
        .perform(
            get("/actuator/metrics")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(status().isOk());
  }
}
