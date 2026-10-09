package com.board.bbs.auth.config;

import com.board.bbs.token.InternalTokenIssuer;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Clock;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

/**
 * 내부 토큰의 서명 키.
 *
 * <p>설정한 개인키 파일(PKCS#8 PEM)을 쓴다. 설정이 없으면 개발 프로필({@code local})에서만 임시 키를 만들고, 그 밖의 프로필에서는 시작을 거부한다.
 * 운영 환경이 키 없이 떠서 재시작할 때마다 키가 바뀌는 사고를 막기 위해서다 (COM-NFR-008).
 */
@Configuration
public class SigningKeyConfig {

  static final String LOCATION_PROPERTY = "bbs.auth.signing-key-location";

  private static final Logger log = LoggerFactory.getLogger(SigningKeyConfig.class);

  /**
   * 서명 키를 만든다. 키 식별자는 공개키의 thumbprint라서 같은 키면 재시작해도 같다.
   *
   * @param environment 설정과 활성 프로필
   * @return 개인키를 포함한 서명 키
   */
  @Bean
  RSAKey signingKey(Environment environment) throws IOException, GeneralSecurityException {
    String location = environment.getProperty(LOCATION_PROPERTY);
    if (location != null && !location.isBlank()) {
      return load(new DefaultResourceLoader().getResource(location));
    }
    if (environment.matchesProfiles("local")) {
      log.warn("{}이 없어 임시 서명 키를 만듭니다. 재시작하면 키가 바뀝니다.", LOCATION_PROPERTY);
      return generate();
    }
    throw new IllegalStateException(LOCATION_PROPERTY + "에 내부 토큰의 서명 키(PKCS#8 PEM) 위치를 설정해야 합니다.");
  }

  @Bean
  JWKSource<SecurityContext> signingKeys(RSAKey signingKey) {
    return new ImmutableJWKSet<>(new JWKSet(signingKey));
  }

  @Bean
  InternalTokenIssuer internalTokenIssuer(JWKSource<SecurityContext> signingKeys) {
    return new InternalTokenIssuer(signingKeys, Clock.systemUTC());
  }

  private static RSAKey load(Resource resource) throws IOException, GeneralSecurityException {
    String pem = resource.getContentAsString(StandardCharsets.US_ASCII);
    String body =
        pem.replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");
    KeyFactory factory = KeyFactory.getInstance("RSA");
    RSAPrivateCrtKey privateKey =
        (RSAPrivateCrtKey)
            factory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(body)));
    RSAPublicKey publicKey =
        (RSAPublicKey)
            factory.generatePublic(
                new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
    try {
      return new RSAKey.Builder(publicKey).privateKey(privateKey).keyIDFromThumbprint().build();
    } catch (JOSEException e) {
      throw new GeneralSecurityException("서명 키의 식별자를 만들 수 없습니다.", e);
    }
  }

  private static RSAKey generate() throws GeneralSecurityException {
    try {
      return new RSAKeyGenerator(2048).keyIDFromThumbprint(true).generate();
    } catch (JOSEException e) {
      throw new GeneralSecurityException("임시 서명 키를 만들 수 없습니다.", e);
    }
  }
}
