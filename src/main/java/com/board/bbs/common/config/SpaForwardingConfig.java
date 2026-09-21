package com.board.bbs.common.config;

import java.io.IOException;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/** 클라이언트 라우팅을 쓰는 화면이 새로고침이나 직접 접근에도 동작하도록 index.html로 넘긴다. */
@Configuration
public class SpaForwardingConfig implements WebMvcConfigurer {

  private static final List<String> 서버가_처리하는_접두사 =
      List.of("api/", "actuator/", "swagger-ui", "v3/api-docs", "oauth2/", "login", "logout");

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/**")
        .addResourceLocations("classpath:/static/")
        .resourceChain(true)
        .addResolver(new SpaResourceResolver());
  }

  private static final class SpaResourceResolver extends PathResourceResolver {

    @Override
    protected @Nullable Resource getResource(String resourcePath, Resource location)
        throws IOException {

      Resource requested = location.createRelative(resourcePath);
      if (requested.exists() && requested.isReadable()) {
        return requested;
      }
      if (서버가_처리하는_접두사.stream().anyMatch(resourcePath::startsWith)) {
        return null;
      }
      Resource index = location.createRelative("index.html");
      return index.exists() && index.isReadable() ? index : null;
    }
  }
}
