# 문서 체계

이 디렉터리는 게시판(bbs) 프로젝트의 정식 문서를 담는다. 문서는 코드와 같은 저장소에서 Markdown으로 관리하고, 코드와 같은 PR에서 함께 리뷰한다 (Docs as Code).

## 1. 문서 목록

### 1.1 프로젝트 공통 문서

프로젝트 전체에 하나씩 존재하며, 모든 기능 문서가 참조하는 기준이다.

| 문서 | 경로 | 답하는 질문 | 참고 표준 |
|---|---|---|---|
| Project Charter | [project/charter.md](project/charter.md) | 왜 하는가, 어디까지 하는가 | PMBOK (경량화) |
| 프로젝트 SRS | [project/srs.md](project/srs.md) | 시스템 전체가 무엇을 해야 하는가 | ISO/IEC/IEEE 29148 |
| 프로젝트 SDS | [project/sds.md](project/sds.md) | 시스템 전체가 어떤 구조인가 | IEEE 1016, ISO/IEC/IEEE 42010 |
| 공통 QA 기준 | [project/qa-standards.md](project/qa-standards.md) | 무엇을 만족해야 완료인가 | ISO/IEC/IEEE 29119-3 (경량화) |
| ADR | [project/adr/](project/adr/README.md) | 왜 그렇게 결정했는가 | Michael Nygard ADR 형식 |

### 1.2 기능별 문서

기능(메뉴) 하나마다 세 문서를 둔다.

| 기능 | SRS | SDS | QA 체크리스트 |
|---|---|---|---|
| 회원·인증 (`MEM`) | [srs](features/member/srs.md) | [sds](features/member/sds.md) | [qa](features/member/qa-checklist.md) |
| 게시글 (`PST`) | [srs](features/post/srs.md) | [sds](features/post/sds.md) | [qa](features/post/qa-checklist.md) |
| 댓글 (`CMT`) | [srs](features/comment/srs.md) | [sds](features/comment/sds.md) | [qa](features/comment/qa-checklist.md) |

### 1.3 템플릿

[templates/](templates/) 아래의 빈 템플릿을 복사해서 새 기능 문서나 다른 프로젝트의 문서를 시작한다.

### 1.4 개발 과정 기록

[superpowers/](superpowers/) 아래의 설계 문서와 구현 계획은 AI 협업 도구(Superpowers)가 개발 중에 만든 **작업 기록**이다. 정식 문서가 아니므로 갱신하지 않으며, 정식 문서에서 출처로만 참조한다.

## 2. 계층 구조와 작성 원칙

```
Project Charter          왜, 어디까지
   └─ 프로젝트 SRS        공통 요구사항 + 기능 목록
        ├─ 기능 SRS       기능의 상세 요구사항
        │    └─ 기능 SDS  기능의 상세 설계
        │         └─ QA 체크리스트  요구사항별 검증 결과
        └─ 프로젝트 SDS   아키텍처 + 공통 컴포넌트 (기능 SDS가 참조)
```

1. **중복하지 않는다.** 기능 문서는 프로젝트 문서를 참조하고, 공통 기준을 반복해서 적지 않는다. 공통 기준과 **다른 점만** 적는다.
2. **사실과 가정을 구분한다.** 확인되지 않은 내용은 "가정" 또는 "미결 사항"으로 표시한다.
3. **근거를 남긴다.** 설계 결정마다 검토한 대안과 선택한 이유를 적는다. 프로젝트 전체에 영향을 주는 결정은 ADR로 분리한다.
4. **검증 가능하게 쓴다.** 요구사항마다 "어떻게 확인하는가"를 함께 적는다. 확인할 방법이 없는 요구사항은 요구사항이 아니라 희망 사항이다.

## 3. 다이어그램

그림이 글보다 빨리 읽히는 곳에만 다이어그램을 둔다. 도구는 다이어그램의 성격에 따라 나눈다.

