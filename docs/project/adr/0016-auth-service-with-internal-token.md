# ADR-0016. 인증은 auth 서비스가 맡고, 다른 서비스에는 auth가 서명한 내부 토큰으로 사용자를 전달한다

- 상태: Proposed
- 일자: 2026-10-10
- 관련: COM-NFR-001~003, COM-NFR-007~009, COM-NFR-020, 021, 036, COM-IF-007, 008, COM-CON-003, 004 (PRJ-SRS 1.7.0), PRJ-SDS 1.9.0 §7·§9·§10, MEM-SRS 1.3.0, MEM-SDS 2.0.0, [ADR-0008](0008-oidc-bff-and-redis-session.md), [ADR-0014](0014-monorepo-with-gradle-convention-plugins.md)

## 맥락

**이 결정의 가장 큰 이유는 Keycloak과의 연결을 auth 한 서비스에만 두는 것이다.**

- 인증을 별도 서비스로 나누는 가장 흔한 방식은, 진입점이 Keycloak 액세스 토큰을 그대로 업무 서비스에 넘기고 업무 서비스가 그 토큰을 직접 검증하는 것이다. 그러면 업무 서비스(board)는 **실행 중에 Keycloak에 연결해야 한다.** 토큰의 서명을 검증하려면 Keycloak의 공개키가 필요하고, 그 공개키는 Keycloak에서 가져와야 하기 때문이다.
  - board의 설정에 Keycloak의 issuer 주소가 들어간다.
  - board가 Keycloak의 공개키(JWKS)를 네트워크로 가져온다. Keycloak이 응답하지 않으면 board는 토큰을 검증하지 못한다.
  - Keycloak이 서명 키를 바꾸면 board가 그 변화를 따라가야 한다.
  - board가 Keycloak 고유의 토큰 형식(`iss`, `realm_access.roles` 같은 클레임 이름)을 해석해야 한다. IdP를 바꾸면 board도 고쳐야 한다.
- 공개키를 board 설정에 고정하거나 auth가 Keycloak의 공개키를 중계해도, board가 검증하는 대상은 여전히 "Keycloak이 서명한 토큰"이다. 연결 방식만 바뀔 뿐 board가 Keycloak에 의존한다는 사실은 그대로다. **board가 Keycloak을 전혀 모르려면, board가 검증하는 토큰을 Keycloak이 아닌 쪽에서 서명해야 한다.**

그 밖의 맥락은 다음과 같다.

- 지금은 board 하나가 로그인(OIDC), 세션, CSRF, URL 접근 규칙, 회원 생성, 역할 매핑을 모두 맡는다([ADR-0008](0008-oidc-bff-and-redis-session.md)). 서비스를 늘리면 각 서비스가 같은 인증 코드를 갖거나, 어느 한 서비스가 다른 서비스의 인증을 대신해야 한다.
- 프로젝트는 진입점 프록시(Traefik) 뒤에 서비스를 두는 구조로 옮겨 가고 있다([ADR-0014](0014-monorepo-with-gradle-convention-plugins.md)의 맥락). Traefik의 오픈소스판은 OIDC 로그인과 토큰 클레임 기반 인가를 하지 못하므로, 그 일을 맡을 서비스가 필요하다.

## 결정

**Keycloak에 연결하는 서비스는 auth 하나뿐이다. board를 비롯한 업무 서비스는 Keycloak에 연결하지 않고, auth가 서명한 내부 토큰만 검증한다.** 아래의 내용은 모두 이 원칙을 지키기 위한 구성이다.

### auth 서비스

