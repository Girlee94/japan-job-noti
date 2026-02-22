# Ready Japan 데이터베이스 설계

> 이 문서는 프로젝트의 데이터베이스 스키마 설계를 정의합니다.
> 데이터베이스: Supabase PostgreSQL

## 개요

### 데이터 흐름

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           데이터 흐름                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  [크롤링 소스]  →  [크롤러]  →  [채용/뉴스/커뮤니티]  →  [요약]  →  [발송] │
│   CrawlSource      Crawler     JobPosting            DailySummary      │
│                       ↓        NewsArticle                ↓            │
│                 CrawlHistory   CommunityPost         Telegram          │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 수집 대상

| 카테고리 | 소스 | 우선순위 |
|----------|------|----------|
| 커뮤니티 | Reddit (r/japanlife, r/movingtojapan, r/japandev) | 1순위 |
| 채용 | Indeed Japan | 2순위 |
| 커뮤니티/블로그 | Qiita | 3순위 |
| 뉴스 | IT Media, TechCrunch Japan | 4순위 |

### 언어 정책

- **수집 언어**: 일본어(ja), 한국어(ko)
- **번역**: 일본어 → 한국어 자동 번역 (LLM 활용)
- **저장**: 원문 + 번역문 모두 저장

---

## Entity 목록

| Entity | 테이블명 | 설명 |
|--------|----------|------|
| CrawlSource | crawl_sources | 크롤링 소스 관리 |
| JobPosting | job_postings | 채용 공고 |
| NewsArticle | news_articles | 뉴스 기사 |
| CommunityPost | community_posts | 커뮤니티 글 |
| CrawlHistory | crawl_histories | 크롤링 이력 |
| DailySummary | daily_summaries | 일간 요약 |

---

## 1. crawl_sources (크롤링 소스)

데이터를 수집할 웹사이트/API 정보를 관리합니다.

### 스키마

```sql
CREATE TABLE crawl_sources (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    url VARCHAR(500) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    platform VARCHAR(20),
    cron_expression VARCHAR(50) DEFAULT '0 0 8 * * *',
    enabled BOOLEAN DEFAULT true,
    config JSONB,
    last_crawled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_crawl_sources_enabled ON crawl_sources(enabled);
CREATE INDEX idx_crawl_sources_type ON crawl_sources(source_type);
```

### 필드 설명

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| id | BIGSERIAL | O | PK |
| name | VARCHAR(100) | O | 소스명 (예: "Reddit r/japanlife") |
| url | VARCHAR(500) | O | 크롤링 대상 URL |
| source_type | VARCHAR(20) | O | JOB_SITE, NEWS_SITE, COMMUNITY, API |
| platform | VARCHAR(20) | X | 세부 플랫폼 (REDDIT, TWITTER, QIITA 등) |
| cron_expression | VARCHAR(50) | X | 크롤링 주기 |
| enabled | BOOLEAN | O | 활성화 여부 |
| config | JSONB | X | 추가 설정 (필터 조건 등, 민감 정보는 환경변수로 관리) |
| last_crawled_at | TIMESTAMP | X | 마지막 크롤링 시각 |
| created_at | TIMESTAMP | O | 생성일 |
| updated_at | TIMESTAMP | O | 수정일 |

### Enum: SourceType

```kotlin
enum class SourceType {
    JOB_SITE,      // 채용 사이트
    NEWS_SITE,     // 뉴스 사이트
    COMMUNITY,     // 커뮤니티
    API            // 외부 API
}
```

### Enum: CommunityPlatform

```kotlin
enum class CommunityPlatform {
    REDDIT,        // Reddit
    TWITTER,       // Twitter/X
    FIVECH,        // 5ch
    QIITA,         // Qiita
    ZENN,          // Zenn
    NOTE,          // Note
    DISCORD,       // Discord
    OTHER          // 기타
}
```

---

## 2. job_postings (채용 공고)

크롤링으로 수집된 채용 공고 정보입니다.

### 스키마

