# 원댓글 단위 댓글 목록 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 댓글 목록을 원댓글과 그 대댓글의 묶음으로 바꾸고, 삭제된 원댓글은 살아 있는 대댓글이 있을 때만 "삭제된 댓글입니다"로 자리를 남긴다.

**Architecture:** 영속성 어댑터가 원댓글 페이지와 대댓글 목록을 쿼리 두 번으로 조회하고, `CommentQueryService`가 원댓글별로 묶어 조회 모델 `CommentThread`를 만든다. 삭제된 댓글의 본문·작성자는 웹 어댑터의 응답 변환에서 가린다. 화면은 묶음 구조를 그대로 그린다.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Data JPA(JPQL), Flyway, Testcontainers(PostgreSQL), Angular 22, Vitest, MSW, openapi-typescript

**Spec:** `docs/features/comment/srs.md` 1.2.0 (CMT-FR-004, 009, 020), `docs/features/comment/sds.md` 1.3.0 (§2.2, §3.3, §4, §6, §7)

## Global Constraints

- COM-IF-005: 목록 API는 오프셋 페이징. 요청은 `page`(0부터), `size`(기본 20). 응답은 `{content, page, size, totalElements, totalPages, last}`.
- COM-NFR-014: 목록 정렬 키에 식별자를 포함해서 페이지 경계에서 행이 겹치거나 빠지지 않는다.
- COM-NFR-012: 삭제된 댓글은 조회·수정·삭제 대상이 아니다 (이번 변경의 예외는 CMT-FR-009의 자리 표시뿐이며, 본문·작성자는 노출하지 않는다).
- COM-IF-006: OpenAPI 문서에서 null이 될 수 없는 응답 필드는 `required`, null이 될 수 있는 필드는 `required`가 아니다.
- COM-NFR-032: API가 바뀌면 `cd frontend && pnpm gen:api` 후 `pnpm typecheck`가 통과해야 한다.
- COM-NFR-030, 031: `./gradlew check`(ArchUnit 포함)가 통과해야 한다. 새 패키지에는 `@NullMarked` `package-info.java`가 필요하다.
- 커밋 메시지는 `type(scope): subject` 형식이다 (커밋 훅이 검사).

## Review Focus

- 삭제된 원댓글의 대댓글이 모두 삭제된 경우: 원댓글이 목록에 나오면 안 된다 (Task 1 테스트).
- 대댓글이 페이지 크기보다 많은 원댓글: 페이지 크기는 원댓글 수 기준이므로 대댓글은 잘리지 않고 모두 나와야 한다 (Task 2 테스트).
- 클라이언트가 `sort=foo` 같은 임의 정렬을 보내는 경우: 무시하고 `200`이어야 한다. 지금은 500이 난다 (Task 2 테스트).
- 화면 상단의 댓글 수: `totalElements`가 원댓글 수로 바뀌므로 그대로 쓰면 숫자가 틀린다. 화면에 보이는 살아 있는 댓글(삭제되지 않은 원댓글 + 대댓글) 수를 보여 줘야 한다 (Task 3 테스트).
- 삭제된 원댓글에 답글 작성: 지금처럼 `404 COMMENT_NOT_FOUND`여야 한다. 기존 QA의 N/T 항목 TC-CMT-008도 이 테스트로 판정한다 (Task 2 테스트).

---

### Task 1: 영속성 — 원댓글 페이지와 대댓글 조회 (CMT-FR-004, CMT-FR-009)

**Files:**
- Create: `src/main/resources/db/migration/V5__add_comment_thread_indexes.sql`
- Modify: `src/main/java/com/board/bbs/comment/adapter/out/persistence/CommentJpaRepository.java`
- Modify: `src/main/java/com/board/bbs/comment/adapter/out/persistence/CommentPersistenceAdapter.java`
- Modify: `src/main/java/com/board/bbs/comment/application/port/out/CommentRepository.java`
- Test: `src/test/java/com/board/bbs/comment/adapter/out/persistence/CommentPersistenceAdapterTest.java`