- `services/auth`를 새로 만든다(Spring Boot, Spring Security).
- 로그인은 지금과 같은 BFF 방식이다. auth가 Authorization Code 흐름을 처리하고, Keycloak 토큰은 auth의 세션(Valkey)에만 둔다. 브라우저에는 세션 쿠키와 CSRF 쿠키만 간다. [ADR-0008](0008-oidc-bff-and-redis-session.md)의 원칙은 그대로이고, 그 일을 하는 서비스만 board에서 auth로 바뀐다.
- auth는 Traefik의 ForwardAuth 대상이 된다. API 요청마다 Traefik이 원래 요청의 메서드와 경로(`X-Forwarded-Method`, `X-Forwarded-Uri`)와 헤더를 auth에 보내고, auth가 다음을 판정한다.
  - 인증과 URL 단위 인가: 지금 board의 URL 접근 규칙을 옮긴다.
  - CSRF: 세션 쿠키가 auth의 것이므로 CSRF 검증도 auth가 한다. 원래 요청의 메서드를 기준으로 판정한다.
  - 전달 경로의 모호성: 인코딩된 슬래시, `..`, `;`처럼 auth와 board가 서로 다른 경로로 해석할 수 있는 값은 판정하지 않고 거부한다.
- 거부할 때는 auth가 ProblemDetail로 응답하고, Traefik이 그 응답을 브라우저에 그대로 전달한다.

### 내부 토큰

- 통과시킬 때 auth는 **auth가 서명한 내부 JWT**를 `Authorization: Bearer`로 돌려주고, Traefik이 이 헤더를 업무 서비스(board)에 붙인다. 비로그인 사용자의 공개 요청은 토큰 없이 통과시킨다.
- 내부 토큰에는 bbs가 정한 클레임만 담는다: 발급자, 대상(`bbs`), 외부 사용자 식별자, 닉네임, 이메일, bbs가 정의한 역할(`USER`, `ADMIN`). IdP 고유의 형식은 auth가 이 형식으로 바꾼다.
- 토큰은 요청마다 새로 서명하고 60초 동안만 유효하다. 토큰은 Traefik에서 업무 서비스로 가는 한 번의 요청에만 쓰인다. 로그아웃하면 다음 요청부터 토큰이 나가지 않으므로 폐기 목록이 필요 없다.
- 서명은 RS256이다. auth가 공개키를 JWKS(`/.well-known/jwks.json`)로 내부에만 제공하고, 업무 서비스는 이 공개키로만 토큰을 검증한다. 업무 서비스가 연결하는 곳은 auth의 공개키 주소뿐이고, 업무 서비스의 설정과 코드에는 Keycloak의 주소도 클레임 형식도 없다.
- 서명 키는 설정으로 받는다. 설정이 없으면 `local` 프로필에서만 시작할 때 임시 키를 만들고, 그 밖의 프로필에서는 시작을 거부한다.

### 업무 서비스(board)

- board는 OAuth2 Resource Server가 되어 내부 토큰만 검증한다. 로그인, 세션, CSRF, OIDC 연동 코드는 board에서 사라진다.
- 회원은 board가 만든다. 처음 보는 사용자 식별자의 토큰이 오면 토큰의 클레임으로 회원을 만든다(JIT). 회원을 만드는 시점이 "로그인 직후"에서 "첫 인증된 API 요청"으로 바뀐다.
- 역할은 내부 토큰에서 읽는다. 소유권 판단은 지금처럼 도메인에 있다([ADR-0003](0003-authorization-in-domain.md)).
- 화면 경로를 `index.html`로 돌려주던 처리(SPA 포워딩)는 진입점의 라우팅으로 옮기고 board에서 뺀다. board는 API만 제공한다.

### 진입점(Traefik)

이번 결정에서는 Traefik을 **라우팅과 ForwardAuth 연결에만** 쓴다. 경로 체계 개편(`/{audience}/{module}/api/**`)과 그 밖의 헤더 정리는 다음 단계에서 다룬다.

