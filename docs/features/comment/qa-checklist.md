---
doc_id: CMT-QA
title: 댓글 QA 체크리스트
version: 1.4.0
status: In Review
owner: HseongH
reviewers: []
approved_date:
last_updated: 2026-10-09
related: [PRJ-QA 1.4.0, CMT-SRS 1.4.0, CMT-SDS 1.5.1]
---

# 댓글 QA 체크리스트

> 작성 규칙과 판정 기준은 [공통 QA 기준](../../project/qa-standards.md)을 따른다.

## 1. 수행 정보

| 항목 | 값 |
|---|---|
| 대상 커밋 | `fix/review-defects` 브랜치 끝, 이 문서를 고친 커밋과 같은 코드 (이전 수행: e96a878, f46a99c, 85cce67, `feature/comment-threads`) |
| 수행일 | 2026-10-09 |
| 백엔드 자동 검증 | `./gradlew check` 성공 (테스트 140개, 실패 0, 오류 0, 건너뜀 0. 이 문서가 인용한 테스트가 모두 이번 실행 결과에 Pass로 있는 것을 대조함) |
| 화면 자동 검증 | `fix/resource-error-state`에서 `pnpm verify` 성공 (린트, 타입 검사, 빌드 통과, 테스트 파일 12개·테스트 44개 통과). 화면만 바뀌어 백엔드는 다시 실행하지 않았다. 이전 수행의 `pnpm gen:api` 후 생성 타입 변화 없음 |
| E2E | `pnpm e2e` 성공 (Chromium, 4개 통과: 로그인 준비, 글·댓글·좋아요 흐름, 검색 URL 유지, 로그아웃). `main` 1fa1827에 E2E 브라우저를 Chromium으로 바꾸는 설정을 적용하고, `deploy/compose.yaml`의 컨테이너와 `./gradlew :services:board:bootRun`을 띄운 뒤 2026-10-09 수행 (이전 수행: Firefox, `main` 05a00c4) |
| 수동 검증 | 실행하지 않음 (N/T) |

## 2. 테스트 항목

### 2.1 작성

| TC | 요구사항 | 시나리오 | 기대 결과 | 검증 수단 | 판정 |
|---|---|---|---|---|---|
| TC-CMT-001 | CMT-FR-001, 004 | 회원이 댓글 작성 후 목록 조회 | `201`, 목록에 표시 | `CommentControllerTest#댓글을_작성하고_목록에서_확인할_수_있다` | Pass |
| TC-CMT-002 | CMT-FR-001 | 원댓글의 깊이 | 0 | `CommentTest#원댓글의_깊이는_0이다` | Pass |
| TC-CMT-003 | CMT-FR-001 | 존재하지 않는 게시글에 작성 | `404 POST_NOT_FOUND` | `CommentControllerTest#존재하지_않는_게시글에는_댓글을_달_수_없다` | Pass |
| TC-CMT-004 | CMT-FR-001, COM-IF-004 | 빈 본문으로 작성 | `400` | `CommentControllerTest#본문이_비면_400이다` | Pass |
| TC-CMT-005 | CMT-FR-001 | 본문 경계값: 공백, 1,001자 | 거부 | `CommentTest#본문은_비어있을_수_없다`, `#본문은_1000자를_넘을_수_없다` | Pass |
| TC-CMT-006 | CMT-FR-001 | 삭제된 게시글에 작성 | `404 POST_NOT_FOUND` | 자동 테스트 없음 | N/T |
| TC-CMT-044 | CMT-FR-001, COM-NFR-012 | 게시글 삭제가 커밋되기 전에 댓글 작성 시작 (회귀) | 작성은 `404 POST_NOT_FOUND`, 삭제된 게시글에 살아 있는 댓글 없음 | `CommentWriteRaceTest#삭제가_커밋되기_전에_시작한_댓글_작성은_실패하고_살아_있는_댓글이_남지_않는다` | Pass |
| TC-CMT-045 | COM-IF-004 | 식별자가 0인 게시글의 댓글 목록, 식별자가 0인 댓글 삭제, 부모 댓글 식별자가 -1인 답글 작성 (회귀) | `400 INVALID_REQUEST`와 `errors` | `CommentControllerTest#식별자가_1보다_작으면_400이다`, `#부모_댓글_식별자가_1보다_작으면_400이다` | Pass |
| TC-CMT-007 | CMT-FR-002 | 원댓글에 답글 작성 | 깊이 1, 부모 식별자 기록 | `CommentTest#원댓글에_달린_답글의_깊이는_1이다`, `CommentControllerTest#대댓글은_깊이_1로_기록된다`, `CommentPersistenceAdapterTest#대댓글은_부모_식별자와_깊이를_유지한다` | Pass |
| TC-CMT-008 | CMT-FR-002, 009 | 삭제된 댓글에 답글 작성 | `404 COMMENT_NOT_FOUND` | `CommentControllerTest#삭제된_원댓글에는_답글을_달_수_없다` | Pass |
| TC-CMT-009 | CMT-FR-003 | 대댓글에 답글 작성 | `400 COMMENT_DEPTH_EXCEEDED` | `CommentTest#대댓글에는_답글을_달_수_없다`, `CommentControllerTest#대댓글에는_답글을_달_수_없다` | Pass |
| TC-CMT-010 | CMT-FR-008 | 게시글 A의 댓글을 부모로 지정해 게시글 B에 답글 작성 | 거부 | `CommentTest#다른_게시글의_댓글에는_답글을_달_수_없다`, `CommentControllerTest#다른_게시글의_댓글에는_답글을_달_수_없다` (`404 COMMENT_NOT_FOUND`) | Pass (1.1.0, 1.0.x에서 Fail) |

