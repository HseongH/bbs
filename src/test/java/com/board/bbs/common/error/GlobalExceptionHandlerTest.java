package com.board.bbs.common.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.board.bbs.member.application.port.out.LoadMemberPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestController.class})
class GlobalExceptionHandlerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private LoadMemberPort loadMemberPort;

  @RestController
  static class TestController {
    @GetMapping("/test/business-error")
    String businessError() {
      throw new BusinessException(ErrorCode.POST_NOT_FOUND);
    }

    @GetMapping("/test/no-resource")
    String noResource() throws NoResourceFoundException {
      throw new NoResourceFoundException(HttpMethod.GET, "/test/no-resource", "no-resource");
    }
  }

  @Test
  void 비즈니스_예외는_ProblemDetail_형식으로_변환된다() throws Exception {
    mockMvc
        .perform(get("/test/business-error"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"))
        .andExpect(jsonPath("$.detail").exists());
  }

  @Test
  void 스프링이_던진_예외는_원래_상태코드를_유지한다() throws Exception {
    mockMvc
        .perform(get("/test/no-resource"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }
}
