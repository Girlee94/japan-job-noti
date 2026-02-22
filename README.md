# Ready Japan

일본 취업을 준비하는 사용자를 위한 자동화 백엔드 시스템.
일본 IT 업계 동향, 채용 공고, 커뮤니티 게시글을 자동으로 수집하고,
LLM으로 번역/감정분석/요약을 수행한 뒤 매일 텔레그램으로 브리핑을 전송합니다.

## 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Kotlin 1.9.25 |
| Framework | Spring Boot 3.4.2 |
| Build | Gradle (Kotlin DSL) |
| Database | Supabase PostgreSQL |
| JDK | 21 |
| Test | Kotest 5.8.0 + MockK 1.13.14 |

## 아키텍처

```
ready-japan/
├── module-core/              # 순수 도메인 (엔티티, 리포지토리 인터페이스, 예외)
├── module-infrastructure/    # 외부 연동 (DB, 크롤러, LLM, 텔레그램)
├── module-batch/             # 스케줄링 및 배치 작업
├── module-api/               # REST API 엔드포인트
└── docs/                     # 문서 및 SQL 스키마
```

**모듈 의존성:** `core` ← `infrastructure` ← `batch`, `api`

## 핵심 기능

### 데이터 수집 (Crawler)

| 소스 | 대상 | 스케줄 |
|------|------|--------|
| Reddit | r/japanlife, r/movingtojapan, r/japandev | 08:00, 18:00 JST |
| Qiita | 일본/취직/전직/커리어 태그 | 09:00, 19:00 JST |

### LLM 처리

| 기능 | 설명 | 스케줄 |
|------|------|--------|
| 번역 | 일본어 → 한국어 자동 번역 (채용공고, 뉴스, 커뮤니티) | 30분 간격 |
| 감정 분석 | 커뮤니티 게시글 POSITIVE/NEUTRAL/NEGATIVE 분류 | 30분 간격 |
| 일일 요약 | 전일 수집 데이터 LLM 요약 + 텔레그램 전송 | 09:00 JST |

### 데이터 파이프라인

```
크롤링 소스 → 크롤러 → 콘텐츠 저장 → 번역 → 감정분석 → 일일 요약 → 텔레그램 전송
```

## 시작하기

### 사전 요구사항

- JDK 21
- Docker (Supabase PostgreSQL)
- Reddit App (OAuth2 client_credentials)
- Gemini API Key (Google AI Studio)
- Telegram Bot Token

### 환경 변수 설정

```bash
cp .env.example .env.local
vi .env.local
```

주요 환경 변수:

```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/postgres
DB_USERNAME=postgres
DB_PASSWORD=<password>

# Reddit OAuth2
REDDIT_CLIENT_ID=<client-id>
REDDIT_CLIENT_SECRET=<client-secret>

# LLM (Gemini)
LLM_API_KEY=<api-key>
LLM_MODEL=gemini-2.0-flash

# Telegram
TELEGRAM_BOT_TOKEN=<bot-token>
TELEGRAM_CHAT_ID=<chat-id>
```

### 데이터베이스 초기화

```bash
psql -h localhost -p 5432 -U postgres -d postgres -f docs/sql/V1__init_schema.sql
```

### 빌드 및 실행

```bash
# 빌드
./gradlew clean build

# 배치 모듈 실행 (스케줄러)
./gradlew :module-batch:bootRun

# API 모듈 실행 (REST API)
./gradlew :module-api:bootRun

# 테스트
./gradlew test
```

> local 프로필에서는 `app.telegram.enabled=false`, `app.llm.enabled=false`로 외부 서비스가 비활성화됩니다.

## API 엔드포인트

### Health Check

```
GET /api/health
```

### 크롤러

```
POST /api/crawler/reddit/run    # Reddit 수동 크롤링
POST /api/crawler/qiita/run     # Qiita 수동 크롤링
GET  /api/crawler/posts/recent  # 최근 게시글 조회 (?limit=20)
```

### LLM 서비스

```
POST /api/v1/llm/translate      # 번역 수동 트리거
POST /api/v1/llm/sentiment      # 감정 분석 수동 트리거
POST /api/v1/llm/summary        # 일일 요약 생성 (?date=2025-01-15)
POST /api/v1/llm/summary/send   # 일일 요약 생성 + 텔레그램 전송
```

Swagger UI: http://localhost:8080/swagger-ui.html

## 데이터 모델

| 엔티티 | 설명 |
|--------|------|
| `CrawlSource` | 크롤링 소스 설정 (서브레딧, 뉴스 사이트 등) |
| `JobPosting` | 채용 공고 (원문 + 번역) |
| `NewsArticle` | 뉴스/블로그 기사 (원문 + 번역) |
| `CommunityPost` | 커뮤니티 게시글 (번역 + 감정분석) |
| `CrawlHistory` | 크롤링 실행 이력 |
| `DailySummary` | LLM 생성 일일 요약 (DRAFT → SENT) |

## 프로젝트 구조

```
module-core/
├── domain/entity/         # JPA 엔티티 (BaseEntity 상속)
├── domain/entity/enums/   # 상태 열거형
├── domain/repository/     # 리포지토리 인터페이스
└── common/                # ApiResponse, 예외 계층

module-infrastructure/
├── crawler/               # Reddit, Qiita 크롤러
├── external/llm/          # LLM 클라이언트 (Gemini/OpenAI), 번역/감정분석/요약 서비스
├── external/telegram/     # 텔레그램 클라이언트
├── orchestration/         # 오케스트레이션 서비스 (비즈니스 로직 조율)
└── persistence/           # JPA 구현체, 어댑터

module-batch/
└── scheduler/             # Cron/FixedRate 스케줄러

module-api/
└── controller/            # REST 컨트롤러, DTO
```

## 문서

- [설정 가이드](docs/SETUP_GUIDE.md) - 환경 구성 및 외부 서비스 연동
- [데이터베이스 설계](docs/DATABASE_DESIGN.md) - ERD 및 스키마 설명
- [코드 컨벤션](CONVENTIONS.md) - 코딩 표준 및 패턴
