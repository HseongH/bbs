# ADR-0009. 기능 사이의 의존은 이벤트와 공개 유스케이스로 한정한다

- 상태: Accepted (공개 API를 "인바운드 포트"로 정한 부분은 [ADR-0010](0010-drop-inbound-ports.md)으로 대체)
- 일자: 2026-10-08
- 관련: COM-NFR-030, PST-FR-006, CMT-FR-001, PR #4

## 맥락

[ADR-0001](0001-hexagonal-architecture-enforced-by-tests.md)의 계층 규칙은 계층 사이의 방향만 검사한다. 그래서 기능끼리 서로의 내부를 쓰거나 순환하는 것은 막지 못했다. 기능 경계 규칙을 먼저 추가해 보니 두 개의 순환이 드러났다.

- `post ↔ comment`: 게시글 삭제가 댓글의 아웃바운드 포트를 직접 호출했고, 댓글 작성은 게시글의 아웃바운드 포트로 게시글을 확인했다.
- `common ↔ member`: 인증 연동 클래스가 `common`에 있으면서 `member`를 참조했다.

## 결정

- `FeatureBoundaryTest`를 추가해서 기능 사이의 순환과, 다른 기능의 아웃바운드 포트·어댑터에 대한 의존을 금지한다.
- 다른 기능에는 **도메인 타입**과 **인바운드 포트(유스케이스)**로만 접근한다.
- 게시글 삭제는 `PostDeleted` 도메인 이벤트를 발행하고, `comment`의 `PostDeletedListener`가 받아 댓글을 삭제한다. 리스너는 동기 `@EventListener`이므로 게시글 삭제와 같은 트랜잭션에서 함께 반영되거나 함께 취소된다.
- 인증 연동 클래스(`BbsOidcUserService`, `CurrentMemberArgumentResolver`)는 `member/adapter/in/` 아래로 옮긴다. `@CurrentMember` 어노테이션은 모든 기능이 쓰므로 `common`에 남긴다.
- 예외: `*QueryRepository`는 읽기 쿼리를 위해 다른 기능의 QueryDSL 메타모델을 조인할 수 있다.

## 검토한 대안

| 대안 | 기각 이유 |
|---|---|
| 순환을 그대로 두고 규칙을 추가하지 않음 | 기능을 따로 이해하거나 떼어 낼 수 없게 된다 |
| 비동기 이벤트 (`@TransactionalEventListener(AFTER_COMMIT)` + `@Async`) | 게시글은 삭제되었는데 댓글은 남는 상태가 생긴다. 현재 규모에서 최종 일관성을 감수할 이유가 없다 |
| 이벤트 발행을 아웃바운드 포트로 감싸기 | 애플리케이션 계층이 이미 `@Transactional` 등 스프링에 의존하므로, `ApplicationEventPublisher`를 직접 쓰는 편이 실용적이라고 판단했다 |

## 결과

- 좋은 점: 기능 사이의 의존이 한 방향이 되었다. `post`는 `comment`의 존재를 모른다.
- 나쁜 점: 게시글 삭제와 댓글 삭제의 연결이 코드에서 직접 보이지 않는다. 이 문서와 `PostDeletionIntegrationTest`가 연결을 설명하고 검증한다.
- 후속: 인바운드 포트 정리 작업이 예정되어 있다. 완료되면 이 ADR의 "인바운드 포트로 접근" 부분을 새 ADR로 대체한다.