```sql
CREATE TABLE job_postings (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT NOT NULL REFERENCES crawl_sources(id) ON DELETE RESTRICT,
    external_id VARCHAR(200) NOT NULL,
    title VARCHAR(500) NOT NULL,
    title_translated VARCHAR(500),
    company_name VARCHAR(200) NOT NULL,
    location VARCHAR(100),
    employment_type VARCHAR(20),
    salary VARCHAR(200),
    description TEXT,
    description_translated TEXT,
    requirements TEXT,
    requirements_translated TEXT,
    original_url VARCHAR(1000) NOT NULL,
    language VARCHAR(10) DEFAULT 'ja',
    posted_at DATE,
    expires_at DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_job_postings_source_external UNIQUE (source_id, external_id)
);

CREATE INDEX idx_job_postings_status ON job_postings(status);
CREATE INDEX idx_job_postings_created ON job_postings(created_at DESC);
CREATE INDEX idx_job_postings_posted ON job_postings(posted_at DESC);
CREATE INDEX idx_job_postings_company ON job_postings(company_name);
```

### 필드 설명

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| id | BIGSERIAL | O | PK |
| source_id | BIGINT | O | FK → crawl_sources |
| external_id | VARCHAR(200) | O | 원본 사이트 고유 ID |
| title | VARCHAR(500) | O | 채용 제목 (원문) |
| title_translated | VARCHAR(500) | X | 채용 제목 (번역) |
| company_name | VARCHAR(200) | O | 회사명 |
| location | VARCHAR(100) | X | 근무지 |
| employment_type | VARCHAR(20) | X | 고용 형태 |
| salary | VARCHAR(200) | X | 급여 정보 |
| description | TEXT | X | 상세 설명 (원문) |
| description_translated | TEXT | X | 상세 설명 (번역) |
| requirements | TEXT | X | 자격 요건 (원문) |
| requirements_translated | TEXT | X | 자격 요건 (번역) |
| original_url | VARCHAR(1000) | O | 원본 링크 |
| language | VARCHAR(10) | O | 원본 언어 (ja, ko) |
| posted_at | DATE | X | 공고 게시일 |
| expires_at | DATE | X | 공고 만료일 |
| status | VARCHAR(20) | O | 상태 |
| created_at | TIMESTAMP | O | 수집일 |
| updated_at | TIMESTAMP | O | 수정일 |

### Enum: EmploymentType

```kotlin
enum class EmploymentType {
    FULL_TIME,     // 正社員
    CONTRACT,      // 契約社員
    PART_TIME,     // パートタイム
    INTERN,        // インターン
    FREELANCE      // フリーランス
}
```

### Enum: PostingStatus

```kotlin
enum class PostingStatus {
    ACTIVE,        // 활성
    EXPIRED,       // 만료
    DELETED        // 삭제됨
}
```

---

## 3. news_articles (뉴스 기사)

일본 IT 업계 관련 뉴스 기사입니다.

### 스키마

```sql
CREATE TABLE news_articles (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT NOT NULL REFERENCES crawl_sources(id) ON DELETE RESTRICT,
    external_id VARCHAR(200) NOT NULL,
    title VARCHAR(500) NOT NULL,
    title_translated VARCHAR(500),
    summary TEXT,
    summary_translated TEXT,
    content TEXT,
    content_translated TEXT,
    author VARCHAR(100),
    category VARCHAR(50),
    original_url VARCHAR(1000) NOT NULL,
    image_url VARCHAR(1000),
    language VARCHAR(10) DEFAULT 'ja',
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_news_articles_source_external UNIQUE (source_id, external_id)
);

CREATE INDEX idx_news_articles_published ON news_articles(published_at DESC);
CREATE INDEX idx_news_articles_created ON news_articles(created_at DESC);
CREATE INDEX idx_news_articles_category ON news_articles(category);
```

