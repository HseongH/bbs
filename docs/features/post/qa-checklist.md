---
doc_id: PST-QA
title: 게시글 QA 체크리스트
version: 1.5.0
status: In Review
owner: HseongH
reviewers: []
approved_date:
last_updated: 2026-10-09
related: [PRJ-QA 1.4.0, PST-SRS 1.4.0, PST-SDS 1.4.0]
---

# 게시글 QA 체크리스트

> 작성 규칙과 판정 기준은 [공통 QA 기준](../../project/qa-standards.md)을 따른다. 자동 테스트 이름은 `클래스#메서드` 형식이다.

## 1. 수행 정보

| 항목 | 값 |
|---|---|
| 대상 커밋 | `feature/auth-service` 브랜치의 7ad3046 (이전 수행: `fix/review-defects`, e96a878, f46a99c, 85cce67) |
| 수행일 | 2026-10-10 |
| 백엔드 자동 검증 | 루트에서 `./gradlew test --rerun check` 성공 (board 테스트 140개, 실패 0, 오류 0, 건너뜀 0. 컨트롤러 테스트는 내부 토큰으로 인증한다. 이 문서가 인용한 테스트가 모두 이번 실행 결과에 Pass로 있는 것을 대조함) |
| 화면 자동 검증 | `main`의 `feat/web-route-titles`에서 `pnpm verify` 성공 (린트, 타입 검사, 빌드 통과, 테스트 파일 13개·테스트 56개 통과). 화면만 바뀌어 백엔드는 다시 실행하지 않았다. 이전 수행의 `pnpm gen:api` 후 생성 타입 변화 없음 |
| E2E | `pnpm e2e` 성공 (Chromium, 9개 통과). `deploy/compose.yaml`의 컨테이너(진입점 포함), auth와 board의 `bootRun`을 띄우고 진입점 `localhost:8000`으로 2026-10-10 수행 |
| 수동 검증 | 실행하지 않음 (N/T) |

## 2. 테스트 항목

### 2.1 작성과 조회

| TC | 요구사항 | 시나리오 | 기대 결과 | 검증 수단 | 판정 |
|---|---|---|---|---|---|
| TC-PST-001 | PST-FR-001 | 회원이 올바른 제목·본문으로 작성 | `201`, `Location` 헤더 | `PostControllerTest#게시글을_작성하면_201과_위치를_반환한다` | Pass |
| TC-PST-002 | PST-FR-001 | 새로 작성한 게시글의 카운터 | 조회수 0, 좋아요 0 | `PostTest#게시글을_작성하면_조회수와_좋아요는_0에서_시작한다` | Pass |
| TC-PST-003 | PST-FR-001, COM-NFR-002 | 비회원이 작성 요청 | `401 UNAUTHENTICATED` | `PostControllerTest#미인증_사용자는_작성할_수_없다` | Pass |
| TC-PST-004 | PST-FR-001, COM-IF-004 | 빈 제목으로 작성 | `400`, `errors.title` | `PostControllerTest#제목이_비면_400과_필드_오류를_반환한다` | Pass |
| TC-PST-005 | PST-FR-001 | 제목 경계값: 공백, 101자, 앞뒤 공백 | 공백·101자 거부, 앞뒤 공백 제거 | `PostTest#제목은_비어있을_수_없다`, `#제목은_100자를_넘을_수_없다`, `#제목의_앞뒤_공백은_제거된다` | Pass |
| TC-PST-006 | PST-FR-001 | 본문 경계값: 공백, 10,001자 | 거부 | `PostTest#본문은_비어있을_수_없다`, `#본문은_10000자를_넘을_수_없다` | Pass |
| TC-PST-007 | PST-FR-002 | 존재하지 않는 게시글 조회 | `404 POST_NOT_FOUND` | `PostControllerTest#존재하지_않는_게시글은_404다` | Pass |
| TC-PST-008 | PST-FR-002, COM-NFR-012 | 삭제된 게시글 조회 | `404` | `PostPersistenceAdapterTest#삭제된_게시글은_읽을_수_없다` | Pass |
| TC-PST-009 | PST-FR-003 | 게시글을 조회한 뒤 다시 조회 | 두 번째 응답의 조회수가 1 올라 있음 | `PostControllerTest#게시글을_조회하면_조회수가_올라간다` | Pass |
| TC-PST-010 | PST-FR-003 | 같은 회원이 24시간 안에 다시 조회 | 조회수가 더 오르지 않음 | `PostViewCountRollbackTest#같은_조회자의_두_번째_조회는_세지_않는다` (24시간 만료 자체는 Valkey TTL 설정 검토) | Pass |
| TC-PST-052 | PST-FR-003 | 조회수 반영이 실패해 롤백된 뒤 같은 회원이 다시 조회 (회귀) | 다시 조회할 때 조회수가 1 오름 | `PostViewCountRollbackTest#조회수_증가가_실패해서_롤백되면_다음_조회에서_다시_센다` | Pass |
| TC-PST-057 | PST-FR-003, COM-NFR-020 | 비회원이 처음 상세 조회 | HttpOnly 조회자 쿠키(`BBS_VIEWER`, UUID) 발급. 세션은 만들지 않음 | `PostControllerTest#비회원에게는_세션_대신_조회자_쿠키를_발급한다` | Pass |
| TC-PST-058 | PST-FR-003 | 비회원이 같은 조회자 쿠키로 다시 조회, 쿠키 없이 한 번 더 조회 | 같은 쿠키의 두 번째 조회는 세지 않음, 쿠키가 없는 조회는 셈 (조회수 2) | `PostControllerTest#비회원이_같은_조회자_쿠키로_다시_조회하면_조회수가_오르지_않는다` | Pass |
| TC-PST-059 | PST-FR-003 | 형식이 틀린 조회자 쿠키(500자)로 조회 | 새 UUID 쿠키 발급 | `PostControllerTest#형식이_틀린_조회자_쿠키는_새로_발급한다` | Pass |
| TC-PST-053 | COM-IF-004 | 식별자가 0인 게시글 조회, 음수 식별자로 좋아요 (회귀) | `400 INVALID_REQUEST` | `PostControllerTest#식별자가_1보다_작으면_400이다` | Pass |

