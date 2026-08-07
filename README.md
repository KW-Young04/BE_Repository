# CODEE

**Github 연동 기반 UI/UX적 문제 분석 및 코드 개선 서비스**

정보융합학부 04학번 모임 · **영포(04)티**

---

## 📌 프로젝트 소개 (Overview)

CODEE는 GitHub 레포지토리 URL만 입력하면 **AI가 소스 코드를 분석해 웹 접근성(WCAG 2.2) 준수 여부를 진단**하고, 개선 방안을 코드 수정 제안까지 이어서 제공하는 **QA 보조 도구(QA Assistant Tool)**입니다.

별도의 회원가입 없이 GitHub OAuth 로그인만으로 사용 가능하며, 분석 → 디자인/코드 수정 → Commit/Push까지 하나의 서비스 안에서 해결하는 것을 목표로 합니다.

## 🧩 배경 및 문제 정의 (Background)

1인 개발 혹은 소규모 팀 프로젝트에서는 UI/UX 및 웹 접근성 문제를 전문적으로 분석하기 어렵고, 감각에 의존한 개선이 이루어지는 경우가 많습니다. 또한:

- **UI/UX 분석의 전문성 부재** — 접근성 문제를 정량적으로 판단할 도구가 부족함
- **디자인과 코드의 단절** — Figma AI, Cursor 등은 디자인 툴과 코드 툴이 분리되어 있어 수정 시 불필요한 비용 발생
- **AI 디자인-코드 도구의 한계** — Claude, Lovable 등 AI 기반 도구는 복잡한 UI/UX·접근성 문제 개선에 한계가 있음

CODEE는 **"만드는 것"이 아니라 "이미 존재하는 웹 페이지를 분석하고 개선하는 것"**에 초점을 맞춘 도구로, Webflow(노코드 제작 플랫폼)나 Figma Dev Mode(디자인 명세 확인)와는 지향점이 다릅니다.

## 🎯 핵심 기능 (Key Features)

| 기능 | 설명 |
| --- | --- |
| 전과정 자동화 분석 | GitHub 코드 추출부터 UI 렌더링까지 전 과정을 자동화하여 분석하고 UX 개선안을 즉시 제시 |
| WCAG 기준 평가 | WCAG 2.2 기준에 따른 상세 점수/등급 제공 및 주요 이슈 요약 |
| All-in-One 에디팅 | 디자인 모드(GUI 수정)와 코드 모드(직접 수정)를 결합한 통합 에디터 + AI 챗봇으로 분석-수정-커밋까지 한 번에 해결 |
| 실시간 접근성 체크 | 코드 수정 중 정적 룰 기반 debounce 체크로 즉시 이슈 반영 (DB 미저장, UI 트랜지언트 표시) |
| AI 챗봇 | 1:1 대화 피드백 + 스크린샷 첨부 기반 시각적 요소(색상 대비 등) 분석 요청 |
| 커밋/푸시 | 코드 Diff 비교 후 컨벤션에 따라 커밋 메시지 작성, GitHub에 직접 반영 |

## 🛠 기술 스택 (Tech Stack)

**Backend & Engine**
- Spring Boot 4.0.5 / Java 21 / Gradle
- Spring Data JPA / MySQL 8.0.45
- Spring Security + OAuth2 Client (GitHub 로그인)
- JWT (jjwt 0.12.6)
- Playwright(Java) 1.45.0 — 렌더링 스냅샷
- springdoc-openapi 3.0.3 (Swagger UI)
- Gemini 2.5 Flash API — AI 분석 (`LlmClient` 인터페이스로 추상화 예정)

**Frontend** (별도 레포)
- TypeScript, React, Tailwind CSS
- html2canvas — 스크린샷 캡처

**개발 환경**
- Windows, VSCode
- API 테스트: Swagger, Thunder Client (Postman 미사용)

## 🏗 시스템 아키텍처 (Architecture)

### 핵심 분석 파이프라인 (5단계)

GitHub API 파일 수집        (GithubRepositoryService)
iframe 렌더링 + html2canvas 스크린샷   (프론트엔드)
WCAG 정적 룰 체커 실행       (WcagCheckerRegistry, AccessibilityRule 구현체)
Gemini AI 분석 (스크린샷 + 코드)   (LlmClient 구현체)
DB 저장                      (WcagAnalysisService)


전체 파이프라인은 "분석" 버튼 클릭 시 한 번에 실행됩니다. 실시간 debounce 체크는 별도 흐름으로, 정적 룰(`aiReviewRequired=false`)만 실행하며 AI 호출·DB 저장을 포함하지 않습니다.

### WCAG 룰 구현 방식

| 방식 | 설명 |
| --- | --- |
| 코드 (Code) | 정적 분석만으로 완전 처리 (예: 색상 대비 수치 계산) |
| 코드+AI (Code+AI) | 존재 여부는 코드로, 의미·맥락 판단은 AI |
| AI | 코드만으론 판단 불가, AI 전담 |

어댑터 패턴(`AccessibilityRule` → `WcagChecker`)으로 구현되어 있으며, `WcagCheckerRegistry`가 Spring `@Component` 체커를 자동 등록합니다.

## 🗄 DB 스키마 요약

