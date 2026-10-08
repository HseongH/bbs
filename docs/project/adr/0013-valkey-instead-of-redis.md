# ADR-0013. 세션과 조회수 키 저장소로 Redis 대신 Valkey를 쓴다

- 상태: Accepted
- 일자: 2026-10-09
- 관련: PR #15, COM-CON-002, [ADR-0008](0008-oidc-bff-and-redis-session.md)(저장소 제품 부분 대체), [ADR-0005](0005-database-decides-duplicates.md)(조회수 키 저장소 부분 대체)

## 맥락

세션([ADR-0008](0008-oidc-bff-and-redis-session.md))과 조회수 중복 판정 키([ADR-0005](0005-database-decides-duplicates.md))를 Redis 7에 저장했다. Redis는 8부터 라이선스가 RSALv2·SSPLv1·AGPLv3로 바뀌었다. Dependabot이 compose 이미지를 올리면 이 라이선스를 매번 따져야 한다.

## 결정

- 로컬·테스트 환경의 저장소를 Redis 프로토콜과 호환되는 BSD 라이선스 포크 **Valkey**(`valkey/valkey:9-alpine`)로 바꾼다.
- 애플리케이션은 Spring Data Redis와 Spring Session Data Redis로 그대로 접속한다. 코드와 설정의 이름(`spring.data.redis`, `RedisViewDeduplicationAdapter`)은 프로토콜 이름이므로 바꾸지 않는다.
- 스프링 부트의 Docker Compose 지원은 이미지 이름으로 Valkey를 알아보지 못하므로, compose 서비스에 `org.springframework.boot.service-connection: redis` 레이블을 단다.
- 통합 테스트도 `compose.yaml`에서 같은 이미지를 읽는다([ADR-0012](0012-version-catalog-and-dependabot.md)).

## 검토한 대안

| 대안 | 기각 이유 |
|---|---|
| Redis 7에 고정 | 보안 업데이트가 끝나면 결국 바꿔야 한다. Dependabot 업데이트를 계속 거절해야 한다 |
| Redis 8 이상 사용 | 학습 프로젝트라 문제는 없지만, 실무로 옮길 때마다 라이선스 검토가 필요하다 |
| KeyDB, Dragonfly 등 다른 호환 서버 | 호환 범위와 유지보수 주체를 따로 확인해야 한다. Valkey는 Linux Foundation이 관리하고 Redis 7.2와 호환된다 |

## 결과

- 좋은 점: 라이선스 검토 없이 업데이트를 받을 수 있다. 애플리케이션 코드는 바뀌지 않았다.
- 나쁜 점: 문서와 코드의 이름이 어긋난다. 문서에서는 "Valkey (Redis 호환)"로 함께 적어 혼동을 줄인다.
- 재검토 조건: Valkey와 Redis 프로토콜의 호환성이 깨지는 기능을 써야 할 때.
