package com.board.bbs.common.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.board.bbs.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class UnauthenticatedResponseTest extends IntegrationTestBase {

  @Autowired private MockMvc mockMvc;

  @Test
  void 미인증_응답은_오류_응답_형식을_따른다() throws Exception {
    mockMvc
        .perform(get("/api/members/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:bbs:error:unauthenticated"))
        .andExpect(jsonPath("$.title").value("Unauthorized"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.detail").value("인증이 필요합니다."))
        .andExpect(jsonPath("$.instance").value("/api/members/me"))
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void 경로에_따옴표가_있어도_응답은_올바른_JSON이다() throws Exception {
    String path = "/api/members/\"quoted\"";
    String encodedPath = "/api/members/%22quoted%22";

    mockMvc
        .perform(
            get("/api/members/x")
                .with(
                    request -> {
                      request.setRequestURI(path);
                      return request;
                    }))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.instance").value(encodedPath))
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }
}