| 테이블 | 설명 |
| --- | --- |
| `GITHUB_USERS` | GitHub OAuth 로그인 사용자 |
| `REPOSITORIES` | 연결된 GitHub 저장소 정보 |
| `WCAG_ITEMS` | WCAG 2.2 항목 마스터 데이터 (87건, 고정 PK) |
| `ANALYSIS_WCAG_RESULTS` | 항목별 PASS/FAIL/NA 결과 |
| `ANALYSIS_ISSUES` | FAIL 항목의 상세 이슈 |
| `ANALYSIS_ISSUES_LOCATIONS` | 이슈 발생 위치 및 AI 개선 제안 |
| `CHAT_MESSAGES` | 사용자-AI 대화 기록 |

## 🚀 실행 방법 (Getting Started)

### 1. 요구사항 (Requirements)
- Java 21
- MySQL 8.0.45
- Gemini API Key
- GitHub OAuth App (Client ID / Secret)

### 2. 환경변수 설정

프로젝트 루트에 `application-secret.properties` 파일을 생성하고 아래 항목을 채워주세요. (`application-secret.properties.example` 참고)

```properties
# MySQL 연결 정보
spring.datasource.url=jdbc:mysql://localhost:3306/{DB이름}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&createDatabaseIfNotExist=true
spring.datasource.username={DB_사용자}
spring.datasource.password={DB_비밀번호}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Gemini API
gemini.api.key={GEMINI_API_KEY}
gemini.api.base-url=https://generativelanguage.googleapis.com/v1beta
gemini.api.model=gemini-2.5-flash

# JWT
app.jwt.secret={32자 이상의 랜덤 문자열}
app.jwt.access-token-expiration=3600000

# GitHub OAuth2
spring.security.oauth2.client.registration.github.client-id={GITHUB_CLIENT_ID}
spring.security.oauth2.client.registration.github.client-secret={GITHUB_CLIENT_SECRET}
spring.security.oauth2.client.registration.github.scope=repo,read:user,user:email

# 로그인 후 리다이렉트
app.oauth2.redirect-uri=http://localhost:5173/auth/callback
```

> ⚠️ 민감 정보는 반드시 `application-secret.properties`에만 작성하며, 이 파일은 `.gitignore`에 포함되어 Git에 커밋되지 않습니다.

### 3. 빌드 및 실행

```bash
# Windows
gradlew.bat bootRun

# macOS / Linux
./gradlew bootRun
```

서버는 기본적으로 `http://localhost:8080`에서 실행됩니다.

> DB 스키마(`schema.sql`)와 WCAG 마스터 데이터(`wcag.sql`)는 서버 기동 시 자동으로 초기화됩니다.

### 4. API 문서 확인 (Swagger)
http://localhost:8080/swagger-ui/index.html

OpenAPI 명세(JSON): `http://localhost:8080/v3/api-docs`

## 📡 주요 API 엔드포인트 (API Endpoints)

| Method | Endpoint | 설명 |
| --- | --- | --- |
| GET | `/api/auth/login` | GitHub OAuth 로그인 리다이렉트 |
| GET | `/api/github/repositories/recent` | 최근 연결한 GitHub 저장소 목록 조회 |
| GET | `/api/repositories/tree` | 저장소 파일 트리 조회 |
| GET | `/api/repositories/file` | 저장소 내 특정 파일 조회 |
| POST | `/api/snapshots` (multipart) | 렌더링 스냅샷(스크린샷) 업로드 |
| POST | `/api/analysis/wcag` (multipart) | WCAG 분석 실행 및 결과 DB 저장 |
| GET | `/api/git/status` | Git 변경 파일 및 현재 브랜치 조회 |
| GET | `/api/git/diff` | 선택 파일 diff 조회 |
| GET | `/api/git/branches` | 로컬 브랜치 목록 조회 |
| POST | `/api/git/commit` | 선택 파일 커밋 |
| POST | `/api/git/push` | 현재 브랜치 push |
| POST | `/api/git/commit-and-push` | 선택 파일 커밋 후 push |

> 세부 요청/응답 스펙은 Swagger UI에서 실시간으로 확인 가능합니다.

Git API가 작업할 저장소는 `GIT_REPOSITORY_PATH` 환경변수로 지정합니다. 지정하지 않으면 서버 실행 위치를 사용합니다.

## 📁 폴더 구조 (Folder Structure)
domain/
└── {도메인명}/
├── entity        # JPA 엔티티
├── repository    # Spring Data JPA Repository
├── service       # 비즈니스 로직
├── controller    # REST API
├── dto           # 요청/응답 DTO
└── checker       # WCAG 정적 분석 룰 (analysis 도메인 전용)

## 👥 팀원 소개 (Team)

| 이름 | 역할 |
| --- | --- |
| 유아름 | 팀장 / 백엔드 |
| 이은송 | 디자인 / 프론트엔드 |
| 전서연 | 프론트엔드 |
| 엄태성 | 백엔드 |
| 이하랑 | AI / 백엔드 |

## 🗺 향후 계획 (Roadmap)

- 웹 접근성 중심의 MVP 완성
- 접근성 문제 자동 도출 및 결과 직관적 시각화 기능 구체화
- 색상 대비, 폰트 크기, 버튼 크기, 대체 텍스트 등 명확한 기준 기반 분석 기능 고도화
- 개선 제안 ↔ 수정 결과 연결 — 실제 개선 과정까지 추적 가능한 흐름으로 발전
- 개선 전·후 비교 및 사용성 평가를 통한 서비스 실효성 검증
- 이후 필요 시 UX/UI 및 SEO 요소까지 확장 검토

---

*UX/UI 분석 및 코드 개선 서비스 — CODEE*