### 2.2 조회

| TC | 요구사항 | 시나리오 | 기대 결과 | 검증 수단 | 판정 |
|---|---|---|---|---|---|
| TC-CMT-011 | CMT-FR-004, COM-NFR-014 | 원댓글 여러 개 작성 후 목록, 페이지 나눔 | 원댓글이 작성 순서대로, 원댓글 수로 페이지가 나뉨 | `CommentPersistenceAdapterTest#원댓글_목록은_작성_순서이고_원댓글_수로_페이지를_나눈다` | Pass |
| TC-CMT-012 | CMT-FR-004, COM-NFR-012 | 삭제된 댓글 | 읽을 수 없음 | `CommentPersistenceAdapterTest#삭제된_댓글은_읽을_수_없다` | Pass |
| TC-CMT-013 | CMT-FR-004 | 저장한 댓글을 다시 읽기 | 같은 값 | `CommentPersistenceAdapterTest#저장한_댓글을_다시_읽을_수_있다` | Pass |
| TC-CMT-014 | CMT-FR-004 | 존재하지 않는 게시글의 댓글 목록 (회귀, CMT-OPEN-05) | `404 POST_NOT_FOUND` | `CommentControllerTest#존재하지_않는_게시글의_댓글_목록은_404다` | Pass |
| TC-CMT-015 | CMT-FR-004 | 삭제된 게시글의 댓글 목록 (회귀, CMT-OPEN-05) | `404 POST_NOT_FOUND` | `CommentControllerTest#삭제된_게시글의_댓글_목록은_404다` | Pass |
| TC-CMT-016 | CMT-FR-004 | 원댓글과 대댓글이 있는 게시글의 원댓글 목록 | 대댓글은 원댓글 목록에 섞이지 않음 | `CommentPersistenceAdapterTest#원댓글_목록에는_대댓글이_포함되지_않는다` | Pass |
| TC-CMT-017 | CMT-FR-004, COM-NFR-012 | 여러 원댓글의 대댓글 조회 (일부 삭제됨) | 원댓글별 작성 순서, 삭제된 대댓글 제외 | `CommentPersistenceAdapterTest#대댓글은_원댓글별로_작성_순서대로_반환되고_삭제된_대댓글은_빠진다` | Pass |
| TC-CMT-018 | CMT-FR-004 | 대댓글 2개가 달린 원댓글과 다른 원댓글을 `size=1`로 조회 | 전체 건수 2(원댓글 수), 한 페이지에 묶음 1개, 대댓글 2개 모두 포함 | `CommentControllerTest#페이지_크기는_원댓글_수_기준이고_대댓글은_잘리지_않는다` | Pass |
| TC-CMT-019 | CMT-FR-004 | `sort=foo`로 목록 요청 | 정렬 무시, `200` | `CommentControllerTest#정렬_파라미터는_무시한다` | Pass |

### 2.3 수정과 삭제

| TC | 요구사항 | 시나리오 | 기대 결과 | 검증 수단 | 판정 |
|---|---|---|---|---|---|
| TC-CMT-020 | CMT-FR-005 | 작성자가 수정 | 성공 | `CommentTest#작성자는_댓글을_수정할_수_있다` | Pass |
| TC-CMT-021 | CMT-FR-005, 006 | 다른 회원이 수정·삭제 | `403`, 작성자는 `204` | `CommentTest#작성자가_아니면_수정할_수_없다`, `#작성자가_아니면_삭제할_수_없다`, `CommentControllerTest#작성자만_수정하고_삭제할_수_있다` | Pass |
| TC-CMT-022 | CMT-FR-005 | 삭제된 댓글 수정 | `COMMENT_NOT_FOUND` | `CommentTest#이미_삭제된_댓글은_수정할_수_없다` | Pass |
| TC-CMT-023 | CMT-FR-006 | 관리자가 다른 사람의 댓글 삭제 | 성공 | `CommentTest#관리자는_다른_사람의_댓글을_삭제할_수_있다` | Pass |
| TC-CMT-024 | CMT-FR-005 | 관리자가 다른 사람의 댓글 수정 | `403` | 자동 테스트 없음 (도메인 `updateBy`가 관리자 여부를 받지 않으므로 구조상 불가능) | N/T |
| TC-CMT-025 | CMT-FR-007 | 게시글의 댓글 일괄 삭제 | 모두 삭제됨 | `CommentPersistenceAdapterTest#게시글의_댓글이_한꺼번에_삭제된다` | Pass |
| TC-CMT-026 | CMT-FR-007 | 댓글이 달린 게시글 삭제 (기능 간 연동) | 게시글과 댓글 모두 삭제 | `PostDeletionIntegrationTest#게시글을_삭제하면_게시글과_댓글이_모두_삭제된다` | Pass |

