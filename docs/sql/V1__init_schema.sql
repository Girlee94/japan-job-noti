-- ============================================
-- Ready Japan Database Schema
-- Version: 1.0
-- Database: Supabase PostgreSQL
-- ============================================

-- Enable UUID extension (Supabase default)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- 1. crawl_sources (크롤링 소스)
-- ============================================
CREATE TABLE IF NOT EXISTS crawl_sources (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    url VARCHAR(500) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    platform VARCHAR(20),
    cron_expression VARCHAR(50) DEFAULT '0 0 8 * * *',
    enabled BOOLEAN DEFAULT true NOT NULL,
    config JSONB,
    last_crawled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE crawl_sources IS '크롤링 소스 관리 테이블';
COMMENT ON COLUMN crawl_sources.source_type IS 'JOB_SITE, NEWS_SITE, COMMUNITY, API';
COMMENT ON COLUMN crawl_sources.platform IS 'REDDIT, TWITTER, QIITA, ZENN, NOTE, DISCORD, FIVECH, OTHER';
COMMENT ON COLUMN crawl_sources.config IS '추가 설정 (API 키, 필터 조건 등)';

CREATE INDEX idx_crawl_sources_enabled ON crawl_sources(enabled);
CREATE INDEX idx_crawl_sources_type ON crawl_sources(source_type);

-- ============================================
-- 2. job_postings (채용 공고)
-- ============================================
CREATE TABLE IF NOT EXISTS job_postings (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT NOT NULL REFERENCES crawl_sources(id) ON DELETE CASCADE,
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
    language VARCHAR(10) DEFAULT 'ja' NOT NULL,
    posted_at DATE,
    expires_at DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_job_postings_source_external UNIQUE (source_id, external_id)
);

COMMENT ON TABLE job_postings IS '채용 공고 테이블';
COMMENT ON COLUMN job_postings.employment_type IS 'FULL_TIME, CONTRACT, PART_TIME, INTERN, FREELANCE';
COMMENT ON COLUMN job_postings.status IS 'ACTIVE, EXPIRED, DELETED';
COMMENT ON COLUMN job_postings.language IS 'ja (일본어), ko (한국어)';

CREATE INDEX idx_job_postings_status ON job_postings(status);
CREATE INDEX idx_job_postings_created ON job_postings(created_at DESC);
CREATE INDEX idx_job_postings_posted ON job_postings(posted_at DESC);
CREATE INDEX idx_job_postings_company ON job_postings(company_name);
CREATE INDEX idx_job_postings_language ON job_postings(language);

-- ============================================
-- 3. news_articles (뉴스 기사)
-- ============================================
CREATE TABLE IF NOT EXISTS news_articles (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT NOT NULL REFERENCES crawl_sources(id) ON DELETE CASCADE,
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
    language VARCHAR(10) DEFAULT 'ja' NOT NULL,
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_news_articles_source_external UNIQUE (source_id, external_id)
);

COMMENT ON TABLE news_articles IS '뉴스 기사 테이블';

CREATE INDEX idx_news_articles_published ON news_articles(published_at DESC);
CREATE INDEX idx_news_articles_created ON news_articles(created_at DESC);
CREATE INDEX idx_news_articles_category ON news_articles(category);
CREATE INDEX idx_news_articles_language ON news_articles(language);

-- ============================================
-- 4. community_posts (커뮤니티 글)
-- ============================================
CREATE TABLE IF NOT EXISTS community_posts (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT NOT NULL REFERENCES crawl_sources(id) ON DELETE CASCADE,
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
    like_count INT DEFAULT 0 NOT NULL,
    comment_count INT DEFAULT 0 NOT NULL,
    share_count INT DEFAULT 0,
    sentiment VARCHAR(20),
    language VARCHAR(10) DEFAULT 'ja' NOT NULL,
    published_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_community_posts_source_external UNIQUE (source_id, external_id)
);

COMMENT ON TABLE community_posts IS '커뮤니티 글 테이블';
COMMENT ON COLUMN community_posts.platform IS 'REDDIT, TWITTER, QIITA, ZENN, NOTE, DISCORD, FIVECH, OTHER';
COMMENT ON COLUMN community_posts.sentiment IS 'POSITIVE, NEUTRAL, NEGATIVE';
COMMENT ON COLUMN community_posts.tags IS 'JSON 배열 형식의 태그 목록';

CREATE INDEX idx_community_posts_platform ON community_posts(platform);
CREATE INDEX idx_community_posts_published ON community_posts(published_at DESC);
CREATE INDEX idx_community_posts_created ON community_posts(created_at DESC);
CREATE INDEX idx_community_posts_sentiment ON community_posts(sentiment);
CREATE INDEX idx_community_posts_like ON community_posts(like_count DESC);
CREATE INDEX idx_community_posts_language ON community_posts(language);

-- ============================================
-- 5. crawl_histories (크롤링 이력)
-- ============================================
CREATE TABLE IF NOT EXISTS crawl_histories (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT NOT NULL REFERENCES crawl_sources(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    items_found INT DEFAULT 0 NOT NULL,
    items_saved INT DEFAULT 0 NOT NULL,
    items_updated INT DEFAULT 0 NOT NULL,
    items_translated INT DEFAULT 0 NOT NULL,
    error_message TEXT,
    duration_ms BIGINT,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE crawl_histories IS '크롤링 이력 테이블';
COMMENT ON COLUMN crawl_histories.status IS 'RUNNING, SUCCESS, FAILED, PARTIAL';

CREATE INDEX idx_crawl_histories_source ON crawl_histories(source_id);
CREATE INDEX idx_crawl_histories_started ON crawl_histories(started_at DESC);
CREATE INDEX idx_crawl_histories_status ON crawl_histories(status);

-- ============================================
-- 6. daily_summaries (일간 요약)
-- ============================================
CREATE TABLE IF NOT EXISTS daily_summaries (
    id BIGSERIAL PRIMARY KEY,
    summary_date DATE NOT NULL UNIQUE,
    job_posting_count INT DEFAULT 0 NOT NULL,
    news_article_count INT DEFAULT 0 NOT NULL,
    community_post_count INT DEFAULT 0 NOT NULL,
    summary_content TEXT NOT NULL,
    trending_topics JSONB,
    key_highlights JSONB,
    sent_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'DRAFT' NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE daily_summaries IS '일간 요약 테이블';
COMMENT ON COLUMN daily_summaries.status IS 'DRAFT, SENT, FAILED';
COMMENT ON COLUMN daily_summaries.trending_topics IS 'JSON 형식의 트렌딩 토픽';
COMMENT ON COLUMN daily_summaries.key_highlights IS 'JSON 형식의 주요 하이라이트';

CREATE INDEX idx_daily_summaries_date ON daily_summaries(summary_date DESC);
CREATE INDEX idx_daily_summaries_status ON daily_summaries(status);

-- ============================================
-- 자동 updated_at 트리거 함수
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 각 테이블에 트리거 적용
CREATE TRIGGER update_crawl_sources_updated_at
    BEFORE UPDATE ON crawl_sources
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_job_postings_updated_at
    BEFORE UPDATE ON job_postings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_news_articles_updated_at
    BEFORE UPDATE ON news_articles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_community_posts_updated_at
    BEFORE UPDATE ON community_posts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_daily_summaries_updated_at
    BEFORE UPDATE ON daily_summaries
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 초기 데이터: 크롤링 소스
-- ============================================
INSERT INTO crawl_sources (name, url, source_type, platform, enabled, config) VALUES
-- Reddit (1순위)
('Reddit r/japanlife', 'https://www.reddit.com/r/japanlife', 'COMMUNITY', 'REDDIT', true,
 '{"subreddit": "japanlife", "sort": "new", "limit": 50}'),
('Reddit r/movingtojapan', 'https://www.reddit.com/r/movingtojapan', 'COMMUNITY', 'REDDIT', true,
 '{"subreddit": "movingtojapan", "sort": "new", "limit": 50}'),
('Reddit r/japandev', 'https://www.reddit.com/r/japandev', 'COMMUNITY', 'REDDIT', true,
 '{"subreddit": "japandev", "sort": "new", "limit": 50}'),

-- Indeed Japan (2순위)
('Indeed Japan IT', 'https://jp.indeed.com', 'JOB_SITE', NULL, true,
 '{"keywords": ["IT", "エンジニア", "developer"], "location": "東京都"}'),

-- Qiita (3순위)
('Qiita 日本就職', 'https://qiita.com', 'COMMUNITY', 'QIITA', true,
 '{"tags": ["日本", "就職", "転職", "キャリア"], "limit": 30}');

-- ============================================
-- Row Level Security (RLS) - Optional
-- Supabase 환경에서 필요시 활성화
-- ============================================
-- ALTER TABLE crawl_sources ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE job_postings ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE news_articles ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE community_posts ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE crawl_histories ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE daily_summaries ENABLE ROW LEVEL SECURITY;

-- ============================================
-- 통계 뷰 (Optional)
-- ============================================
CREATE OR REPLACE VIEW v_daily_stats AS
SELECT
    DATE(created_at) as date,
    COUNT(*) FILTER (WHERE source_id IN (SELECT id FROM crawl_sources WHERE source_type = 'JOB_SITE')) as job_count,
    COUNT(*) FILTER (WHERE source_id IN (SELECT id FROM crawl_sources WHERE source_type = 'NEWS_SITE')) as news_count,
    COUNT(*) FILTER (WHERE source_id IN (SELECT id FROM crawl_sources WHERE source_type = 'COMMUNITY')) as community_count
FROM (
    SELECT id, source_id, created_at FROM job_postings
    UNION ALL
    SELECT id, source_id, created_at FROM news_articles
    UNION ALL
    SELECT id, source_id, created_at FROM community_posts
) combined
GROUP BY DATE(created_at)
ORDER BY date DESC;

COMMENT ON VIEW v_daily_stats IS '일별 수집 통계 뷰';
