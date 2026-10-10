package com.board.bbs.support;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** 통합 테스트가 auth의 공개키 대신 테스트 키의 공개키로 내부 토큰을 검증하게 한다. 검증기 자체는 운영과 같다. */
@TestConfiguration(proxyBeanMethods = false)
public class TestInternalTokenKeys {

  @Bean
  @Primary
  JWKSource<SecurityContext> testInternalTokenKeys() {
    return new ImmutableJWKSet<>(new JWKSet(TestInternalTokens.KEY.toPublicJWK()));
  }
}