### 필드 설명

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| id | BIGSERIAL | O | PK |
| source_id | BIGINT | O | FK → crawl_sources |
| external_id | VARCHAR(200) | O | 원본 기사 고유 ID |
| title | VARCHAR(500) | O | 기사 제목 (원문) |
| title_translated | VARCHAR(500) | X | 기사 제목 (번역) |
| summary | TEXT | X | 기사 요약 (원문) |
| summary_translated | TEXT | X | 기사 요약 (번역) |
| content | TEXT | X | 기사 본문 (원문) |
| content_translated | TEXT | X | 기사 본문 (번역) |
| author | VARCHAR(100) | X | 작성자 |
| category | VARCHAR(50) | X | 카테고리 |
| original_url | VARCHAR(1000) | O | 원본 링크 |
| image_url | VARCHAR(1000) | X | 썸네일 이미지 |
| language | VARCHAR(10) | O | 원본 언어 |
| published_at | TIMESTAMP | X | 기사 발행일 |
| created_at | TIMESTAMP | O | 수집일 |
| updated_at | TIMESTAMP | O | 수정일 |

---

## 4. community_posts (커뮤니티 글)

커뮤니티에서 수집한 글/게시물입니다.

### 스키마

```sql
CREATE TABLE community_posts (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT NOT NULL REFERENCES crawl_sources(id) ON DELETE RESTRICT,
    external_id VARCHAR(200) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    title VARCHAR(500),
    title_translated VARCHAR(500),
    content TEXT NOT NULL,
    content_translated TEXT,
    author VARCHAR(100),
    author_profile_url VARCHAR(500),
    original_url VARCHAR(1000) NOT NULL,
    tags JSONB,
    like_count INT NOT NULL DEFAULT 0,
    comment_count INT NOT NULL DEFAULT 0,
    share_count INT NOT NULL DEFAULT 0,
    sentiment VARCHAR(20),
    language VARCHAR(10) DEFAULT 'ja',
    published_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_community_posts_source_external UNIQUE (source_id, external_id)
);

CREATE INDEX idx_community_posts_platform ON community_posts(platform);
CREATE INDEX idx_community_posts_published ON community_posts(published_at DESC);
CREATE INDEX idx_community_posts_created ON community_posts(created_at DESC);
CREATE INDEX idx_community_posts_sentiment ON community_posts(sentiment);
CREATE INDEX idx_community_posts_like ON community_posts(like_count DESC);
```

### 필드 설명

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| id | BIGSERIAL | O | PK |
| source_id | BIGINT | O | FK → crawl_sources |
| external_id | VARCHAR(200) | O | 원본 글 고유 ID |
| platform | VARCHAR(20) | O | 플랫폼 (REDDIT, QIITA 등) |
| title | VARCHAR(500) | X | 글 제목 (원문) |
| title_translated | VARCHAR(500) | X | 글 제목 (번역) |
| content | TEXT | O | 글 본문 (원문) |
| content_translated | TEXT | X | 글 본문 (번역) |
| author | VARCHAR(100) | X | 작성자명 |
| author_profile_url | VARCHAR(500) | X | 작성자 프로필 URL |
| original_url | VARCHAR(1000) | O | 원본 링크 |
| tags | JSONB | X | 태그/플레어 |
| like_count | INT | O | 좋아요/업보트 수 |
| comment_count | INT | O | 댓글 수 |
| share_count | INT | O | 공유/리트윗 수 |
| sentiment | VARCHAR(20) | X | 감정 분석 결과 |
| language | VARCHAR(10) | O | 원본 언어 |
| published_at | TIMESTAMP | O | 게시 시각 |
| created_at | TIMESTAMP | O | 수집일 |
| updated_at | TIMESTAMP | O | 수정일 |

### Enum: Sentiment

```kotlin
enum class Sentiment {
    POSITIVE,      // 긍정적
    NEUTRAL,       // 중립
    NEGATIVE       // 부정적
}
```

---

## 5. crawl_histories (크롤링 이력)

크롤링 실행 이력을 기록합니다.

### 스키마

