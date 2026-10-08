package com.board.bbs.member.adapter.in.web;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 컨트롤러가 {@code @CurrentMember}로 현재 회원을 받을 수 있게 인자 해석기를 등록한다. */
@Configuration
@RequiredArgsConstructor
public class CurrentMemberWebConfig implements WebMvcConfigurer {

  private final CurrentMemberArgumentResolver currentMemberArgumentResolver;

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(currentMemberArgumentResolver);
  }
}
