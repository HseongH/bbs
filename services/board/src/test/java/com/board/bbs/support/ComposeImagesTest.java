package com.board.bbs.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ComposeImagesTest {

  private static final String COMPOSE =
      """
      services:
        postgres:
          image: postgres:17-alpine
          environment:
            POSTGRES_DB: bbs
        redis:
          image: redis:7-alpine
      """;

  @Test
  void 서비스의_이미지를_읽는다() {
    assertThat(ComposeImages.parse(COMPOSE, "postgres")).isEqualTo("postgres:17-alpine");
    assertThat(ComposeImages.parse(COMPOSE, "redis")).isEqualTo("redis:7-alpine");
  }

  @Test
  void 없는_서비스를_읽으면_예외가_발생한다() {
    assertThatThrownBy(() -> ComposeImages.parse(COMPOSE, "mysql"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("mysql");
  }

  @Test
  void 저장소의_compose_파일에서_읽는다() {
    assertThat(ComposeImages.of("postgres")).startsWith("postgres:");
  }
}
