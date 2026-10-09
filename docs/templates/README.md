# 문서 템플릿

다른 프로젝트나 새 기능에 복사해서 쓰는 빈 템플릿이다. 각 템플릿 안의 `<!-- -->` 주석은 작성 안내이며, 작성을 마치면 지운다. 템플릿 안의 상대 링크는 **붙여 넣을 위치를 기준으로** 작성되어 있으므로 `templates/` 안에서는 열리지 않는다.

## 사용 순서

### 새 프로젝트를 시작할 때

| 순서 | 복사할 템플릿 | 붙여 넣을 위치 |
|---|---|---|
| 1 | [charter.md](charter.md) | `docs/project/charter.md` |
| 2 | [project-srs.md](project-srs.md) | `docs/project/srs.md` |
| 3 | [project-sds.md](project-sds.md) | `docs/project/sds.md` |
| 4 | [qa-standards.md](qa-standards.md) | `docs/project/qa-standards.md` |
| 5 | [coding-standards.md](coding-standards.md) | `docs/project/coding-standards.md` |
| 6 | [adr.md](adr.md) | `docs/project/adr/0001-<결정-요약>.md` (결정마다 하나) |
| 7 | 이 저장소의 [docs/README.md](../README.md) | `docs/README.md` (식별자 접두사와 문서 목록만 바꾼다) |

### 새 기능을 개발할 때

| 순서 | 복사할 템플릿 | 붙여 넣을 위치 | 작성 시점 |
|---|---|---|---|
| 1 | [feature-srs.md](feature-srs.md) | `docs/features/<기능>/srs.md` | 설계 합의 단계 (구현 전) |
| 2 | [feature-sds.md](feature-sds.md) | `docs/features/<기능>/sds.md` | SRS 승인 후, 구현 계획 전 |
| 3 | [feature-qa-checklist.md](feature-qa-checklist.md) | `docs/features/<기능>/qa-checklist.md` | 항목은 SRS와 함께, 판정은 구현 후 |
| 4 | 프로젝트 SRS §3 기능 목록에 행 추가 | | SRS와 함께 |

## 작성 예시

이 저장소의 문서가 모든 템플릿의 작성 예시다.

| 템플릿 | 예시 |
|---|---|
| charter | [project/charter.md](../project/charter.md) |
| project-srs | [project/srs.md](../project/srs.md) |
| project-sds | [project/sds.md](../project/sds.md) |
| qa-standards | [project/qa-standards.md](../project/qa-standards.md) |
| adr | [project/adr/0004-atomic-counter-update.md](../project/adr/0004-atomic-counter-update.md) |
| feature-srs | [features/post/srs.md](../features/post/srs.md) |
| feature-sds | [features/post/sds.md](../features/post/sds.md) |
| feature-qa-checklist | [features/comment/qa-checklist.md](../features/comment/qa-checklist.md) (Fail 항목 기록 예시 포함) |

## 자주 하는 실수

1. **기능 SRS에 공통 요구사항을 다시 적는다.** 공통 기준과 다른 점만 적는다.
2. **수용 기준이 없는 요구사항을 쓴다.** "빠르게 응답한다"가 아니라 "목록 조회는 쿼리 2회로 처리한다"처럼 확인할 수 있게 쓴다.
3. **QA 판정을 실행 없이 채운다.** 판정은 이번에 실행한 명령의 결과로만 채운다. 실행하지 않았으면 `N/T`다.
4. **Fail 항목을 지운다.** Fail은 남기고 후속 조치를 적는다. 고친 뒤 판정을 바꾸고 변경 이력에 기록한다.
5. **Approved 문서를 버전 변경 없이 고친다.**