**Interfaces:**
- Produces (포트 `CommentRepository`에 추가, Javadoc 포함):
  - `Page<Comment> listRoots(PostId postId, Pageable pageable)` — 깊이 0이면서 "삭제되지 않았거나 살아 있는 대댓글이 있는" 댓글. `created_at ASC, id ASC`. `pageable`의 정렬은 무시하고 페이지 번호와 크기만 쓴다.
  - `List<Comment> listRepliesOf(List<CommentId> rootIds)` — 주어진 원댓글들의 삭제되지 않은 대댓글. `created_at ASC, id ASC`. 빈 목록이 오면 쿼리 없이 빈 목록을 반환한다.
- 기존 `listByPost`는 이 Task에서는 남겨 둔다 (Task 2에서 제거).

- [ ] **Step 1: 실패하는 테스트 작성** — `CommentPersistenceAdapterTest`에 추가. 대댓글은 `Comment.write(post, author, body, parent)`로, 삭제는 `jdbcTemplate.update("UPDATE comment SET deleted_at = now() WHERE id = ?", id)`로 만든다.

```java
@Test
void 원댓글_목록에는_대댓글이_포함되지_않는다() {
  // 원댓글 1, 대댓글 1 → listRoots(post, PageRequest.of(0, 10)).getContent()의 본문이 ["원댓글"]
}

@Test
void 삭제된_원댓글은_살아있는_대댓글이_있을_때만_포함된다() {
  // 원댓글 A(대댓글 a1 살아 있음), 원댓글 B(대댓글 b1 삭제됨), 원댓글 C(대댓글 없음). A, B, C 모두 삭제
  // → listRoots 결과의 식별자가 [A]이고, 그 댓글의 isDeleted()가 true
}

@Test
void 원댓글_목록은_작성_순서이고_원댓글_수로_페이지를_나눈다() {
  // 원댓글 첫째, 둘째, 셋째(첫째에 대댓글 2개)
  // → PageRequest.of(0, 2): 본문 ["첫째", "둘째"], totalElements 3
  // → PageRequest.of(1, 2): 본문 ["셋째"]
}

@Test
void 대댓글은_원댓글별로_작성_순서대로_반환되고_삭제된_대댓글은_빠진다() {
  // 원댓글 A에 a1, a2(삭제), a3 / 원댓글 B에 b1
  // → listRepliesOf([A, B])의 본문이 ["a1", "a3", "b1"]
  // → listRepliesOf([])가 빈 목록
}
```

기존 테스트 두 개를 새 메서드로 바꾼다.
- `게시글의_댓글이_한꺼번에_삭제된다`: `listByPost(...)` 대신 `listRoots(post, PageRequest.of(0, 10))`
- `댓글_목록은_작성_순서대로_반환된다`: 위 `원댓글_목록은_작성_순서이고...`와 겹치므로 삭제한다.

- [ ] **Step 2: 실패 확인**

Run: `./gradlew test --tests '*CommentPersistenceAdapterTest'`
Expected: 컴파일 실패 (`listRoots`, `listRepliesOf` 없음)

- [ ] **Step 3: 마이그레이션 작성** — `V5__add_comment_thread_indexes.sql`

```sql
CREATE INDEX idx_comment_roots_by_post
    ON comment (post_id, created_at, id)
    WHERE depth = 0;

CREATE INDEX idx_comment_live_replies
    ON comment (parent_comment_id, created_at, id)
    WHERE deleted_at IS NULL;
```

- [ ] **Step 4: 저장소 쿼리 추가** — `CommentJpaRepository`
  - `Page<CommentJpaEntity> findRootsForListing(@Param("postId") Long postId, Pageable pageable)`: `@Query`로 SDS §3.3 ① 조건을 JPQL로 작성하고 `countQuery`에 같은 조건을 둔다. `EXISTS (SELECT r.id FROM CommentJpaEntity r WHERE r.parentCommentId = c.id AND r.deletedAt IS NULL)`.
  - `List<CommentJpaEntity> findByParentCommentIdInAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(Collection<Long> parentCommentIds)`
  - 기존 `findByPostIdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc`는 Task 2에서 제거한다.

