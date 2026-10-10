package com.board.bbs.common.config;

import com.board.bbs.token.InternalTokenDecoders;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;
import java.net.MalformedURLException;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * 내부 토큰 검증.
 *
 * <p>board가 믿는 것은 auth가 서명한 내부 토큰뿐이다. 외부 IdP의 주소나 토큰 형식은 모른다 (ADR-0016). 공개키는 처음 검증할 때 가져오므로 board가
 * auth보다 먼저 떠도 되고, 모르는 키 식별자가 오면 다시 가져오므로 auth의 키가 바뀌어도 재시작할 필요가 없다.
 */
@Configuration
public class InternalTokenConfig {

  @Bean
  JWKSource<SecurityContext> internalTokenKeys(
      @Value("${bbs.internal-token.jwk-set-uri}") String jwkSetUri) throws MalformedURLException {
    return JWKSourceBuilder.create(URI.create(jwkSetUri).toURL()).build();
  }

  @Bean
  JwtDecoder jwtDecoder(JWKSource<SecurityContext> internalTokenKeys) {
    return InternalTokenDecoders.create(internalTokenKeys);
  }
}