### 2.2 목록과 검색

| TC | 요구사항 | 시나리오 | 기대 결과 | 검증 수단 | 판정 |
|---|---|---|---|---|---|
| TC-PST-011 | PST-FR-004 | 비회원이 목록 조회 | `200` | `PostControllerTest#목록은_비로그인으로도_조회할_수_있다` | Pass |
| TC-PST-012 | PST-FR-004, COM-NFR-012 | 삭제된 게시글이 있는 상태에서 목록 조회 | 삭제된 게시글 제외 | `PostQueryRepositoryTest#삭제된_게시글은_목록에_나오지_않는다` | Pass |
| TC-PST-013 | PST-FR-004 | 목록 항목의 작성자 | 닉네임 포함 | `PostQueryRepositoryTest#작성자_닉네임이_함께_조회된다` | Pass |
| TC-PST-014 | PST-FR-004, COM-NFR-014 | 페이지 크기를 넘는 게시글 | 다음 페이지로 넘어가며 중복·누락 없음 | `PostQueryRepositoryTest#페이지_크기를_넘으면_다음_페이지로_넘어간다` | Pass |
| TC-PST-015 | PST-FR-009 | 키워드가 제목에 포함 | 검색됨 | `PostQueryRepositoryTest#키워드로_제목을_검색할_수_있다` | Pass |
| TC-PST-016 | PST-FR-009 | 키워드가 본문에만 포함 | 검색됨 | `PostQueryRepositoryTest#키워드는_본문도_함께_검색한다` | Pass |
| TC-PST-017 | PST-FR-009 | 공백뿐인 키워드 | 조건 없이 전체 목록 | `PostQueryRepositoryTest#키워드가_공백뿐이면_조건에서_제외된다` | Pass |
| TC-PST-018 | PST-FR-010 | 작성자로 필터링 | 해당 작성자의 글만 | `PostQueryRepositoryTest#작성자로_필터링할_수_있다` | Pass |
| TC-PST-019 | PST-FR-009 | 키워드의 대소문자가 다름 (`Spring` / `spring`) | 검색됨 | 자동 테스트 없음 | N/T |

