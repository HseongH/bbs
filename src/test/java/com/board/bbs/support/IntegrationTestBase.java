package com.board.bbs.support;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 실제 PostgreSQL과 Redis 컨테이너를 띄워 두고 통합 테스트를 수행하기 위한 기반 클래스.
 *
 * <p>컨테이너를 정적 필드로 두고 한 번만 기동하여 모든 통합 테스트가 공유한다. JVM 종료 시 Ryuk이 정리하므로 별도의 종료 처리는 두지 않는다.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

  @ServiceConnection
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

  @ServiceConnection
  static final RedisContainer REDIS = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

  static {
    POSTGRES.start();
    REDIS.start();
  }
}
