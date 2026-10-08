# ADR-0011. Lombok을 쓰지 않는다

- 상태: Accepted
- 일자: 2026-10-08
- 관련: PR #6, [ADR-0002](0002-separate-domain-and-jpa-entity.md)(Lombok 관련 문장 대체)

## 맥락

생성자 주입(`@RequiredArgsConstructor`)과 getter(`@Getter`)를 줄이기 위해 Lombok을 썼다. 그러나 Lombok은 공개 API가 아니라 컴파일러 내부에 의존하므로 JDK가 올라갈 때마다 깨질 위험이 있고, 실제로 빌드할 때마다 `sun.misc.Unsafe` 제거 예고 경고를 냈다. 사용처 대부분은 생성자 주입이라 직접 써도 부담이 적었다.

## 결정

- Lombok을 제거하고 생성자와 getter를 직접 작성한다.
- 스프링만 호출하는 생성자는 `package-private`으로 둔다. 공개할 이유가 없고, 공개 생성자마다 형식적인 Javadoc을 달지 않아도 된다.
- JPA 엔티티는 같은 패키지의 매퍼만 쓰므로 getter를 `package-private`으로 두고, 쓰이지 않는 getter는 만들지 않는다.

## 검토한 대안

| 대안 | 기각 이유 |
|---|---|
| Lombok 유지 | JDK 업그레이드마다 호환성 위험. 경고가 계속 출력된다 |
| Java `record`로 대체 | DTO와 값 객체는 이미 `record`다. 서비스와 엔티티는 `record`로 만들 수 없다 |

## 결과

- 좋은 점: 컴파일러 내부 의존이 사라져 JDK 업그레이드가 안전해진다. 생성되는 코드가 눈에 보인다.
- 나쁜 점: 생성자와 getter 코드가 늘어난다.