```sql
CREATE TABLE crawl_histories (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT NOT NULL REFERENCES crawl_sources(id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL,
    items_found INT DEFAULT 0,
    items_saved INT DEFAULT 0,
    items_updated INT DEFAULT 0,
    items_translated INT DEFAULT 0,
    error_message TEXT,
    duration_ms BIGINT,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_crawl_histories_source ON crawl_histories(source_id);
CREATE INDEX idx_crawl_histories_started ON crawl_histories(started_at DESC);
CREATE INDEX idx_crawl_histories_status ON crawl_histories(status);
```

### 필드 설명

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| id | BIGSERIAL | O | PK |
| source_id | BIGINT | O | FK → crawl_sources |
| status | VARCHAR(20) | O | 크롤링 상태 |
| items_found | INT | O | 발견된 항목 수 |
| items_saved | INT | O | 저장된 항목 수 (신규) |
| items_updated | INT | O | 업데이트된 항목 수 |
| items_translated | INT | O | 번역된 항목 수 |
| error_message | TEXT | X | 에러 메시지 |
| duration_ms | BIGINT | X | 소요 시간 (밀리초) |
| started_at | TIMESTAMP | O | 시작 시각 |
| finished_at | TIMESTAMP | X | 종료 시각 |
| created_at | TIMESTAMP | O | 생성일 |

### Enum: CrawlStatus

```kotlin
enum class CrawlStatus {
    RUNNING,       // 실행 중
    SUCCESS,       // 성공
    FAILED,        // 실패
    PARTIAL        // 부분 성공
}
```

---

## 6. daily_summaries (일간 요약)

매일 생성되는 요약 정보입니다.

### 스키마

```sql
CREATE TABLE daily_summaries (
    id BIGSERIAL PRIMARY KEY,
    summary_date DATE NOT NULL UNIQUE,
    job_posting_count INT DEFAULT 0,
    news_article_count INT DEFAULT 0,
    community_post_count INT DEFAULT 0,
    summary_content TEXT NOT NULL,
    trending_topics JSONB,
    key_highlights JSONB,
    sent_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_daily_summaries_date ON daily_summaries(summary_date DESC);
CREATE INDEX idx_daily_summaries_status ON daily_summaries(status);
```

### 필드 설명

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| id | BIGSERIAL | O | PK |
| summary_date | DATE | O | 요약 대상 날짜 (UK) |
| job_posting_count | INT | O | 수집된 채용공고 수 |
| news_article_count | INT | O | 수집된 뉴스 수 |
| community_post_count | INT | O | 수집된 커뮤니티 글 수 |
| summary_content | TEXT | O | LLM 생성 요약 본문 |
| trending_topics | JSONB | X | 트렌딩 토픽 |
| key_highlights | JSONB | X | 주요 하이라이트 |
| sent_at | TIMESTAMP | X | 텔레그램 발송 시각 |
| status | VARCHAR(20) | O | 상태 |
| created_at | TIMESTAMP | O | 생성일 |
| updated_at | TIMESTAMP | O | 수정일 |

### Enum: SummaryStatus

```kotlin
enum class SummaryStatus {
    DRAFT,         // 생성됨 (미발송)
    SENT,          // 발송 완료
    FAILED         // 발송 실패
}
```

---

## ERD (Entity Relationship Diagram)

```
                        ┌──────────────────┐
                        │   crawl_sources  │
                        ├──────────────────┤
                        │ PK id            │
                        │    name          │
                        │    url           │
                        │    source_type   │
                        │    platform      │
                        │    enabled       │
                        └────────┬─────────┘
                                 │
           ┌─────────────────────┼─────────────────────┐
           │                     │                     │
           ▼                     ▼                     ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│   job_postings   │  │  news_articles   │  │ community_posts  │
├──────────────────┤  ├──────────────────┤  ├──────────────────┤
│ PK id            │  │ PK id            │  │ PK id            │
│ FK source_id     │  │ FK source_id     │  │ FK source_id     │
│    external_id   │  │    external_id   │  │    external_id   │
│    title         │  │    title         │  │    platform      │
│    title_trans   │  │    title_trans   │  │    title         │
│    company_name  │  │    summary       │  │    title_trans   │
│    description   │  │    summary_trans │  │    content       │
│    desc_trans    │  │    content       │  │    content_trans │
│    language      │  │    content_trans │  │    sentiment     │
│    status        │  │    language      │  │    language      │
└──────────────────┘  └──────────────────┘  │    like_count    │
                                            └──────────────────┘
           │
           ▼
┌──────────────────┐                       ┌──────────────────┐
│ crawl_histories  │                       │ daily_summaries  │
├──────────────────┤                       ├──────────────────┤
│ PK id            │                       │ PK id            │
│ FK source_id     │                       │ UK summary_date  │
│    status        │                       │    job_count     │
│    items_found   │                       │    news_count    │
│    items_saved   │                       │    community_cnt │
│    items_trans   │                       │    summary       │
│    duration_ms   │                       │    sent_at       │
└──────────────────┘                       └──────────────────┘
```