### 2.4 화면

| TC | 요구사항 | 시나리오 | 기대 결과 | 검증 수단 | 판정 |
|---|---|---|---|---|---|
| TC-CMT-030 | CMT-FR-020 | 댓글과 대댓글 표시 | 함께 표시, 대댓글 들여쓰기 | `comment-section.spec: 댓글과 대댓글을 함께 보여준다` | Pass |
| TC-CMT-031 | CMT-FR-021 | 댓글 등록 | 본문 전송 후 입력란 비움 | `comment-section.spec: 댓글을 작성하면 본문을 전송한다`, `…입력란을 비운다` | Pass |
| TC-CMT-032 | CMT-FR-022 | 대댓글의 답글 버튼 | 표시하지 않음 | `comment-section.spec: 대댓글에는 답글 버튼을 보여주지 않는다` | Pass |
| TC-CMT-033 | CMT-FR-023 | 본인 댓글과 남의 댓글 | 본인 댓글에만 수정·삭제 | `comment-section.spec: 작성자 본인에게만 수정과 삭제를 보여준다` | Pass |
| TC-CMT-035 | CMT-FR-020 | 원댓글 두 개 중 첫째에만 답글 | 화면 순서가 원댓글1, 답글1, 원댓글2 | `comment-section.spec: 대댓글은 자기 원댓글 바로 아래에 보여준다` | Pass |
| TC-CMT-036 | CMT-FR-020, 009 | 삭제된 원댓글과 그 답글 | "삭제된 댓글입니다" 표시, 답글 표시, 답글·수정·삭제 버튼 없음 | `comment-section.spec: 삭제된 원댓글은 자리만 표시하고 버튼을 보여주지 않는다` | Pass |
| TC-CMT-037 | CMT-FR-020 | 삭제된 원댓글(답글 2개)과 살아 있는 원댓글 | 제목의 댓글 수가 3 (원댓글 묶음 수 2가 아님) | `comment-section.spec: 댓글 수는 화면에 보이는 댓글만 센다` | Pass |
| TC-CMT-046 | CMT-FR-020 | 댓글 목록 API가 오류를 응답 (회귀) | 안내 문구, 화면이 깨지지 않음 | `comment-section.spec: 댓글을 불러오지 못하면 안내를 보여준다` | Pass |
| TC-CMT-034 | CMT-FR-021 | 상세 화면에서 댓글 작성 (E2E) | 작성한 댓글 표시 | E2E `board.spec: 글을 쓰고 댓글과 좋아요를 남긴다` | Pass |

### 2.5 삭제된 원댓글 (CMT-FR-009)

| TC | 요구사항 | 시나리오 | 기대 결과 | 검증 수단 | 판정 |
|---|---|---|---|---|---|
| TC-CMT-040 | CMT-FR-009 | 답글이 달린 원댓글을 삭제한 뒤 목록 조회 | 원댓글 자리 유지, `deleted: true`, 본문·작성자 `null`, 답글은 그대로 | `CommentControllerTest#삭제된_원댓글은_본문과_작성자를_가리고_대댓글과_함께_나온다`, `CommentPersistenceAdapterTest#삭제된_원댓글은_살아있는_대댓글이_있을_때만_포함된다` | Pass |
| TC-CMT-041 | CMT-FR-009 | 답글이 없거나 답글도 모두 삭제된 원댓글을 삭제 | 목록에 나오지 않음 | `CommentControllerTest#대댓글이_없는_삭제된_원댓글은_목록에_나오지_않는다`, `CommentPersistenceAdapterTest#삭제된_원댓글은_살아있는_대댓글이_있을_때만_포함된다` | Pass |
| TC-CMT-042 | CMT-FR-009, COM-IF-006 | OpenAPI 문서의 댓글 응답 | `body`, `authorId`는 필수 아님, `deleted`, `root`, `replies`는 필수 | `OpenApiDocumentTest#댓글_응답의_가려질_수_있는_필드는_필수가_아니다` | Pass |
| TC-CMT-043 | CMT-FR-009 | 원댓글 조회와 대댓글 조회 사이에 마지막 대댓글이 삭제됨 | 대댓글 없는 삭제된 원댓글 묶음을 내보내지 않음 | `CommentQueryServiceTest#대댓글이_사라진_삭제된_원댓글은_목록에서_뺀다` | Pass |

