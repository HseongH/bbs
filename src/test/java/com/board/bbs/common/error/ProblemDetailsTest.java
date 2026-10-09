package com.board.bbs.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProblemDetailsTest {

  @Test
  void 올바른_경로는_그대로_쓴다() {
    assertThat(ProblemDetails.instanceOf("/api/posts/1").toString()).isEqualTo("/api/posts/1");
  }

  @Test
  void 경로에서_허용되지_않는_문자만_인코딩한다() {
    assertThat(ProblemDetails.instanceOf("/api/\"x\" y").toString()).isEqualTo("/api/%22x%22%20y");
    assertThat(ProblemDetails.instanceOf("/api/한").toString()).isEqualTo("/api/%ED%95%9C");
  }

  @Test
  void 이미_인코딩된_문자는_다시_인코딩하지_않는다() {
    assertThat(ProblemDetails.instanceOf("/api/a%20\"").toString()).isEqualTo("/api/a%20%22");
  }

  @Test
  void 인코딩_형식이_아닌_퍼센트_기호는_인코딩한다() {
    assertThat(ProblemDetails.instanceOf("/api/a%zz").toString()).isEqualTo("/api/a%25zz");
  }
}