### 2.3 수정과 삭제

| TC | 요구사항 | 시나리오 | 기대 결과 | 검증 수단 | 판정 |
|---|---|---|---|---|---|
| TC-PST-020 | PST-FR-005 | 작성자가 수정 | 성공 | `PostTest#작성자는_게시글을_수정할_수_있다` | Pass |
| TC-PST-021 | PST-FR-005 | 다른 회원이 수정 | `403 ACCESS_DENIED` | `PostTest#작성자가_아니면_수정할_수_없다`, `PostControllerTest#작성자는_수정할_수_있고_다른_사람은_할_수_없다` | Pass |
| TC-PST-022 | PST-FR-005 | 관리자가 다른 사람의 글을 수정 | `403 ACCESS_DENIED` | 자동 테스트 없음 (도메인 `updateBy`는 관리자 여부를 받지 않으므로 구조상 불가능) | N/T |
| TC-PST-023 | PST-FR-005 | 삭제된 게시글 수정 | `POST_NOT_FOUND` | `PostTest#이미_삭제된_게시글은_수정할_수_없다` | Pass |
| TC-PST-024 | PST-FR-006 | 작성자가 삭제 | 성공 | `PostTest#작성자는_게시글을_삭제할_수_있다` | Pass |
| TC-PST-025 | PST-FR-006 | 다른 회원이 삭제 | `ACCESS_DENIED` | `PostTest#작성자가_아니면_삭제할_수_없다` | Pass |
| TC-PST-026 | PST-FR-006 | 관리자가 다른 사람의 글을 삭제 | `204` | `PostTest#관리자는_다른_사람의_글을_삭제할_수_있다`, `PostControllerTest#관리자는_다른_사람의_글을_삭제할_수_있다` | Pass |
| TC-PST-027 | PST-FR-006 | 이미 삭제된 게시글 삭제 | `POST_NOT_FOUND` | `PostTest#이미_삭제된_게시글은_다시_삭제할_수_없다` | Pass |
| TC-PST-028 | PST-FR-006 | 댓글이 달린 게시글 삭제 | 게시글과 댓글이 모두 삭제됨 | `PostDeletionIntegrationTest#게시글을_삭제하면_게시글과_댓글이_모두_삭제된다` | Pass |
| TC-PST-029 | PST-FR-011 | 게시글을 불러온 뒤 좋아요가 오르고 나서 수정 | 좋아요 수 유지 | `PostCounterPreservationTest#게시글을_수정하는_사이에_오른_좋아요_수가_유지된다` | Pass |
| TC-PST-030 | PST-FR-011 | 게시글을 불러온 뒤 조회수가 오르고 나서 수정 | 조회수 유지 | `PostCounterPreservationTest#게시글을_수정하는_사이에_오른_조회수가_유지된다` | Pass |

### 2.4 좋아요

| TC | 요구사항 | 시나리오 | 기대 결과 | 검증 수단 | 판정 |
|---|---|---|---|---|---|
| TC-PST-031 | PST-FR-007, 008 | 좋아요 → 다시 좋아요 → 취소 | `204` → `409 ALREADY_LIKED` → `204` | `PostControllerTest#좋아요는_한_번만_가능하고_취소할_수_있다` | Pass |
| TC-PST-032 | PST-FR-007, 008 | 좋아요 후 취소 | 좋아요 행 0, 카운터 0 | `PostLikeConcurrencyTest#좋아요를_누르고_취소하면_카운터가_0으로_돌아온다` | Pass |
| TC-PST-033 | PST-NFR-001 | 같은 회원이 동시에 좋아요 여러 번 | 성공 1회, 행 1개, 카운터 1 | `PostLikeConcurrencyTest#동시에_좋아요를_눌러도_한_번만_반영된다` | Pass |
| TC-PST-034 | PST-NFR-001 | 같은 회원이 동시에 취소 여러 번 | 성공 1회, 카운터 0 (음수 아님) | `PostLikeConcurrencyTest#동시에_취소해도_카운터가_음수로_내려가지_않는다` | Pass |
| TC-PST-035 | PST-FR-008 | 좋아요하지 않은 글을 취소 | `409 NOT_LIKED` | 자동 테스트 없음 | N/T |
| TC-PST-036 | PST-FR-007 | 삭제된 게시글에 좋아요 | `404 POST_NOT_FOUND` | 자동 테스트 없음 | N/T |