## 3. 추적 요약

| 요구사항 | TC | 자동화 |
|---|---|---|
| CMT-FR-001 | 001~006, 044 | 부분 (이미 삭제된 게시글에 작성하는 단순한 경우는 미검증. 삭제와 겹치는 경우는 044) |
| CMT-FR-002 | 007, 008 | 완전 |
| CMT-FR-003 | 009 | 완전 |
| CMT-FR-004 | 001, 011~019 | 완전 |
| CMT-FR-005 | 020~022, 024 | 부분 |
| CMT-FR-006 | 021, 023 | 완전 |
| CMT-FR-007 | 025, 026 | 완전 |
| CMT-FR-008 | 010 | 완전 |
| CMT-FR-009 | 008, 036, 040~043 | 완전 |
| CMT-FR-020~023 | 030~037, 046 | 화면 단위 테스트 완전, E2E 1건 |

## 4. 결과 요약과 후속 조치

- 집계: 전체 41건 중 Pass 39, Fail 0, N/T 2 (자동 테스트 없음 2).
- TC-CMT-034(E2E)는 1.3.0까지 N/T였다. 모노레포 구조의 `main`에서 E2E를 Firefox로 실행해 Pass로 판정했다.
- TC-CMT-010은 1.0.x에서 Fail이었다. 재현 테스트를 먼저 추가하고 `Comment.write`가 부모 댓글의 게시글을 검사하도록 고쳐 Pass가 되었다. 오류 코드는 기존 `COMMENT_NOT_FOUND`를 재사용하므로 프로젝트 SRS의 오류 코드 목록은 바뀌지 않는다.
- TC-CMT-014, 015는 CMT-OPEN-05를 해결하면서 추가한 회귀 항목이다.
- TC-CMT-016~019, 035~037, 040~043은 원댓글 단위 목록(CMT-FR-004 변경)과 삭제된 원댓글 표시(CMT-FR-009)를 추가하면서 생긴 항목이다. TC-CMT-011은 기존 평면 목록 테스트가 없어져 원댓글 목록 테스트로 근거를 바꿨다.
- TC-CMT-008은 1.1.x까지 N/T였다. 삭제된 원댓글 표시를 추가하면서 자동 테스트가 생겨 Pass로 판정했다.
- 자동 테스트가 없는 항목(TC-CMT-006, 024)은 회귀 테스트 추가 후보다.

## 변경 이력

| 버전 | 일자 | 변경 내용 | 작성자 |
|---|---|---|---|
| 1.0.0 | 2026-10-09 | 최초 작성 (`main` e96a878 기준 수행) | HseongH |
| 1.0.1 | 2026-10-09 | `main` f46a99c(포트 정리, Lombok 제거 반영)에서 재수행. 판정 변화 없음. 참조 테스트 이름 전수 확인 | HseongH |
| 1.1.0 | 2026-10-09 | TC-CMT-010 Fail → Pass (CMT-OPEN-01 해결). 회귀 항목 TC-CMT-014, 015 추가 (CMT-OPEN-05 해결) | HseongH |
| 1.1.1 | 2026-10-09 | 수행 정보를 `main` 85cce67 재수행 결과로 정정. 1.1.0의 Pass 판정(TC-CMT-010, 014, 015)은 f46a99c에 없던 테스트에 근거하므로, 실제 근거가 된 실행을 기록 | HseongH |
| 1.2.0 | 2026-10-09 | `feature/comment-threads`에서 수행. 원댓글 단위 목록과 삭제된 원댓글 표시 항목 추가(TC-CMT-016~019, 035~037, 040~043), TC-CMT-008 N/T → Pass, TC-CMT-011 근거 테스트 교체 | HseongH |
| 1.3.0 | 2026-10-09 | `fix/review-defects`에서 수행. 회귀 항목 TC-CMT-044(삭제와 작성의 경쟁), 045(1보다 작은 식별자) 추가 | HseongH |
| 1.3.1 | 2026-10-09 | `main` 05a00c4에서 E2E 수행. TC-CMT-034 N/T → Pass | HseongH |
| 1.3.2 | 2026-10-09 | E2E 브라우저를 Chromium으로 바꾸고 재수행. TC-CMT-034 판정 변화 없음 (Pass) | HseongH |
| 1.4.0 | 2026-10-10 | 회귀 항목 TC-CMT-046(댓글 조회 오류 시 화면이 깨지던 결함) 추가 (CMT-SRS 1.4.0) | HseongH |
