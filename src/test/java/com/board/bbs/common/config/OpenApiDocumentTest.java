package com.board.bbs.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.board.bbs.support.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@AutoConfigureMockMvc
class OpenApiDocumentTest extends IntegrationTestBase {

  private static final List<String> 내부_인자 = List.of("requester", "author", "memberId", "viewer");

  @Autowired private MockMvc mockMvc;

  private List<String> 모든_파라미터_이름() throws Exception {
    String body =
        mockMvc
            .perform(MockMvcRequestBuilders.get("/v3/api-docs"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    return JsonPath.read(body, "$.paths..parameters[*].name");
  }

  @Test
  void 서버가_채우는_인자는_문서에_노출되지_않는다() throws Exception {
    assertThat(모든_파라미터_이름()).doesNotContainAnyElementsOf(내부_인자);
  }

  @Test
  void 페이지_정보는_개별_파라미터로_평탄화된다() throws Exception {
    List<String> names = 모든_파라미터_이름();

    assertThat(names).doesNotContain("pageable");
    assertThat(names).contains("page", "size");
  }

  @Test
  void 응답_필드는_필수로_표시된다() throws Exception {
    String body =
        mockMvc
            .perform(MockMvcRequestBuilders.get("/v3/api-docs"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    List<String> postResponse = JsonPath.read(body, "$.components.schemas.PostResponse.required");
    assertThat(postResponse)
        .contains("id", "title", "content", "authorId", "viewCount", "likeCount", "createdAt");

    List<String> page =
        JsonPath.read(body, "$.components.schemas.PageResponsePostSummaryResponse.required");
    assertThat(page).contains("content", "page", "size", "totalElements", "totalPages", "last");
  }

  @Test
  void null이_될_수_있는_필드는_필수가_아니다() throws Exception {
    String body =
        mockMvc
            .perform(MockMvcRequestBuilders.get("/v3/api-docs"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    List<String> comment = JsonPath.read(body, "$.components.schemas.CommentResponse.required");
    assertThat(comment).doesNotContain("parentCommentId");
  }
}
