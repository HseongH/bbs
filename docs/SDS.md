# 🧩 Software Design Specification (SDS)

## 1. 소개 (Introduction)

### 1.1 목적 (Purpose)

이 문서는 **BBS** 시스템의 소프트웨어 설계를 정의하며, 아키텍처 구조, 모듈 분해, 데이터베이스 설계, 인터페이스 설계 등을 포함한다. 개발자 및 유지보수 담당자를 위한 참조 문서로 활용된다.

### 1.2 범위 (Scope)

본 문서는 **BBS**의 전체 아키텍처 및 상세 설계를 다루며, 주요 기능으로는 게시글 작성 및 관리, 댓글 작성 및 관리 등을 포함한다. 문서의 대상 범위는 백엔드, 프론트엔드, DB 구조 및 API 설계를 포함한다.

### 1.3 용어 정의 (Definitions, Acronyms, and Abbreviations)

| 용어 | 정의                              |
| ---- | --------------------------------- |
| API  | Application Programming Interface |
| DTO  | Data Transfer Object              |
| ERD  | Entity-Relationship Diagram       |

### 1.4 참조 문서 (References)

- SRS 문서 v0.1

---

## 2. 시스템 아키텍처 (System Architecture)

### 2.1 시스템 구성도 (High-Level Architecture Diagram)

(아키텍처 다이어그램 이미지 또는 링크)

### 2.2 주요 기술 스택

| 계층       | 기술                               |
| ---------- | ---------------------------------- |
| 프론트엔드 | Javascript ES6+, SCSS, TailwindCSS |
| 백엔드     | Node.js (Express)                  |
| DB         | PostgreSQL 17.5                    |
| 배포       | Docker, Nginx, AWS EC2             |

---

## 3. 모듈 설계 (Module Design)

### 3.1 모듈 개요

| 모듈명      | 설명                       |
| ----------- | -------------------------- |
| BoardModule | 게시글 등록/조회/수정/삭제 |
| ReplyModule | 댓글 등록/조회/수정/삭제   |

### 3.2 모듈 간 관계 (Module Interaction Diagram)

(시퀀스 다이어그램 또는 상호작용 다이어그램 삽입)

---

## 4. 데이터베이스 설계 (Database Design)

### 4.1 ERD(Entity-Relationship Diagram)

(ERD 다이어그램 첨부 또는 링크)

### 4.2 주요 테이블 정의

#### 📄 board 테이블

| 필드명     | 타입         | 설명            |
| ---------- | ------------ | --------------- |
| id         | UUID         | PK              |
| title      | VARCHAR(255) | 게시글 제목     |
| contents   | TEXT         | 게시글 내용     |
| username   | VARCHAR(255) | 작성자          |
| password   | VARCHAR(255) | 해시된 비밀번호 |
| views      | INT          | 조회수          |
| created_at | DATETIME     | 생성일자        |
| updated_at | DATETIME     | 수정일자        |

#### 📄 reply 테이블

| 필드명     | 타입         | 설명            |
| ---------- | ------------ | --------------- |
| id         | UUID         | PK              |
| board_id   | UUID         | FK → board.id   |
| contents   | TEXT         | 댓글 내용       |
| username   | VARCHAR(255) | 작성자          |
| password   | VARCHAR(255) | 해시된 비밀번호 |
| created_at | DATETIME     | 생성일자        |

---

## 5. 인터페이스 설계 (Interface Design)

### 5.1 API 명세 (RESTful)

#### 📌 POST /api/users/login

- **설명**: 로그인 요청
- **Request Body**:

```json
{
  "email": "user@example.com",
  "password": "********"
}
```

- **Response**:

```json
{
  "token": "JWT_TOKEN",
  "user": { "id": 1, "email": "user@example.com" }
}
```

#### 📌 GET /api/todos

- **설명**: 사용자의 할 일 목록 조회
- **Query Params**: `?completed=true`

---

## 6. UI 설계 (간략)

> 전체 UI는 별도의 UI 문서 참조. 이 문서에서는 각 페이지와 컴포넌트 구조만 요약.

| 페이지             | 주요 컴포넌트                                                                                                  |
| ------------------ | -------------------------------------------------------------------------------------------------------------- |
| 게시글 목록 페이지 | 작성된 게시글 목록, 검색창, 게시글 작성 버튼                                                                   |
| 게시글 작성 페이지 | 작성자명 입력, 비밀번호 입력, 제목 입력, 상세내용 입력, 저장 버튼, 캡차 검증                                   |
| 게시글 상세 페이지 | 선택한 게시글, 게시글 댓글 목록, 댓글 작성자 입력, 댓글 비밀번호 입력, 댓글내용 입력, 댓글 작성버튼, 캡차 검증 |

---

## 7. 설계 원칙 및 규칙 (Design Guidelines)

- SOLID 원칙 준수
- 비즈니스 로직은 서비스 계층에만 작성
- 공통 에러 핸들러 사용
- 비동기 통신 시 Promise + async/await 패턴 통일
- Response DTO 표준화

---

## 8. 부록 (Appendices)

- 전체 폴더 구조 예시
- Swagger URL 또는 스크린샷
- 라이브러리 목록 및 버전

---

## 📌 문서 메타 정보

- 문서 버전: v0.1 (초안)
- 작성일: 2025-07-10
- 작성자: 홍성훈