- 브라우저는 Traefik 한 곳으로만 접속한다. `/oauth2`, `/login`, `/logout`은 auth로, `/api`는 ForwardAuth를 거쳐 board로, 나머지는 화면(web)으로 보낸다.
- `/api` 경로에서는 ForwardAuth보다 먼저 브라우저가 보낸 `Authorization` 헤더를 지운다. 브라우저가 내부 토큰 자리를 차지하는 경로를 막기 위해서다.
- auth가 통과 응답에 실은 CSRF 쿠키(`XSRF-TOKEN`)는 브라우저 응답에 함께 전달한다. ForwardAuth는 기본적으로 auth의 `Set-Cookie`를 버리므로 명시해야 한다.

## 검토한 대안

| 대안 | 기각 이유 |
|---|---|
| Keycloak 액세스 토큰을 그대로 board에 전달 (board가 Keycloak 공개키로 검증) | 가장 흔하고 단순한 방식이지만, **board가 실행 중에 Keycloak에 연결해야 한다**(issuer 주소 설정, 공개키 조회). Keycloak이 응답하지 않으면 board가 토큰을 검증하지 못하고, board가 Keycloak의 토큰 형식(`iss`, 역할 클레임 이름)도 해석해야 한다. 이 결정이 피하려는 의존 그 자체다. 공개키를 고정하거나 auth가 JWKS를 중계해도 "Keycloak이 서명한 토큰"을 검증한다는 사실은 바뀌지 않는다 |
| auth가 평문 헤더(`X-User-Sub`, `X-User-Roles`)로 전달 | 구현은 가장 단순하지만 board가 "이 헤더는 진입점을 거쳐 왔다"는 네트워크 경계를 믿어야 한다. board 포트에 직접 접근하면 신원을 위조할 수 있다 |
| auth가 공유 비밀키로 헤더에 HMAC 서명 | 자체 형식의 토큰을 새로 만드는 셈이다. JWT보다 나은 점이 없고, 서비스마다 비밀키를 나눠 가져야 한다 |
| board에 인증을 남기고 서비스마다 같은 인증 코드를 둠 | 서비스가 늘 때마다 OIDC 설정, 세션, CSRF가 복사되고 서로 어긋난다 |
| 진입점을 화면 개발 서버로 유지하고 그 뒤에 Traefik을 둠 | 프록시가 두 겹이 되어 `X-Forwarded-*`와 redirect URI의 기준이 모호해진다. 운영 구성과 다른 경로로 개발하게 된다 |
| auth와 Traefik을 이번에 경로 체계 개편까지 함께 진행 | 한 번에 결정할 것이 너무 많아진다. 인증 분리를 먼저 끝내고 E2E로 검증한 뒤 경로를 바꾸는 편이 실패 원인을 좁히기 쉽다 |

## 결과

- 좋은 점: board는 Keycloak에 연결하지 않는다. Keycloak이 멈춰도 이미 로그인한 사용자의 요청은 auth의 세션과 내부 토큰으로 계속 처리된다. IdP 연동이 auth 한 곳에 갇혀서, IdP를 바꿔도 업무 서비스는 바뀌지 않는다. 업무 서비스는 서명된 토큰만 믿으므로 진입점을 우회한 요청의 신원 위조를 거부한다. 새 업무 서비스는 Resource Server 설정과 공개키 주소만으로 인증을 얻는다.
- 나쁜 점: auth가 서명 키를 관리해야 한다. 모든 API 요청이 auth를 한 번 더 거치므로 지연이 늘고, auth가 멈추면 공개 조회까지 멈춘다. 로그인과 회원 생성의 시점이 달라져(로그인은 auth, 회원 생성은 board의 첫 요청) 두 서비스를 함께 봐야 흐름이 보인다. Keycloak에서 역할을 바꾸면 다시 로그인해야 반영된다(지금과 같다). 개발 환경에서 실행할 프로세스가 하나 늘어난다.
- 재검토 조건: auth를 여러 인스턴스로 띄울 때(키 공유 방식), 서명 키를 교체해야 할 때(여러 키를 함께 공개하는 기간), 업무 서비스가 늘어 대상(`aud`)을 서비스마다 나눠야 할 때, ForwardAuth 지연이 문제가 될 때.
