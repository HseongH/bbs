package com.board.bbs.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.RSAKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SigningKeyConfigTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(SigningKeyConfig.class);

  @Test
  void 키_설정이_없으면_local이_아닌_프로필에서는_시작하지_않는다() {
    runner
        .withPropertyValues("spring.profiles.active=prod")
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .hasMessageContaining("bbs.auth.signing-key-location"));
  }

  @Test
  void local_프로필은_키_설정이_없으면_임시_키로_시작한다() {
    runner
        .withPropertyValues("spring.profiles.active=local")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context.getBean(RSAKey.class).isPrivate()).isTrue();
            });
  }

  @Test
  void 설정한_PEM_키를_읽고_kid는_공개키_thumbprint다(@TempDir Path dir) throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair pair = generator.generateKeyPair();
    Path pem = dir.resolve("signing-key.pem");
    Files.writeString(
        pem,
        "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(pair.getPrivate().getEncoded())
            + "\n-----END PRIVATE KEY-----\n");

    ApplicationContextRunner withKey =
        runner.withPropertyValues(
            "spring.profiles.active=prod", "bbs.auth.signing-key-location=file:" + pem);

    String[] kids = new String[2];
    withKey.run(context -> kids[0] = context.getBean(RSAKey.class).getKeyID());
    withKey.run(
        context -> {
          RSAKey key = context.getBean(RSAKey.class);
          kids[1] = key.getKeyID();
          assertThat(key.toRSAPublicKey()).isEqualTo(pair.getPublic());
          assertThat(key.getKeyID()).isEqualTo(key.computeThumbprint().toString());
        });
    assertThat(kids[0]).isNotBlank().isEqualTo(kids[1]);
  }
}