### 2.5 화면

| TC | 요구사항 | 시나리오 | 기대 결과 | 검증 수단 | 판정 |
|---|---|---|---|---|---|
| TC-PST-040 | PST-FR-020 | 목록 화면 표시 | 제목과 작성자 표시 | `post-list-page.spec: 게시글 제목과 작성자를 보여준다` | Pass |
| TC-PST-041 | PST-FR-020 | 결과가 없는 검색 | 안내 문구 | `post-list-page.spec: 결과가 없으면 안내를 보여준다` | Pass |
| TC-PST-042 | PST-FR-020 | 현재 페이지 버튼 | `aria-current`로 현재 위치 알림 | `post-list-page.spec: 현재 페이지 버튼을 보조기술에 알린다` | Pass |
| TC-PST-043 | PST-FR-020 | 검색 후 새로고침 | URL에 `keyword`가 남고 같은 결과 | E2E `board.spec: 검색 결과가 URL에 남는다` | Pass |
| TC-PST-044 | PST-FR-020 | 공백뿐인 키워드로 검색 | 키워드 없이 요청 | `post.store.spec: 공백뿐인 키워드는 조건에서 빠진다` | Pass |
| TC-PST-045 | PST-FR-021 | 본인 글과 남의 글 상세 | 본인 글에만 수정·삭제 버튼 | `post-detail-page.spec: 작성자 본인에게만…`, `…다른 사람 글에는…` | Pass |
| TC-PST-046 | PST-FR-021 | 없는 글 상세 | 안내 문구 | `post-detail-page.spec: 없는 글이면 안내를 보여준다` | Pass |
| TC-PST-054 | PST-FR-020 | 목록 API가 오류를 응답 (회귀) | 안내 문구, 화면이 깨지지 않음 | `post-list-page.spec: 목록을 불러오지 못하면 안내를 보여준다` | Pass |
| TC-PST-047 | PST-FR-022 | 비로그인으로 작성 화면 진입 | 로그인으로 이동 | `auth.guard.spec: 미인증이면 막고 로그인으로 보낸다` | Pass |
| TC-PST-048 | PST-FR-022 | 수정 화면 진입 | 기존 값이 채워짐 | `post-form.spec: 수정 모드에서는 기존 값이 채워진다` | Pass |
| TC-PST-049 | PST-FR-023 | 서버 검증 오류 | 필드 아래 메시지, 전체 메시지 중복 없음 | `post-form.spec: 서버 검증 오류를…`, `…겹쳐 보여주지 않는다`, `post-edit-page.spec: 저장이 검증 오류로 실패하면 입력란 아래에 서버 메시지를 보여준다` | Pass |
| TC-PST-050 | PST-FR-024 | 좋아요 버튼 클릭 | 숫자 증가, 중복이면 안내 | `like-button.spec` 2건 | Pass |
| TC-PST-051 | PST-FR-001, 021, 024 | 글 작성 → 상세 → 좋아요 | 전체 흐름 성공 | E2E `board.spec: 글을 쓰고 댓글과 좋아요를 남긴다` | Pass |
| TC-PST-055 | PST-FR-025 | 목록·작성·상세·수정·없는 경로로 이동 | 화면마다 `<화면 이름> \| 게시판` 형식의 제목 | `app.spec: / 화면의 브라우저 제목은 게시글 목록 \| 게시판이다` 외 4건 (경로별) | Pass |

## 3. 추적 요약

