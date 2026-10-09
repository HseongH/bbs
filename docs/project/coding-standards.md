---
doc_id: PRJ-CS
title: 게시판(bbs) 코딩 표준
version: 1.4.0
status: In Review
owner: HseongH
reviewers: []
approved_date:
last_updated: 2026-10-09
related: [PRJ-SDS 1.6.0, PRJ-QA 1.3.0]
---

# 게시판(bbs) 코딩 표준

> 코드를 어떻게 써야 하는지 정한다. DO-178C의 Software Code Standards(SCS)나 사내 코딩 규약에 해당하는 문서다. 설계 문서가 "무엇을 어떤 구조로 만드는가"를 다룬다면, 이 문서는 "그 구조 안에서 코드를 어떻게 쓰는가"를 다룬다.

## 1. 원칙

1. **도구가 강제하는 규칙은 설정 파일이 기준이다.** 이 문서는 그 규칙을 다시 나열하지 않고, 왜 두었는지와 설정 위치만 적는다 (§2). 문서와 설정에 같은 목록을 두면 둘 중 하나가 반드시 낡는다.
2. **도구가 강제하지 못하는 규칙만 이 문서가 기준이다** (§3~§6). 리뷰에서 규칙 번호(`CS-B20` 등)로 지적할 수 있도록 번호를 붙인다.
3. **규칙을 새로 만들 때는 도구로 강제할 수 있는지 먼저 검토한다.** 강제할 수 있으면 설정에 넣고 §2에 한 줄만 추가한다.
4. 규칙마다 **이유**를 적는다. 이유를 적을 수 없는 규칙은 취향이므로 두지 않는다.

## 2. 도구가 강제하는 규칙

위반하면 커밋 훅, 빌드, CI 중 하나가 실패한다. 상세 기준은 "설정 위치"의 파일이 정한다.

### 2.1 백엔드

