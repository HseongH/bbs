package com.board.bbs.auth.support;

import com.redis.testcontainers.RedisContainer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.utility.DockerImageName;

/**
 * 세션 저장소인 Valkey(Redis 호환) 컨테이너를 띄워 두고 통합 테스트를 수행하기 위한 기반 클래스.
 *
 * <p>이미지는 개발용 deploy/compose.yaml과 같은 것을 쓴다. Keycloak은 띄우지 않는다. 로그인 상태는 테스트 도구로 만든다. 서명 키는 저장소에
 * 개인키를 두지 않도록 실행할 때 임시 파일로 만든다.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

  @ServiceConnection
  static final RedisContainer REDIS =
      new RedisContainer(DockerImageName.parse(ComposeImages.of("valkey")));

  private static final Path SIGNING_KEY = writeSigningKey();

  static {
    REDIS.start();
  }

  @DynamicPropertySource
  static void signingKey(DynamicPropertyRegistry registry) {
    registry.add("bbs.auth.signing-key-location", () -> SIGNING_KEY.toUri().toString());
  }

  private static Path writeSigningKey() {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      String body =
          Base64.getMimeEncoder(64, "\n".getBytes())
              .encodeToString(generator.generateKeyPair().getPrivate().getEncoded());
      Path file = Files.createTempFile("auth-signing-key", ".pem");
      file.toFile().deleteOnExit();
      return Files.writeString(
          file, "-----BEGIN PRIVATE KEY-----\n" + body + "\n-----END PRIVATE KEY-----\n");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
