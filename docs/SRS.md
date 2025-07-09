# 📄 Software Requirements Specification (SRS)

## 1. 소개 (Introduction)

### 1.1 목적 (Purpose)

이 문서는 **BBS** 시스템의 소프트웨어 요구사항을 정의하며, 개발자, 기획자, 테스트 담당자 등의 이해관계자 간의 명확한 이해를 돕기 위한 기준 문서로 사용된다.

### 1.2 범위 (Scope)

**BBS**은/는 게시판 기능을 제공하는 웹 애플리케이션으로, 게시글 작성, 댓글 작성, 게시글 검색 등을 수행한다.

### 1.3 용어 정의 (Definitions, Acronyms, and Abbreviations)

| 약어/용어 | 정의                                                              |
| --------- | ----------------------------------------------------------------- |
| CRUD      | Create, Read, Update, Delete                                      |
| REST API  | Representational State Transfer Application Programming Interface |

### 1.4 참조 문서 (References)

- IEEE 830-1998: Software Requirements Specification

---

## 2. 전체적인 설명 (Overall Description)

### 2.1 제품의 관점 (Product Perspective)

**BBS**는 독립형/모듈형/서비스형 시스템이며, 다음과 같은 환경에서 동작한다.  
Vanilla Javascript 프론트엔드 + Node.js(Express) 백엔드, PostgreSQL 데이터베이스 사용.

### 2.2 제품 기능 (Product Functions)

- 게시글 작성 및 관리(CRUD)
- 댓글 작성 및 관리(CRUD)
- 게시글 / 댓글 검색 기능
- 조회수 기능

### 2.3 사용자 특성 (User Characteristics)

- 일반 사용자: 비전문가, 기본적인 웹 사용 가능자

### 2.4 제약사항 (Constraints)

- 비밀번호는 SHA-256로 암호화

---

## 3. 시스템 요구사항 (Specific Requirements)

### 3.1 기능적 요구사항 (Functional Requirements)

| ID     | 요구사항 설명                                                                               |
| ------ | ------------------------------------------------------------------------------------------- |
| FR-001 | 사용자는 작성된 게시글 목록을 조회할 수 있다.                                               |
| FR-002 | 사용자는 로그인없이 게시글을 작성할 수 있다.                                                |
| FR-003 | 사용자는 게시글 작성 시 입력한 패스워드 인증을 통해, 작성한 게시글을 수정 / 삭제할 수 있다. |
| FR-004 | 게시글이 조회된 횟수를 조회할 수 있다.                                                      |
| FR-005 | 사용자는 게시글에 작성된 댓글을 조회할 수 있다.                                             |
| FR-006 | 사용자는 게시글에 댓글을 작성할 수 있다.                                                    |
| FR-007 | 사용자는 댓글 작성 시 입력한 패스워드 인증을 통해, 작성한 댓글을 수정 / 삭제할 수 있다.     |
| FR-008 | 사용자는 게시글 / 댓글을 검색할 수 있다.                                                    |

### 3.2 비기능적 요구사항 (Non-functional Requirements)

| 유형 | 설명                      |
| ---- | ------------------------- |
| 보안 | 비밀번호는 sha-256 암호화 |

### 3.3 외부 인터페이스 요구사항 (External Interface Requirements)

- **사용자 인터페이스(UI)**: 브라우저 기반 웹 UI, 반응형 디자인
- **하드웨어 인터페이스**: 없음 (클라우드 기반)
- **소프트웨어 인터페이스**:
  - 프론트엔드: Javascript
  - 백엔드: Node.js, Express
  - DB: PostgreSQL 17.5
- **통신 인터페이스**: RESTful API 사용 (예: `/api/posts`, `GET /api/reply/:postId`)

---

## 📌 부가 메모

- 문서 버전: v0.1 (초안)
- 작성일: 2025-07-09
- 작성자: 홍성훈
