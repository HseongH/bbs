# 아키텍처 결정 기록 (ADR)

프로젝트 전체에 영향을 주는 설계 결정을 한 건씩 기록한다. 형식은 Michael Nygard의 ADR을 따른다.

## 규칙

- 파일 이름은 `NNNN-<결정-요약>.md`이다. 번호는 순서대로 부여하고 재사용하지 않는다.
- **승인된 ADR은 고치지 않는다.** 결정을 바꾸려면 새 ADR을 쓰고, 이전 ADR의 상태를 `Superseded by ADR-NNNN`으로만 바꾼다. 결정이 바뀐 이력이 그대로 남아야 하기 때문이다.
- 결정의 **일부만** 바뀌었다면 이전 ADR의 상태에 "(… 부분은 ADR-NNNN으로 대체)"를 덧붙인다. 본문은 고치지 않는다.
- 상태는 `Proposed` → `Accepted` → (`Deprecated` | `Superseded by ADR-NNNN`) 순서로 바뀐다.
- 기능 하나 안에서 끝나는 결정은 ADR이 아니라 그 기능 SDS의 "설계 결정" 절에 적는다.

## 목록

| 번호 | 제목 | 상태 | 일자 |
|---|---|---|---|
| [0001](0001-hexagonal-architecture-enforced-by-tests.md) | 헥사고날 아키텍처를 쓰고 테스트로 강제한다 | Accepted (일부 0010으로 대체) | 2026-09-18 |
| [0002](0002-separate-domain-and-jpa-entity.md) | 도메인 모델과 JPA 엔티티를 분리한다 | Accepted (일부 0011로 대체) | 2026-09-18 |
| [0003](0003-authorization-in-domain.md) | 소유권 검사는 도메인에 둔다 | Accepted | 2026-09-18 |
| [0004](0004-atomic-counter-update.md) | 카운터는 원자적 UPDATE로만 바꾼다 | Accepted | 2026-10-08 |
| [0005](0005-database-decides-duplicates.md) | 중복 판정은 저장소에 맡긴다 | Accepted (일부 0013으로 대체) | 2026-09-18 |
| [0006](0006-querydsl-openfeign-fork.md) | QueryDSL은 openfeign 포크를 쓴다 | Accepted | 2026-09-18 |
| [0007](0007-flyway-single-source-of-schema.md) | 스키마는 Flyway가 유일한 출처다 | Accepted | 2026-09-18 |
| [0008](0008-oidc-bff-and-redis-session.md) | 인증은 OIDC BFF 방식, 세션은 Redis에 둔다 | Accepted (일부 0013으로 대체) | 2026-09-18 |
| [0009](0009-feature-boundaries-via-events.md) | 기능 사이의 의존은 이벤트와 공개 유스케이스로 한정한다 | Accepted (일부 0010으로 대체) | 2026-10-08 |
| [0010](0010-drop-inbound-ports.md) | 인바운드 포트를 두지 않고 아웃바운드 포트는 애그리게이트마다 하나로 한다 | Accepted | 2026-10-08 |
| [0011](0011-remove-lombok.md) | Lombok을 쓰지 않는다 | Accepted | 2026-10-08 |
| [0012](0012-version-catalog-and-dependabot.md) | 의존성 버전은 version catalog 한 곳에서 관리하고 Dependabot으로 갱신한다 | Accepted | 2026-10-08 |
| [0013](0013-valkey-instead-of-redis.md) | 세션과 조회수 키 저장소로 Redis 대신 Valkey를 쓴다 | Accepted | 2026-10-09 |

일자는 결정이 코드에 반영된 날이다. 이 기록들은 2026-10-09에 기존 설계 문서, README, PR 설명을 근거로 역작성했다.