| 종류 | 도구 | 저장 위치 | 이유 |
|---|---|---|---|
| 구성도, 계층 구조, 의존도, ERD | draw.io | `docs/project/diagrams/*.drawio.svg` | 배치를 세밀하게 잡아야 한다. `.drawio.svg`는 GitHub에서 그림으로 보이고 draw.io로 열면 그대로 편집된다 |
| 시퀀스, 상태 전이, 흐름도 | Mermaid | 문서 본문의 ` ```mermaid ` 코드 블록 | 텍스트라서 PR에서 줄 단위로 리뷰할 수 있다 |

- draw.io 라벨은 HTML 서식을 쓰지 않는다 (`html=1` 금지). 그래야 SVG 안에 `<text>`로 들어가 GitHub에서 글자가 보인다.
- 배경은 흰색으로 둔다. GitHub 다크 모드에서도 읽혀야 한다.
- 커밋 전에 렌더링 결과를 직접 확인한다: draw.io는 `drawio --export --format png`, Mermaid는 GitHub 미리 보기나 Mermaid 렌더러.
- 다이어그램도 코드와 함께 낡는다. 구조가 바뀌는 PR에서 다이어그램을 함께 고친다.

## 4. 식별자 규칙

문서 사이의 연결은 식별자로만 한다. 식별자는 한 번 부여하면 바꾸거나 재사용하지 않는다. 요구사항을 삭제하면 번호를 비워 두고 "삭제됨"으로 표시한다.

| 대상 | 형식 | 예시 |
|---|---|---|
| 프로젝트 공통 비기능 요구사항 | `COM-NFR-NNN` | `COM-NFR-003` |
| 프로젝트 공통 인터페이스 요구사항 | `COM-IF-NNN` | `COM-IF-001` |
| 프로젝트 제약 조건 | `COM-CON-NNN` | `COM-CON-002` |
| 기능 요구사항 | `<기능>-FR-NNN` | `PST-FR-004` |
| 기능별 비기능 요구사항 | `<기능>-NFR-NNN` | `PST-NFR-001` |
| QA 테스트 항목 | `TC-<기능>-NNN` | `TC-CMT-007` |
| 아키텍처 결정 | `ADR-NNNN` | `ADR-0004` |

기능 접두사는 `MEM`(회원·인증), `PST`(게시글), `CMT`(댓글)이다.

### 추적 경로

```
PST-FR-007 (기능 SRS)
  → PostLikeService, PostLikePersistenceAdapter (기능 SDS §요구사항 대응표)
    → PostLikeConcurrencyTest#동시에_좋아요를_눌러도_한_번만_반영된다 (자동 테스트)
      → TC-PST-015 (QA 체크리스트, 판정 기록)
```

QA 체크리스트의 "요구사항" 열을 모으면 요구사항 추적 매트릭스가 된다. 모든 `FR` 식별자가 하나 이상의 `TC`에 연결되어 있어야 한다.

## 5. 문서 머리말

모든 정식 문서는 다음 머리말로 시작한다.

```yaml
---
doc_id: PST-SRS            # 문서 식별자
title: 게시글 요구사항 명세서
version: 1.0.0
status: In Review          # Draft | In Review | Approved | Superseded
owner: HseongH             # 문서 책임자
reviewers: []              # 검토자
approved_date:             # Approved가 되는 날 기록
last_updated: 2026-10-09
related: [PRJ-SRS 1.0.0, PST-SDS 1.1.0, PST-QA 1.0.1]
---
```

## 6. 버전 규칙

파일을 복사해서 버전을 나누지 않는다 (`srs_v2.md` 금지). 파일은 하나만 유지하고 과거 버전은 Git 이력과 태그로 확인한다.

| 자리 | 올리는 경우 | 예시 |
|---|---|---|
| Major (`2.0.0`) | 범위나 핵심 흐름이 바뀌어 기존 요구사항 일부가 무효가 된다 | 게시판에 카테고리 체계 도입 |
| Minor (`1.1.0`) | 요구사항이나 설계를 추가·변경하지만 기존 흐름은 유지된다 | 게시글 신고 기능 추가 |
| Patch (`1.0.1`) | 의미가 바뀌지 않는 수정 | 오탈자, 표현 개선, 링크 수정 |

- **Approved 문서를 고치면 반드시 버전을 올리고** 상태를 In Review로 되돌린다.
- **같은 기능의 SRS·SDS·QA는 함께 움직인다.** 셋 중 하나가 바뀌면 나머지 둘에 미치는 영향을 같은 PR에서 검토하고, 머리말의 `related`를 갱신한다.
- 문서마다 본문 끝에 **변경 이력** 표를 둔다.

## 7. 작성과 승인 절차

1. 기능 브랜치(`feature/<설명>`)에서 템플릿을 복사해 `status: Draft`로 작성한다.
2. 코드와 함께 PR을 연다. 문서 상태를 `In Review`로 바꾼다.
3. PR 리뷰가 곧 문서 검토다. 지적 사항을 반영한다.
4. merge 직전에 `status: Approved`, `approved_date`, `reviewers`를 기록한다.
5. 릴리스할 때 Git 태그(`vX.Y.Z`)를 붙이면 그 시점의 모든 문서 상태를 다시 확인할 수 있다.

개인 프로젝트에서는 검토자가 본인뿐이다. 그래도 PR을 열고 **하루 뒤에 다시 읽고 승인하는 습관**을 들이면 같은 효과의 상당 부분을 얻을 수 있다.

## 변경 이력

| 버전 | 일자 | 변경 내용 | 작성자 |
|---|---|---|---|
| 1.0.0 | 2026-10-09 | 문서 체계 최초 작성 | HseongH |
| 1.1.0 | 2026-10-09 | 다이어그램 규칙(§3) 추가 | HseongH |