- [ ] **Step 5: 어댑터 구현** — `CommentPersistenceAdapter.listRoots`는 `PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())`로 정렬을 버리고 호출한다. `listRepliesOf`는 빈 입력이면 `List.of()`.

- [ ] **Step 6: 통과 확인**

Run: `./gradlew test --tests '*CommentPersistenceAdapterTest'`
Expected: PASS

- [ ] **Step 7: 커밋**

```bash
git add src/main/resources/db/migration/V5__add_comment_thread_indexes.sql src/main/java/com/board/bbs/comment src/test/java/com/board/bbs/comment/adapter/out/persistence
git commit -m "feat(comment): 원댓글 페이지와 대댓글을 따로 조회한다"
```

---

### Task 2: 애플리케이션과 API — 원댓글 묶음 응답 (CMT-FR-004, CMT-FR-009, CMT-FR-002)

**Files:**
- Create: `src/main/java/com/board/bbs/comment/application/CommentThread.java`
- Create: `src/main/java/com/board/bbs/comment/application/package-info.java` (`@NullMarked`, `post/application/package-info.java`와 같은 형식)
- Create: `src/main/java/com/board/bbs/comment/adapter/in/web/dto/CommentThreadResponse.java`
- Modify: `src/main/java/com/board/bbs/comment/application/service/CommentQueryService.java`
- Modify: `src/main/java/com/board/bbs/comment/adapter/in/web/dto/CommentResponse.java`
- Modify: `src/main/java/com/board/bbs/comment/adapter/in/web/CommentController.java`
- Modify: `CommentRepository`, `CommentPersistenceAdapter`, `CommentJpaRepository` — `listByPost`와 그 파생 쿼리 제거
- Test: `src/test/java/com/board/bbs/comment/adapter/in/web/CommentControllerTest.java`, `src/test/java/com/board/bbs/common/config/OpenApiDocumentTest.java`

**Interfaces:**
- Consumes: Task 1의 `listRoots`, `listRepliesOf`
- Produces:
  - `public record CommentThread(Comment root, List<Comment> replies)` (`com.board.bbs.comment.application`)
  - `CommentQueryService.list(PostId postId, Pageable pageable) → Page<CommentThread>`: 게시글 확인(`postQueryService.getById`) → `listRoots` → 원댓글이 있으면 `listRepliesOf(원댓글 식별자들)` → `parentId`별로 묶어 원댓글 순서대로 `CommentThread` 생성. `Page.map`으로 페이지 정보를 유지한다.
  - `CommentResponse(Long id, Long postId, @Nullable Long authorId, @Nullable String body, boolean deleted, @Nullable Long parentCommentId, int depth, Instant createdAt)`: 삭제된 댓글이면 `authorId`, `body`를 `null`로 채운다.
  - `public record CommentThreadResponse(CommentResponse root, List<CommentResponse> replies)` + `static from(CommentThread)`
  - `GET /api/posts/{postId}/comments` → `PageResponse<CommentThreadResponse>`

- [ ] **Step 1: 실패하는 테스트 작성** — `CommentControllerTest`. 기존 도우미 `댓글을_만든다(body, parentId)`를 쓰고, 원댓글 삭제는 `DELETE /api/comments/{id}`(작성자 로그인, csrf)로 한다.

```java
// 기존 테스트의 기대값을 새 구조로 바꾼다
// 댓글을_작성하고_목록에서_확인할_수_있다: $.content[0].root.body == "첫 댓글", $.content[0].root.deleted == false, $.content[0].replies 길이 0
// 대댓글은_깊이_1로_기록된다: $.content[0].replies[0].depth == 1, $.content[0].replies[0].parentCommentId == parentId

@Test
void 삭제된_원댓글은_본문과_작성자를_가리고_대댓글과_함께_나온다() {
  // 원댓글 → 답글 → 원댓글 삭제
  // $.content[0].root.deleted == true, $.content[0].root.body 가 null, $.content[0].root.authorId 가 null
  // $.content[0].replies[0].body == "답글", $.totalElements == 1
}

@Test
void 대댓글이_없는_삭제된_원댓글은_목록에_나오지_않는다() {
  // 원댓글 → 삭제 → $.totalElements == 0
}

@Test
void 페이지_크기는_원댓글_수_기준이고_대댓글은_잘리지_않는다() {
  // 원댓글 A(답글 2개), 원댓글 B → ?size=1
  // $.totalElements == 2, $.content 길이 1, $.content[0].replies 길이 2
}

@Test
void 정렬_파라미터는_무시한다() {
  // ?sort=foo → 200
}

@Test
void 삭제된_원댓글에는_답글을_달_수_없다() {
  // 원댓글 → 삭제 → 그 원댓글을 부모로 작성 → 404, $.code == "COMMENT_NOT_FOUND"
}
```