| 요구사항 | TC | 자동화 |
|---|---|---|
| PST-FR-001 | 001~006 | 완전 |
| PST-FR-002 | 007, 008 | 완전 |
| PST-FR-003 | 009, 010, 052, 057~059 | 완전 (24시간 만료는 설정 검토) |
| PST-FR-004 | 011~014 | 완전 |
| PST-FR-005 | 020~023 | 부분 (관리자 수정 불가는 구조로 보장, 테스트 없음) |
| PST-FR-006 | 024~028 | 완전 |
| PST-FR-007 | 031, 033, 036 | 부분 |
| PST-FR-008 | 031, 032, 034, 035 | 부분 |
| PST-FR-009 | 015~017, 019 | 부분 (대소문자 무시 미검증) |
| PST-FR-010 | 018 | 완전 |
| PST-FR-011 | 029, 030 | 완전 |
| PST-FR-020~025 | 040~051, 054, 055 | 화면 단위 테스트 완전, E2E 2건 |
| PST-NFR-001 | 033, 034 | 완전 |
| PST-NFR-002 | - | 설계 검토로만 확인. 쿼리 횟수 테스트 없음 |

## 4. 결과 요약과 후속 조치

- 판정 근거: 2026-10-10에 `feature/auth-service`에서 `./gradlew test --rerun check`, `pnpm e2e`를 실행한 결과와, `main`의 화면 PR에서 실행한 `pnpm verify` 결과 (§1).
- 집계: 전체 55건 중 Pass 51, Fail 0, N/T 4 (자동 테스트 없음 4).
- TC-PST-057~059는 비회원 조회자 구분을 세션에서 조회자 쿠키로 바꾸면서(PST-SRS 1.4.0) 추가한 항목이다.
- E2E 2건(TC-PST-043, 051)은 1.0.x까지 N/T였다. E2E 브라우저를 Firefox로 바꾸고 실행해 Pass로 판정했다.
- **자동 테스트가 없는 항목**(TC-PST-019, 022, 035, 036)은 테스트를 추가할 후보다. 특히 TC-PST-022는 "관리자도 수정할 수 없다"는 규칙을 명시적으로 고정하는 회귀 테스트로 가치가 크다.
- PST-NFR-002(목록 쿼리 횟수)는 쿼리 횟수를 세는 테스트를 추가하면 자동 검증으로 바꿀 수 있다.

## 변경 이력

| 버전 | 일자 | 변경 내용 | 작성자 |
|---|---|---|---|
| 1.0.0 | 2026-10-09 | 최초 작성 (`main` e96a878 기준 수행) | HseongH |
| 1.0.1 | 2026-10-09 | `main` f46a99c(포트 정리, Lombok 제거 반영)에서 재수행. 판정 변화 없음. 참조 테스트 이름 전수 확인 | HseongH |
| 1.0.2 | 2026-10-09 | `main` 85cce67(결함 수정 3건, Valkey 전환 반영)에서 재수행. 판정 변화 없음 | HseongH |
| 1.1.0 | 2026-10-09 | E2E를 Firefox로 실행해 TC-PST-043, 051을 N/T → Pass로 판정 | HseongH |
| 1.2.0 | 2026-10-09 | `fix/review-defects`에서 수행. 회귀 항목 TC-PST-052(조회수 롤백), 053(1보다 작은 식별자) 추가, TC-PST-010 N/T → Pass | HseongH |
| 1.2.1 | 2026-10-09 | E2E 브라우저를 Chromium으로 바꾸고 재수행. TC-PST-043, 051 판정 변화 없음 (Pass) | HseongH |
| 1.3.0 | 2026-10-10 | 회귀 항목 TC-PST-054(목록 조회 오류 시 화면이 깨지던 결함) 추가 (PST-SRS 1.2.0) | HseongH |
| 1.3.1 | 2026-10-10 | 오류 해석을 `problem.ts`로 모은 리팩터링 후 재수행. TC-PST-049에 화면 수준 근거(수정 화면의 저장 실패) 추가. 판정 변화 없음 | HseongH |
| 1.3.2 | 2026-10-10 | 폼을 Signal Forms로 옮긴 뒤 재수행. 검색 폼 단위 테스트(`search-form.spec`) 추가. 판정 변화 없음 | HseongH |
| 1.4.0 | 2026-10-10 | TC-PST-055(화면별 브라우저 제목) 추가 (PST-SRS 1.3.0) | HseongH |
| 1.5.0 | 2026-10-10 | `feature/auth-service`에서 수행. 비회원 조회자 쿠키 항목 TC-PST-057~059 추가 (PST-SRS 1.4.0). 진입점 경유 E2E로 재수행, 판정 변화 없음 | HseongH |