| 영역 | 도구 | 설정 위치 | 요지 | 이유 |
|---|---|---|---|---|
| 서식 | Spotless (google-java-format, ktlint) | `build-logic`의 `bbs.java-conventions`의 `spotless` (Gradle 스크립트는 루트 `build.gradle.kts`) | 들여쓰기, 줄바꿈, import 순서, Javadoc 모양을 자동으로 맞춘다 | 서식은 사람이 판단할 가치가 없다. 리뷰에서 서식 논쟁을 없앤다 |
| 이름·구조 | Checkstyle | `config/checkstyle/checkstyle.xml` | Google Java Style에서 서식 규칙을 뺀 이름 규칙, 구조 규칙, Javadoc 존재 여부 | 포매터가 손대지 않는 부분만 검사해서 두 도구가 충돌하지 않게 한다 |
| 약어 | Checkstyle `AbbreviationAsWordInName` | 같은 파일 (`allowedAbbreviationLength=0`) | `JPAEntity`가 아니라 `JpaEntity`, `HTTPClient`가 아니라 `HttpClient` | 대문자 약어가 이어지면 단어 경계를 읽기 어렵다 |
| Javadoc | Checkstyle `MissingJavadocType`, `MissingJavadocMethod` | 같은 파일 (범위 `protected` 이상) | 공개 타입과 공개 메서드에는 Javadoc이 있어야 한다. `@Override`, 테스트는 제외 | 공개 API는 다른 기능이 사용하므로 계약을 문장으로 남긴다 |
| 테스트 예외 | Checkstyle 억제 | `config/checkstyle/checkstyle-suppressions.xml` | `src/test`에는 메서드·변수 이름 규칙과 Javadoc 규칙을 적용하지 않는다 | 테스트 이름을 한국어 문장으로 쓰기 위해서다 (CS-T01) |
| 정적 분석 | Error Prone, `-Werror` | `build-logic`의 `bbs.java-conventions`의 `tasks.withType<JavaCompile>` | 버그 패턴 검사. 컴파일러 경고도 실패로 처리 | 경고를 허용하면 쌓이고, 쌓이면 아무도 보지 않는다 |
| null 안정성 | NullAway (JSpecify 모드) | 같은 위치 | `@NullMarked` 패키지에서 null 계약 위반은 컴파일 오류 | null 오류를 실행 전에 잡는다 |
| 아키텍처 | ArchUnit | `src/test/java/.../architecture/` | 계층 의존 방향, 기능 경계, `@Transactional`·엔티티·컨트롤러의 위치, 모든 패키지의 `@NullMarked` | [프로젝트 SDS §4.3](sds.md#43-의존-규칙)과 ADR-0001, 0009, 0010 |
| 커버리지 | JaCoCo | `build-logic`의 `bbs.java-conventions`의 `jacocoTestCoverageVerification` | 전체 80%, 도메인·애플리케이션 패키지 90% | [공통 QA 기준 §3](qa-standards.md#3-자동-품질-기준) |

### 2.2 프론트엔드

| 영역 | 도구 | 설정 위치 | 요지 | 이유 |
|---|---|---|---|---|
| 서식 | Prettier | `services/web/.prettierrc` | 한 줄 100자, 큰따옴표 | 백엔드와 같은 이유 |
| 린트 | ESLint (typescript-eslint recommended·stylistic, angular-eslint), 타입 정보 사용 | `services/web/eslint.config.js` | 컴포넌트 선택자는 `app-` 접두사의 kebab-case, 지시자는 `app` 접두사의 camelCase. 템플릿 접근성 규칙 포함. §5에서 "린트"로 표시한 규칙을 강제 | 접근성 위반과 예전 방식의 Angular 코드를 리뷰가 아니라 린트에서 잡는다 ([ADR-0015](adr/0015-keep-angular-for-frontend.md)) |
| 타입 | TypeScript 6 | `services/web/tsconfig.json` | 엄격 모드(TypeScript 6의 기본값)에 더해 `noUncheckedIndexedAccess`, `noImplicitOverride`, `noPropertyAccessFromIndexSignature`, `noImplicitReturns`, `noFallthroughCasesInSwitch` | 배열 접근과 인덱스 시그니처에서 생기는 `undefined`를 타입으로 드러낸다 |
| API 계약 | openapi-typescript | `services/web/package.json`의 `gen:api` | 백엔드 OpenAPI에서 생성한 타입만 사용 | 백엔드가 바뀌면 프론트엔드 타입 검사가 실패한다 |
| 템플릿 검사 | Angular 컴파일러 (`ng build`) | `services/web/angular.json`, `services/web/tsconfig.json` | 엄격한 템플릿 타입 검사(Angular 22의 기본값), 번들 크기 예산 초과 시 실패 | `tsc`는 템플릿을 읽지 않는다. 템플릿의 바인딩 오류는 빌드에서만 드러난다 |

`tsconfig.json`에는 `strict`가 적혀 있지 않다. TypeScript 6부터 엄격 모드가 기본값이기 때문이다. 같은 이유로 `strictTemplates`도 적혀 있지 않다(Angular 22의 기본값). 이 사실이 설정 파일만 봐서는 드러나지 않으므로 여기에 적어 둔다.

`pnpm verify`는 린트 → 타입 검사 → 빌드 → 단위 테스트 순서로 실행하고, 화면 CI(`web.yml`)도 같은 명령을 쓴다. 타입 검사(`tsc`)는 빌드보다 빨라서 커밋 전 검사에 쓰고, 빌드는 템플릿 오류를 테스트 전에 잡는다.

### 2.3 저장소

| 영역 | 도구 | 설정 위치 | 요지 |
|---|---|---|---|
| 커밋 메시지 | Git 훅 | `hooks/commit-msg` | `type(scope): subject`. type은 `feat fix docs style refactor perf test build ci chore revert` |
| 커밋 전 검사 | Git 훅 | `hooks/pre-commit` | 포맷, Checkstyle. `services/web/`이 바뀌면 프론트엔드 검사도 실행 |
| 줄바꿈 | `.gitattributes`, CI | `.github/workflows/line-endings.yml` | 저장소에 CRLF로 저장된 파일이 있으면 실패 |

훅은 `./gradlew build`가 `core.hooksPath`를 설정해서 연결한다.

## 3. 백엔드 규칙

### 3.1 책임의 배치

| 번호 | 규칙 | 이유 |
|---|---|---|
| CS-B01 | 업무 규칙(길이, 권한, 상태, 깊이 제한)은 도메인 객체 안에 둔다. 서비스와 컨트롤러에 `if`로 규칙을 쓰지 않는다. | 규칙이 한 곳에 있어야 우회 경로가 생기지 않는다 ([ADR-0003](adr/0003-authorization-in-domain.md)) |
| CS-B02 | 서비스는 조율만 한다: 불러오기 → 도메인 메서드 호출 → 저장 → 필요하면 이벤트 발행. | 서비스가 판단을 시작하면 CS-B01이 무너진다 |
| CS-B03 | 다른 기능의 데이터를 바꿔야 하면 직접 호출하지 않고 도메인 이벤트를 발행한다. | 기능 사이의 순환을 막는다 ([ADR-0009](adr/0009-feature-boundaries-via-events.md)) |

**좋은 예와 나쁜 예 (CS-B01)**

```java
// 나쁜 예: 서비스가 권한을 판단한다
if (!post.getAuthorId().equals(requester)) {
  throw new BusinessException(ErrorCode.ACCESS_DENIED);
}
post.setTitle(title);

// 좋은 예: 도메인이 판단하고 서비스는 호출만 한다
post.updateBy(requester, new Title(title), new Content(content));
```

### 3.2 도메인 모델

| 번호 | 규칙 | 이유 |
|---|---|---|
| CS-B10 | 값 객체는 `record`로 만들고, compact constructor에서 검증과 정규화(앞뒤 공백 제거 등)를 한다. | 잘못된 값 객체가 만들어질 수 없게 한다 |
| CS-B11 | 애그리게이트의 생성자는 `private`이고, 신규 생성은 업무 의미가 있는 정적 팩토리(`write()`, `provision()`), 영속 상태 복원은 `restore()`로만 한다. `restore()`는 영속성 어댑터에서만 호출한다. | 새로 만드는 경로와 저장된 것을 되살리는 경로의 검증이 다르기 때문이다 |
| CS-B12 | 다른 애그리게이트는 식별자 값 객체(`PostId`, `MemberId`)로만 참조한다. | 애그리게이트 경계를 지키고, 하나를 불러올 때 다른 것까지 끌려오지 않게 한다 |
| CS-B13 | 현재 시각은 서비스에서 `Instant.now()`로 만들어 도메인 메서드에 인자로 넘긴다. 도메인은 시계를 직접 읽지 않는다. 시각은 항상 `Instant`(UTC)로 다룬다. | 도메인 테스트에서 시각을 고정할 수 있다 |
| CS-B14 | 도메인 객체에는 setter를 두지 않는다. 상태 변경은 업무 의미가 있는 메서드(`updateBy`, `deleteBy`)로만 한다. | 상태 변경마다 규칙 검사를 거치게 한다 |

### 3.3 예외와 오류

| 번호 | 규칙 | 이유 |
|---|---|---|
| CS-B20 | 예상된 실패(없음, 권한 없음, 규칙 위반)는 `BusinessException(ErrorCode)`로 던진다. 예외를 던지는 코드는 HTTP 상태를 알지 못한다. | HTTP 변환은 `GlobalExceptionHandler` 한 곳에서만 한다 |
| CS-B21 | 새 오류 코드는 `ErrorCode`와 [프로젝트 SRS §5.1](srs.md#51-오류-코드-목록)에 함께 추가한다. 기존 코드로 의미가 충분하면 새로 만들지 않는다. | 오류 코드는 API 계약이다 |
| CS-B22 | 값 객체의 `IllegalArgumentException`과 `Objects.requireNonNull`은 **프로그래밍 오류**를 뜻한다. 사용자 입력은 요청 DTO의 `jakarta.validation` 제약이 먼저 막아야 하므로, DTO 제약과 값 객체 제약을 같게 유지한다. | 사용자 입력이 값 객체까지 도달하면 `400`이 아니라 `500`이 된다 |
| CS-B23 | 다른 게시글의 댓글처럼 "이 맥락에서 보이지 않는 대상"은 `ACCESS_DENIED`가 아니라 `NOT_FOUND` 계열로 거부한다. | 존재 여부를 노출하지 않는다 (CMT-FR-008의 선례) |
| CS-B24 | `catch`로 예외를 삼키지 않는다. 처리할 수 없으면 그대로 던진다. | 실패가 조용히 사라지면 원인을 찾을 수 없다 |

### 3.4 영속성

| 번호 | 규칙 | 이유 |
|---|---|---|
| CS-B30 | 스키마는 새 Flyway 버전 파일로만 바꾼다. 이미 적용된 마이그레이션 파일은 고치지 않는다. | [ADR-0007](adr/0007-flyway-single-source-of-schema.md) |
| CS-B31 | 여러 요청이 동시에 바꾸는 값(카운터)은 원자적 `UPDATE`로만 바꾸고, JPA 엔티티의 해당 컬럼은 `updatable = false`로 선언한다. | 엔티티 저장이 동시에 바뀐 값을 덮어쓰지 않게 한다 ([ADR-0004](adr/0004-atomic-counter-update.md)) |
| CS-B32 | 중복을 허용하지 않는 저장은 "조회 후 저장" 대신 유니크 제약과 `INSERT ... ON CONFLICT DO NOTHING`으로 하고, 영향받은 행 수나 재조회로 결과를 판정한다. | 동시 요청에서도 정확하고, 제약 위반 예외로 트랜잭션이 깨지지 않는다 ([ADR-0005](adr/0005-database-decides-duplicates.md)) |
| CS-B33 | 벌크 `UPDATE`·`DELETE`·`INSERT`는 `@Modifying(flushAutomatically = true, clearAutomatically = true)`로 선언한다. | flush 없이 clear하면 같은 트랜잭션의 앞선 변경이 유실된다 |
| CS-B34 | 목록 조회의 정렬에는 식별자를 마지막 키로 포함한다. | 같은 시각의 행 순서를 고정해서 페이지 경계의 중복·누락을 막는다 |
| CS-B35 | 목록 조회는 필요한 열만 읽는 프로젝션으로 하고, 연관 데이터는 조인으로 한 번에 읽는다. | 큰 본문을 읽지 않고, N+1 쿼리를 막는다 |
| CS-B36 | JPA 엔티티의 getter와 생성자는 같은 패키지의 매퍼만 쓰도록 `package-private`으로 둔다. 엔티티에 `@Data`, `@EqualsAndHashCode`를 쓰지 않는다. | 엔티티가 어댑터 밖으로 새지 않게 한다 |

### 3.5 트랜잭션

| 번호 | 규칙 | 이유 |
|---|---|---|
| CS-B40 | 쓰기가 없는 서비스 메서드는 `@Transactional(readOnly = true)`로 선언한다. | 의도를 드러내고, 읽기 전용 최적화를 받는다 |
| CS-B41 | 한 유스케이스는 서비스 메서드 하나의 트랜잭션 안에서 끝낸다. 컨트롤러가 서비스 메서드 여러 개를 이어 불러 하나의 업무를 완성하지 않는다. | 중간에 실패했을 때 일부만 반영되는 상태를 막는다 |

### 3.6 웹 계층

| 번호 | 규칙 | 이유 |
|---|---|---|
| CS-B50 | 요청·응답 DTO는 `record`다. 응답 DTO는 도메인에서 만드는 정적 팩토리 `from()`을 가진다. 도메인 객체를 응답으로 직접 반환하지 않는다. | 도메인 변경이 API 계약을 바꾸지 않게 한다 |
| CS-B51 | 생성은 `201 Created`와 `Location`, 수정·삭제·좋아요는 `204 No Content`를 반환한다. | [프로젝트 SRS COM-IF-002](srs.md#5-공통-인터페이스-요구사항) |
| CS-B52 | 현재 회원은 `@CurrentMember MemberId` 파라미터로만 받는다. 컨트롤러에서 `OidcUser`나 `SecurityContextHolder`를 직접 쓰지 않는다. | 기능 코드가 인증 방식을 모르게 한다 |
| CS-B53 | 관리자 여부는 컨트롤러가 `Authentication`의 권한으로 판단해 `boolean`으로 도메인에 넘긴다. | 도메인이 스프링 보안 타입을 모르게 한다 ([ADR-0003](adr/0003-authorization-in-domain.md)) |
| CS-B54 | 요청 DTO의 필드에는 검증 메시지를 한국어로 적는다. | 메시지가 그대로 ProblemDetail의 `errors`로 사용자에게 간다 |

### 3.7 스프링 구성 요소

| 번호 | 규칙 | 이유 |
|---|---|---|
| CS-B60 | 의존성은 생성자로 주입하고, 생성자는 `package-private`으로 둔다. 필드 주입과 Lombok을 쓰지 않는다. | 스프링만 호출하는 생성자를 공개할 이유가 없다 ([ADR-0011](adr/0011-remove-lombok.md)) |
| CS-B61 | 인터페이스는 대체할 구현이 있을 때만 만든다. 인바운드 포트(유스케이스 인터페이스)는 두지 않는다. | [ADR-0010](adr/0010-drop-inbound-ports.md) |

### 3.8 null

| 번호 | 규칙 | 이유 |
|---|---|---|
| CS-B70 | 새 패키지에는 `package-info.java`에 `@NullMarked`를 선언한다. null이 될 수 있는 위치에만 JSpecify `@Nullable`을 붙인다. | ArchUnit이 선언 여부를, NullAway가 계약을 검사한다 |
| CS-B71 | 저장소에서 "없을 수 있는 조회"의 반환은 `Optional`, 필드와 파라미터는 `@Nullable`로 표현한다. `Optional`을 필드나 파라미터로 쓰지 않는다. | `Optional`은 반환값의 의미를 드러내기 위한 타입이다 |

### 3.9 주석과 Javadoc

| 번호 | 규칙 | 이유 |
|---|---|---|
| CS-B80 | Javadoc과 주석은 한국어로 쓴다. 식별자와 코드 요소는 원래 이름을 쓴다. | 팀의 작업 언어와 맞춘다 |
| CS-B81 | Javadoc에는 **계약**을 쓴다: 무엇을 보장하는지, 어떤 경우에 어떤 `ErrorCode`로 실패하는지(`@throws`). 구현 설명은 쓰지 않는다. | 호출하는 쪽이 구현을 읽지 않아도 쓸 수 있게 한다 |
| CS-B82 | 코드 안의 주석은 **왜**만 쓴다. 코드가 무엇을 하는지는 코드로 드러낸다. | "무엇"을 설명하는 주석은 코드가 바뀌면 거짓말이 된다 |

## 4. 테스트 규칙

| 번호 | 규칙 | 이유 |
|---|---|---|
| CS-T01 | 테스트 메서드 이름은 검증하는 규칙을 한국어 문장으로 쓰고 단어를 밑줄로 잇는다 (`작성자가_아니면_수정할_수_없다`). `@DisplayName`은 쓰지 않는다. | 테스트 이름이 곧 요구사항 목록이 되고, QA 체크리스트가 이름으로 테스트를 인용한다 |
| CS-T02 | 규칙은 가능한 가장 낮은 수준에서 검증한다. 도메인 규칙은 도메인 단위 테스트로, 웹 테스트에서는 오류 변환만 확인한다. | [공통 QA 기준 §2](qa-standards.md#2-테스트-수준) |
| CS-T03 | 데이터베이스의 동작(제약, 원자적 UPDATE, `ON CONFLICT`)은 목이 아니라 `IntegrationTestBase`를 상속한 실제 컨테이너로 검증한다. | 목은 데이터베이스가 실제로 하는 일을 검증하지 못한다 |
| CS-T04 | 스프링 없는 서비스 테스트는 저장소 포트를 구현한 인메모리 대역을 직접 만들어 쓴다 (예: `MemberServiceTest`). 목 라이브러리로 호출 횟수를 검증하지 않는다. 단, 웹 슬라이스 테스트(`@WebMvcTest`)에서 컨텍스트에 필요한 빈을 채우기 위한 `@MockitoBean`은 허용한다 (예: `GlobalExceptionHandlerTest`). | 호출 방식이 아니라 결과를 검증해야 리팩터링에 깨지지 않는다 |
| CS-T05 | 결함을 고칠 때는 결함을 재현하는 테스트를 먼저 추가하고, 실패하는 것을 확인한 뒤에 고친다. 그 테스트를 QA 체크리스트에 회귀 항목으로 기록한다. | 같은 결함이 다시 생기는 것을 막는다 |
| CS-T06 | 동시성 테스트는 가상 스레드 실행기로 같은 작업을 동시에 실행하고, 테스트 메서드에는 트랜잭션을 걸지 않는다. | 테스트 트랜잭션이 요청들을 하나로 묶으면 경쟁 상태가 재현되지 않는다 |
| CS-T07 | 테스트 준비 메서드는 `데이터를_준비한다`, `데이터를_비운다`처럼 이름을 짓고, 각 테스트는 다른 테스트의 데이터에 의존하지 않는다. | 실행 순서에 따라 결과가 바뀌지 않게 한다 |

## 5. 프론트엔드 규칙

| 번호 | 규칙 | 이유 |
|---|---|---|
| CS-F01 | 기능은 `features/<기능>/` 아래에 `*-api.service.ts`(HTTP 호출만), `*.store.ts`(상태와 재조회 범위), `pages/`, `components/`로 나눈다. 컴포넌트는 스토어만 주입받고 API 서비스를 직접 쓰지 않는다. | 재조회 범위를 스토어 한 곳이 소유해야 화면 사이의 상태가 어긋나지 않는다 |
| CS-F02 | API 요청·응답 타입은 `core/api/schema.d.ts`(생성 파일)에서만 가져온다. 직접 타입을 정의하지 않고, 생성 파일을 손으로 고치지 않는다. 백엔드 API가 바뀌면 `pnpm gen:api`를 실행한다. | 백엔드와 계약이 어긋나면 컴파일이 실패해야 한다 |
| CS-F03 | 의존성은 `inject()`로 받고, 주입하는 필드는 클래스 맨 위에 둔다. 생성자는 `effect` 등록처럼 주입 외의 초기화에만 쓴다. 앱 전체에서 하나인 서비스는 `@Service()`로 선언한다 (`@Injectable({ providedIn: "root" })`를 쓰지 않는다). 린트: `prefer-inject`, `inject-at-top`, `prefer-service-decorator` | 주입 방식을 하나로 통일한다. 필드는 적힌 순서대로 초기화되므로, 주입이 위에 있어야 다른 필드가 안전하게 쓸 수 있다 |
| CS-F04 | 변경 감지는 Angular 22의 기본값(`OnPush`)을 쓰고, `changeDetection`을 적지 않는다. 기본값을 끄는 설정(`Eager`)은 쓰지 않는다. 상태는 signal로 두고, 서버 상태는 스토어의 `httpResource`로 읽는다. 리소스의 `value()`는 `hasValue()`로 확인한 뒤에 읽는다 (오류 상태에서 읽으면 예외가 난다). 린트: `prefer-on-push-component-change-detection`, `no-uncalled-signals`, `computed-must-return`, `reactive-context-must-read-signal` | 변경 감지 범위를 좁히고, 상태가 어디서 바뀌는지 추적할 수 있게 한다. 기본값을 다시 적으면 AI가 만든 코드와 사람이 쓴 코드가 섞여 보인다 |
| CS-F05 | 새로고침과 링크 공유에서 유지되어야 하는 상태(검색어, 페이지)는 URL 쿼리에 두고, 라우터 입력 바인딩으로 컴포넌트 입력에 받는다. | 상태를 잃지 않는다 (PST-FR-020) |
| CS-F06 | 서버 오류는 `core/api/problem.ts`로 ProblemDetail을 해석해서 다룬다. 응답 본문의 형태를 컴포넌트마다 직접 검사하지 않는다. | 오류 규약(COM-IF-003)의 해석을 한 곳에 모은다 |
| CS-F07 | 단위 테스트의 API 목은 MSW로 만들고, 응답 데이터에는 생성된 API 타입을 붙인다. | 백엔드 계약이 바뀌면 목도 컴파일 오류가 나야 한다 |
| CS-F08 | 사용자에게 보이는 문구와 테스트 이름은 한국어로 쓴다. | 화면 언어와 테스트 언어를 맞춘다 |
| CS-F09 | 컴포넌트의 입력·출력은 `input()`, `output()`, `model()` 함수로, 호스트 바인딩은 데코레이터의 `host` 객체로 선언한다. 템플릿은 내장 제어 흐름(`@if`, `@for`, `@switch`)과 `class`·`style` 바인딩을 쓴다 (`*ngIf`, `ngClass`, `ngStyle`을 쓰지 않는다). 린트: `prefer-signals`, `prefer-output-emitter-ref`, `prefer-host-metadata-property`, `template/prefer-control-flow`, `template/prefer-class-binding`, `template/prefer-style-binding` | Angular 22의 현재 방식으로 통일한다. 예전 방식은 AI가 자주 생성하므로 리뷰가 아니라 린트에서 막는다 |
| CS-F10 | 새 폼은 Signal Forms(`@angular/forms/signals`)로 만든다. 기존 Reactive Forms는 그 폼의 동작을 바꿀 때 함께 옮긴다. | Angular 22부터 Signal Forms가 안정 API이고, 상태를 signal로 둔다는 CS-F04와 맞는다. 옮기기만 하는 변경은 검증 비용에 비해 얻는 것이 없다 |
| CS-F11 | 안정 API만 쓴다. 실험(`@experimental`)·개발자 미리보기(`@developerPreview`) API를 쓰지 않는다. 린트: `no-experimental`, `no-developer-preview` | 메이저 업그레이드(`ng update`) 때 깨질 수 있는 코드를 들이지 않는다 ([ADR-0015](adr/0015-keep-angular-for-frontend.md)) |

## 6. 버전 관리 규칙

| 번호 | 규칙 | 이유 |
|---|---|---|
| CS-G01 | 브랜치는 `<type>/<설명>` 형식이다 (`feature/post-report`, `fix/comment-parent-post-check`). type은 커밋 메시지의 type과 같은 목록을 쓴다. | 브랜치 이름만 보고 변경의 성격을 안다 |
| CS-G02 | 커밋 메시지 본문에는 **왜** 바꿨는지를 쓴다. 무엇을 바꿨는지는 diff가 보여 준다. | 이유는 코드에 남지 않는다 |
| CS-G03 | 코드 변경과 그에 따른 문서 변경(SRS·SDS·QA, ADR, 다이어그램)은 같은 PR에 넣는다. | 문서가 코드보다 늦게 따라오면 아무도 고치지 않는다 |
| CS-G04 | 한 PR에는 한 가지 목적만 담는다. 관련 없는 리팩터링이나 서식 변경을 섞지 않는다. | 리뷰할 수 있는 크기를 유지하고, 되돌릴 때 다른 변경까지 되돌리지 않는다 |

## 7. 예외와 개정

- 규칙을 지킬 수 없는 정당한 이유가 있으면, 그 자리에 **왜 예외인지 주석으로 남긴다.** 도구 규칙이면 억제 설정에 이유를 함께 적는다 (`checkstyle-suppressions.xml`의 예처럼).
- 같은 예외가 반복되면 규칙을 고친다. 규칙을 고칠 때는 이 문서의 버전을 올리고, 프로젝트 전체에 영향을 주는 변경이면 ADR을 함께 남긴다.

## 변경 이력

| 버전 | 일자 | 변경 내용 | 작성자 |
|---|---|---|---|
| 1.0.0 | 2026-10-09 | 최초 작성 (`main` 2c1659d의 도구 설정과 코드 관례를 기준으로 정리) | HseongH |
| 1.1.0 | 2026-10-09 | 모노레포 전환 반영 ([ADR-0014](adr/0014-monorepo-with-gradle-convention-plugins.md)): Java 품질 도구의 설정 위치를 `build-logic`의 컨벤션 플러그인으로, 프론트엔드 설정 위치를 `services/web/`으로 변경 | HseongH |
| 1.2.0 | 2026-10-09 | 프론트엔드 품질 게이트에 `ng build`의 템플릿 검사와 번들 예산 추가 (`pnpm verify`에 포함) | HseongH |
| 1.3.0 | 2026-10-09 | Angular 22 기준으로 프론트엔드 규칙 개정 ([ADR-0015](adr/0015-keep-angular-for-frontend.md)): CS-F03에 `@Service()`와 주입 위치 추가, CS-F04를 기본 `OnPush` 기준으로 변경, CS-F09(컴포넌트 API와 템플릿 문법)·CS-F10(Signal Forms)·CS-F11(안정 API) 추가. 규칙마다 강제하는 린트 규칙을 표시하고 타입 정보 린트를 켬 | HseongH |
| 1.4.0 | 2026-10-10 | CS-F04에 리소스 `value()`를 `hasValue()`로 확인한 뒤 읽는 규칙 추가 (오류 상태에서 화면이 깨지던 결함, TC-PST-054·TC-CMT-046) | HseongH |
