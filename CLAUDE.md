# 프로젝트 작업 지침

## 문서 체계

이 저장소의 정식 문서는 `docs/`에 있으며 규칙은 [docs/README.md](docs/README.md)를 따른다. 코드를 바꾸기 전에 관련 문서를 먼저 읽는다.

- 프로젝트 공통: `docs/project/` (charter, srs, sds, qa-standards, coding-standards, adr/)
- 코드를 쓸 때는 [코딩 표준](docs/project/coding-standards.md)을 따른다. 리뷰에서는 규칙 번호(`CS-B20` 등)로 지적한다.
- 기능별: `docs/features/<기능>/` (srs, sds, qa-checklist)
- 템플릿: `docs/templates/`

### 기능을 새로 만들거나 동작을 바꿀 때

1. 설계 합의(brainstorming)의 결과는 Superpowers 기본 경로(`docs/superpowers/specs/`)가 아니라 **기능 문서**에 쓴다.
   - 새 기능: `docs/templates/feature-srs.md`, `feature-sds.md`, `feature-qa-checklist.md`를 `docs/features/<기능>/`에 복사해서 작성한다.
   - 기존 기능의 변경: 해당 기능의 SRS·SDS를 고치고 버전을 올린다 (`docs/README.md` §6).
2. 구현 계획(writing-plans)은 임시 산출물이므로 `docs/superpowers/plans/`에 둔다. 계획의 각 작업에는 구현하는 요구사항 ID(`PST-FR-004` 등)를 적는다.
3. 계획의 Global Constraints에는 프로젝트 SRS의 공통 요구사항(`COM-*`) 중 해당하는 것을 옮겨 적는다.
4. 구현을 마치면 QA 체크리스트의 판정을 **실제로 실행한 명령의 결과**로 채운다. 실행하지 않은 항목은 `N/T`다.
5. 프로젝트 전체에 영향을 주는 결정은 `docs/project/adr/`에 새 ADR로 추가한다. 승인된 ADR은 고치지 않는다.
6. SDS는 [문서 체계 §8](docs/README.md#8-sds-작성-기준)의 기준대로 책임·흐름·결정만 적는다. 메서드 시그니처, 필드 목록, 세부 버전, 테스트 목록은 적지 않는다.
7. 구조가 바뀌면 `docs/project/diagrams/`의 다이어그램과 문서 안의 Mermaid 다이어그램도 함께 고치고, 렌더링 결과를 확인한다 (`docs/README.md` §3).
8. 새 오류 코드는 `ErrorCode`와 `docs/project/srs.md` §5.1에 함께 추가한다.

### 식별자

- 기능 접두사: `MEM`(회원·인증), `PST`(게시글), `CMT`(댓글). 새 기능은 세 글자 접두사를 정해 `docs/README.md`와 프로젝트 SRS §3에 등록한다.
- 식별자는 바꾸거나 재사용하지 않는다.

## Git

- 브랜치: `<type>/<설명>` (예: `feature/post-report`, `fix/comment-parent-post-check`, `docs/...`)
- 커밋 메시지: `type(scope): subject` (커밋 훅이 검사한다)
- 코드 변경과 그에 따른 문서 변경은 같은 PR에 넣는다.

## 검증 명령

- 백엔드: `./gradlew check` (Docker 필요)
- 프론트엔드: `cd services/web && pnpm verify`
- API가 바뀌면: `cd services/web && pnpm gen:api` 후 `pnpm typecheck`