---

## 인덱스 전략

### Primary Key
- 모든 테이블에 `id BIGSERIAL PRIMARY KEY`

### Unique Constraints
- `job_postings`: (source_id, external_id)
- `news_articles`: (source_id, external_id)
- `community_posts`: (source_id, external_id)
- `daily_summaries`: (summary_date)

### 조회 성능 인덱스
| 테이블 | 인덱스 | 용도 |
|--------|--------|------|
| job_postings | (status) | 활성 공고 필터 |
| job_postings | (created_at DESC) | 최신순 정렬 |
| job_postings | (posted_at DESC) | 게시일 정렬 |
| news_articles | (published_at DESC) | 최신 뉴스 |
| community_posts | (platform) | 플랫폼별 필터 |
| community_posts | (sentiment) | 감정별 필터 |
| community_posts | (like_count DESC) | 인기순 정렬 |
| crawl_histories | (source_id, started_at DESC) | 소스별 이력 |

---

## 번역 정책

### 번역 대상 필드
| 테이블 | 원문 필드 | 번역 필드 |
|--------|-----------|-----------|
| job_postings | title | title_translated |
| job_postings | description | description_translated |
| job_postings | requirements | requirements_translated |
| news_articles | title | title_translated |
| news_articles | summary | summary_translated |
| news_articles | content | content_translated |
| community_posts | title | title_translated |
| community_posts | content | content_translated |

### 번역 프로세스
1. `language = 'ja'`인 데이터만 번역 대상
2. LLM API (Gemini/OpenAI)를 통한 번역
3. 번역 완료 시 `*_translated` 필드에 저장
4. `crawl_histories.items_translated`에 번역 건수 기록

---

## 감정 분석 정책

### 분석 대상
- `community_posts` 테이블의 `content` 필드

### 분석 방법
- LLM API를 통한 감정 분석
- POSITIVE, NEUTRAL, NEGATIVE 3단계 분류

### 분석 시점
- 크롤링 후 배치 처리
- 또는 일간 요약 생성 시 분석

---

## 초기 데이터

### crawl_sources 초기 데이터

```sql
INSERT INTO crawl_sources (name, url, source_type, platform, enabled) VALUES
-- 커뮤니티 (1순위)
('Reddit r/japanlife', 'https://www.reddit.com/r/japanlife', 'COMMUNITY', 'REDDIT', true),
('Reddit r/movingtojapan', 'https://www.reddit.com/r/movingtojapan', 'COMMUNITY', 'REDDIT', true),
('Reddit r/japandev', 'https://www.reddit.com/r/japandev', 'COMMUNITY', 'REDDIT', true),

-- 채용 (2순위)
('Indeed Japan', 'https://jp.indeed.com', 'JOB_SITE', NULL, true),

-- 블로그/커뮤니티 (3순위)
('Qiita', 'https://qiita.com', 'COMMUNITY', 'QIITA', true);
```

---

## 버전 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|-----------|
| 1.0 | 2024-XX-XX | 초기 설계 |
| 2.0 | 2026-02-22 | FK ON DELETE RESTRICT 적용, share_count NOT NULL, config 코멘트 보안 개선 |
