package com.board.bbs.common.config;

import com.board.bbs.common.security.CurrentMember;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** API 문서 메타 정보. */
@Configuration
public class OpenApiConfig {

  static {
    SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentMember.class);
  }

  /**
   * OpenAPI 문서 정보를 구성한다.
   *
   * @return OpenAPI 정의
   */
  @Bean
  OpenAPI bbsOpenApi() {
    return new OpenAPI()
        .info(new Info().title("게시판 API").description("헥사고날 아키텍처로 구현한 게시판 REST API").version("v1"));
  }
}
