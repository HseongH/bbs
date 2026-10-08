# ADR-0006. QueryDSL은 openfeign 포크를 쓴다

- 상태: Accepted
- 일자: 2026-09-18
- 관련: PST-FR-008

## 맥락

게시글 검색은 키워드와 작성자 조건이 선택적으로 붙는 동적 쿼리다. QueryDSL을 쓰기로 했지만, `com.querydsl:querydsl-jpa`의 최신 버전(5.1.0)은 Hibernate 6을 대상으로 빌드되어 있고, Spring Boot 4.1은 Hibernate 7을 사용한다.

## 결정

Hibernate 7을 대상으로 유지보수되는 포크 `io.github.openfeign.querydsl:querydsl-jpa` 7.0을 사용한다.

## 검토한 대안

| 대안 | 기각 이유 |
|---|---|
| 원본 QueryDSL 5.1.0 | Hibernate 7에서 런타임에 깨진다 |
| JPA Criteria API | 타입 안정성은 있지만 코드가 장황하다 |
| Spring Data JPA Specification | 프로젝션과 조인(작성자 닉네임)을 표현하기 번거롭다 |
| JPQL 문자열 조립 | 컴파일 시점 검증이 없다 |

## 결과

- 좋은 점: 타입 안전한 동적 쿼리를 쓸 수 있다.
- 나쁜 점: 원본 프로젝트가 아닌 포크에 의존한다. 포크의 유지보수가 중단되면 다시 결정해야 한다.
- 재검토 조건: 원본 QueryDSL이 Hibernate 7을 지원하는 버전을 내면 전환을 검토한다.