`OpenApiDocumentTest`에 추가:

```java
@Test
void 댓글_응답의_가려질_수_있는_필드는_필수가_아니다() {
  // CommentResponse.required 가 body, authorId, parentCommentId 를 포함하지 않고 deleted 를 포함
  // CommentThreadResponse.required 가 root, replies 를 포함
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew test --tests '*CommentControllerTest' --tests '*OpenApiDocumentTest'`
Expected: 새 테스트와 기대값을 바꾼 테스트 실패. `정렬_파라미터는_무시한다`는 500으로 실패.

- [ ] **Step 3: 구현** — Interfaces의 시그니처대로 `CommentThread`, `package-info.java`, `CommentQueryService.list`, `CommentResponse`, `CommentThreadResponse`, 컨트롤러 반환 타입을 바꾸고, `listByPost` 경로(포트·어댑터·파생 쿼리)를 지운다. 공개 타입과 공개 메서드에는 Javadoc을 단다 (Checkstyle).

- [ ] **Step 4: 통과 확인**

Run: `./gradlew check`
Expected: BUILD SUCCESSFUL (ArchUnit, Checkstyle, 커버리지 포함)

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/board/bbs/comment src/test/java/com/board/bbs/comment src/test/java/com/board/bbs/common/config/OpenApiDocumentTest.java
git commit -m "feat(comment): 댓글 목록을 원댓글 묶음으로 반환한다"
```

---

### Task 3: 화면 — 묶음 표시와 삭제된 원댓글 자리 표시 (CMT-FR-020, CMT-FR-022, CMT-FR-023)

**Files:**
- Modify: `frontend/src/app/core/api/schema.d.ts` (생성 파일, 직접 고치지 않는다)
- Modify: `frontend/src/app/features/comment/comment-api.service.ts` — 타입 별칭
- Modify: `frontend/src/app/features/comment/comment.store.ts` — `list`의 응답 타입
- Modify: `frontend/src/app/features/comment/components/comment-section.ts`
- Modify: `frontend/src/app/features/comment/components/comment-item.ts`
- Test: `frontend/src/app/features/comment/components/comment-section.spec.ts`

**Interfaces:**
- Consumes: Task 2의 API (`PageResponseCommentThreadResponse`, `CommentThreadResponse`, `CommentResponse`)
- Produces (`comment-api.service.ts`):
  - `export type Comment = components["schemas"]["CommentResponse"];`
  - `export type CommentThread = components["schemas"]["CommentThreadResponse"];`
  - `export type CommentPage = components["schemas"]["PageResponseCommentThreadResponse"];`

- [ ] **Step 1: API 타입 재생성**

Run: `docker compose up -d` 후 `./gradlew bootRun`을 띄운 상태에서 `cd frontend && pnpm gen:api`
Expected: `schema.d.ts`에 `CommentThreadResponse`, `PageResponseCommentThreadResponse`가 생기고 `CommentResponse`에 `deleted`가 생긴다. 이 시점의 `pnpm typecheck`는 기존 코드 때문에 실패한다 (COM-NFR-032가 의도한 동작).

- [ ] **Step 2: 실패하는 테스트 작성** — `comment-section.spec.ts`. 도우미를 `묶음(root, replies = [])`과 `댓글페이지(threads)`로 바꾸고, `댓글()` 도우미의 기본값에 `deleted: false`를 추가한 뒤 기존 테스트 5개를 새 구조로 옮긴다. 추가할 테스트:

```ts
it("대댓글은 자기 원댓글 바로 아래에 보여준다", ...)
// 묶음(원댓글1, [답글1]), 묶음(원댓글2) → 화면의 댓글 본문 순서가 ["원댓글1", "답글1", "원댓글2"]

