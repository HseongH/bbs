# ADR-0002. 도메인 모델과 JPA 엔티티를 분리한다

- 상태: Accepted (Lombok 관련 문장은 [ADR-0011](0011-remove-lombok.md)으로 대체)
- 일자: 2026-09-18
- 관련: [ADR-0001](0001-hexagonal-architecture-enforced-by-tests.md), [프로젝트 SDS §5.3](../sds.md#53-도메인-모델과-영속성-모델의-분리)

## 맥락

JPA 엔티티를 도메인 모델로 함께 쓰면 도메인 클래스가 `jakarta.persistence`에 의존하고, 기본 생성자, 프록시, 지연 로딩 같은 JPA의 요구 사항이 도메인 설계를 제약한다. ADR-0001의 "도메인은 프레임워크를 모른다" 규칙도 지킬 수 없다.

## 결정

- 도메인 객체(`Post`, `Comment`, `Member`)와 JPA 엔티티(`*JpaEntity`)를 별도 클래스로 둔다.
- 영속성 어댑터 안의 매퍼(`*Mapper`)가 둘을 변환한다.
- 도메인 객체는 `write()`/`provision()`(신규, 식별자 없음)과 `restore()`(영속 상태 복원) 팩토리만 가진다.
- JPA 엔티티에는 `@Data`, Lombok `@EqualsAndHashCode`를 쓰지 않는다 (프록시, 지연 로딩과 충돌).

## 검토한 대안

| 대안 | 기각 이유 |
|---|---|
| JPA 엔티티를 도메인 모델로 사용 | 도메인이 JPA에 묶인다. ArchUnit 규칙과 정면으로 충돌한다 |
| JPA 대신 jOOQ나 JDBC 사용 | 학습 목표에 실무에서 많이 쓰는 JPA 경험이 포함되어 있다 |

## 결과

- 좋은 점: 도메인 테스트에 스프링 컨텍스트가 필요 없다. 영속성 기술을 바꿔도 도메인이 영향을 받지 않는다.
- 나쁜 점: 매퍼 코드가 늘어난다. JPA의 변경 감지(dirty checking)를 쓰지 않고 명시적으로 `save()`해야 한다.
- 주의: 도메인 객체 전체를 엔티티로 변환해 저장하므로, 저장하지 않으려는 컬럼(카운터)은 엔티티에서 `updatable = false`로 막아야 한다. 이 문제는 [ADR-0004](0004-atomic-counter-update.md)에서 실제로 결함으로 드러났다.
