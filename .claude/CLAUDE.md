# Ready Japan Project Context

## 프로젝트 개요
일본 취업을 준비하는 사용자를 위한 자동화 백엔드 시스템.
일본 취업 동향(IT 업계 트렌드, 채용 공고, 관련 뉴스 등) 데이터를 자동으로 수집 및 요약하여
매일 정해진 시간에 텔레그램으로 전송합니다.

## 기술 스택
- **언어**: Kotlin 1.9.25
- **프레임워크**: Spring Boot 3.4.2
- **빌드**: Gradle (Kotlin DSL)
- **데이터베이스**: Supabase PostgreSQL
- **JDK**: 21

## 멀티 모듈 구조

```
ready-japan/
├── module-core/           # 순수 도메인 로직, 엔티티, 공통 유틸리티
├── module-infrastructure/ # 외부 시스템 연동 (DB, API, 크롤러)
├── module-batch/          # 스케줄링, 배치 작업
└── module-api/            # REST API (추후 확장용)
```

### 모듈 의존성
- `module-core`: 의존성 없음 (순수 도메인)
- `module-infrastructure`: core에만 의존
- `module-batch`, `module-api`: core, infrastructure에 의존

## 코드 컨벤션
**반드시 `CONVENTIONS.md` 파일을 참조하여 모든 코드를 작성해야 합니다.**

주요 규칙:
1. 생성자 주입 사용 (필드 주입 금지)
2. Entity는 비즈니스 로직 캡슐화
3. DTO는 `data class` + `companion object.from()` 패턴
4. 예외는 `BusinessException` 계층 사용
5. 로깅은 `KotlinLogging.logger {}` 사용
6. Null 처리는 Elvis 연산자, Safe call 활용 (`!!` 금지)

## 핵심 기능
1. **데이터 수집**: Reddit/Qiita 크롤러 (OAuth2 client_credentials, 08:00/18:00 JST)
2. **번역**: `TranslationService` — LLM(Gemini/OpenAI)으로 일→한 번역 (30분 간격 배치)
3. **감정 분석**: `SentimentAnalysisService` — 커뮤니티 게시글 감정 분류 (30분 간격 배치)
4. **일간 요약**: `SummarizationService` — 전일 데이터 LLM 요약 생성 (09:00 JST)
5. **알림 발송**: `TelegramClient` — 일간 요약 텔레그램 전송

## 주요 Entity
- `CrawlSource`: 크롤링 소스 설정 (서브레딧, 뉴스 사이트 등)
- `CommunityPost`: 커뮤니티 게시글 (Reddit 등, 감정분석/번역 포함)
- `JobPosting`: 채용 공고 (이중 언어: 원문 + 번역)
- `NewsArticle`: 뉴스 기사 (이중 언어: 원문 + 번역)
- `CrawlHistory`: 크롤링 실행 이력
- `DailySummary`: LLM 생성 일간 요약 (텔레그램 발송 상태 포함)

모든 엔티티는 `BaseEntity`(createdAt, updatedAt — JPA Auditing)를 상속.

## 환경 변수
```
# Database
DATABASE_URL=postgresql://<supabase-url>
DB_USERNAME=
DB_PASSWORD=

# Telegram
TELEGRAM_BOT_TOKEN=
TELEGRAM_CHAT_ID=

# LLM (Gemini 기본, OpenAI 대안)
LLM_PROVIDER=gemini
LLM_API_KEY=
LLM_MODEL=gemini-2.0-flash

# Reddit OAuth2
REDDIT_CLIENT_ID=
REDDIT_CLIENT_SECRET=
```

local 프로필에서는 `app.telegram.enabled=false`, `app.llm.enabled=false`로 외부 서비스 비활성화.

## 개발 명령어
```bash
# 빌드
./gradlew build

# 테스트
./gradlew test

# 특정 모듈 실행
./gradlew :module-batch:bootRun
./gradlew :module-api:bootRun
```