it("삭제된 원댓글은 자리만 표시하고 버튼을 보여주지 않는다", ...)
// 로그인한 회원 id 1, 묶음(댓글({ id: 1, deleted: true, body: undefined, authorId: undefined }), [답글])
// "삭제된 댓글입니다"가 보이고, 답글은 보이며, "답글 달기" 버튼은 답글 쪽에도 없다(답글은 깊이 1), 수정·삭제 버튼 없음

it("댓글 수는 화면에 보이는 댓글만 센다", ...)
// 묶음(삭제된 원댓글, [답글]), 묶음(살아 있는 원댓글) → 제목이 "댓글 2"
```

- [ ] **Step 3: 실패 확인**

Run: `cd frontend && pnpm test -- comment-section`
Expected: FAIL

- [ ] **Step 4: 구현**
  - `comment-section`: `@for (thread of ...content; track thread.root.id)`로 원댓글 `app-comment-item`을 그리고, 그 안에서 `@for (reply of thread.replies; track reply.id)`로 대댓글을 그린다. 제목의 숫자는 `computed`로 `삭제되지 않은 원댓글 수 + 대댓글 수`를 계산한다.
  - `comment-item`: `comment().deleted`이면 본문 대신 "삭제된 댓글입니다"를 회색 글씨로 보여 주고, 답글·수정·삭제 버튼을 그리지 않는다. `isMine`은 `authorId`가 `null`이면 `false`.

- [ ] **Step 5: 통과 확인**

Run: `cd frontend && pnpm verify`
Expected: 린트, 타입 검사, 테스트 모두 통과

- [ ] **Step 6: 커밋**

```bash
git add frontend/src
git commit -m "feat(comment): 화면에 원댓글 묶음과 삭제된 원댓글 자리를 표시한다"
```

---

### Task 4: 문서 — QA 판정과 프로젝트 SDS (CMT-FR-004, CMT-FR-009, CMT-FR-020)

**Files:**
- Modify: `docs/features/comment/qa-checklist.md` (1.1.1 → 1.2.0)
- Modify: `docs/project/sds.md` (데이터 표의 `comment` 인덱스, 버전 Minor 올림, 변경 이력)

- [ ] **Step 1: 전체 검증 실행**

Run: `./gradlew check` 와 `cd frontend && pnpm verify`
Expected: 둘 다 통과. 실행 결과(테스트 수 포함)를 QA 수행 정보에 적는다.

- [ ] **Step 2: QA 체크리스트 갱신**
  - TC-CMT-011을 `원댓글_목록은_작성_순서이고_원댓글_수로_페이지를_나눈다`로 바꾼다 (삭제한 기존 테스트를 참조하지 않게).
  - TC-CMT-008(삭제된 댓글에 답글)을 `CommentControllerTest#삭제된_원댓글에는_답글을_달_수_없다`로 판정한다 (N/T → Pass).
  - 새 항목: Task 1의 3개, Task 2의 4개(`삭제된_원댓글은_...`, `대댓글이_없는_...`, `페이지_크기는_...`, `정렬_파라미터는_무시한다`), Task 3의 3개를 CMT-FR-004, 009, 020에 연결해 추가한다. 판정은 Step 1에서 실제로 실행한 결과로 채운다.
  - 추적 요약에 CMT-FR-009를 추가하고 집계와 변경 이력을 갱신한다. 머리말 `related`를 CMT-SRS 1.2.0, CMT-SDS 1.3.0으로.
- [ ] **Step 3: 프로젝트 SDS 갱신** — §5 데이터 표의 `comment` 행에 `idx_comment_roots_by_post`, `idx_comment_live_replies`(V5)를 추가하고 버전과 변경 이력을 올린다.
- [ ] **Step 4: 커밋**

```bash
git add docs
git commit -m "docs(comment): 원댓글 묶음 목록의 QA 판정과 인덱스 반영"
```
