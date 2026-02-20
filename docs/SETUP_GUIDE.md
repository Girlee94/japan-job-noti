# Ready Japan 설정 가이드

## 1. Supabase PostgreSQL 설정

### 1.1 Docker로 Supabase 실행

```bash
# Supabase 디렉토리로 이동
cd /path/to/supabase

# Docker Compose 실행
docker-compose up -d
```

### 1.2 데이터베이스 스키마 생성

Supabase Studio (http://localhost:8000) 또는 psql로 DDL 실행:

```bash
# psql로 접속
psql -h localhost -p 5432 -U postgres -d postgres

# DDL 실행
\i docs/sql/V1__init_schema.sql
```

또는 Supabase Studio에서:
1. http://localhost:8000 접속
2. SQL Editor 탭 클릭
3. `docs/sql/V1__init_schema.sql` 내용 붙여넣기
4. Run 버튼 클릭

### 1.3 연결 정보

| 연결 방식 | Host | Port | Database | Username |
|----------|------|------|----------|----------|
| Direct | localhost | 5432 | postgres | postgres |
| Transaction Pooler | localhost | 6543 | postgres | postgres.{tenant-id} |

**권장**: 로컬 개발 시 Direct 연결(5432), 프로덕션에서 Pooler 연결(6543)

---

## 2. Telegram Bot 설정

### 2.1 Bot 생성

1. Telegram에서 @BotFather 검색
2. `/newbot` 명령어 입력
3. Bot 이름 입력 (예: Ready Japan Bot)
4. Bot username 입력 (예: ready_japan_bot)
5. **Bot Token** 저장 (예: `123456789:ABCdefGHIjklMNOpqrsTUVwxyz`)

### 2.2 Chat ID 확인

1. 생성한 봇에게 아무 메시지 전송
2. @userinfobot 또는 @getidsbot에게 메시지 전송
3. 표시되는 **Chat ID** 저장 (예: `123456789`)

### 2.3 환경 변수 설정

```bash
TELEGRAM_BOT_TOKEN=123456789:ABCdefGHIjklMNOpqrsTUVwxyz
TELEGRAM_CHAT_ID=123456789
```

---

## 3. Reddit API 설정

### 3.1 Reddit App 생성

1. https://www.reddit.com/prefs/apps 접속
2. "Create App" 또는 "Create Another App" 클릭
3. 다음 정보 입력:
   - Name: Ready Japan
   - App type: **script** 선택
   - Redirect URI: http://localhost:8080 (사용하지 않지만 필수)
4. **Client ID** (앱 이름 아래 짧은 문자열)
5. **Client Secret** (secret 라벨 옆)

### 3.2 환경 변수 설정

```bash
REDDIT_CLIENT_ID=your_client_id
REDDIT_CLIENT_SECRET=your_client_secret
```

---

## 4. LLM API 설정

### 4.1 OpenAI API (권장)

1. https://platform.openai.com 접속
2. API Keys 메뉴에서 새 키 생성
3. 환경 변수 설정:

```bash
LLM_PROVIDER=openai
OPENAI_API_KEY=sk-...
LLM_MODEL=gpt-4o-mini
```

### 4.2 모델 선택 가이드

| 모델 | 용도 | 비용 |
|------|------|------|
| gpt-4o-mini | 번역, 감정분석 (권장) | 저렴 |
| gpt-4o | 복잡한 요약 | 중간 |
| gpt-4-turbo | 최고 품질 | 높음 |

---

## 5. 애플리케이션 실행

### 5.1 환경 변수 파일 생성

```bash
# 템플릿 복사
cp .env.example .env.local

# 값 수정
vi .env.local
```

### 5.2 빌드 및 실행

```bash
# 빌드
./gradlew clean build

# Batch 모듈 실행 (스케줄러)
./gradlew :module-batch:bootRun

# API 모듈 실행 (REST API)
./gradlew :module-api:bootRun
```

### 5.3 환경 변수 로드 (선택)

IntelliJ에서 EnvFile 플러그인 사용 또는:

```bash
# 환경 변수 로드 후 실행
export $(cat .env.local | xargs) && ./gradlew :module-batch:bootRun
```

---

## 6. 확인

### 6.1 데이터베이스 연결 확인

```bash
# API 서버 Health Check
curl http://localhost:8080/api/health

# 응답 예시
{
  "success": true,
  "data": {
    "status": "UP",
    "timestamp": "2024-01-01T09:00:00"
  }
}
```

### 6.2 Swagger UI

http://localhost:8080/swagger-ui.html

### 6.3 Supabase Studio

http://localhost:8000

---

## 7. 문제 해결

### 데이터베이스 연결 실패

```
Connection refused
```

→ Docker Compose가 실행 중인지 확인:
```bash
docker-compose ps
```

### Hibernate 스키마 오류

```
Schema-validation: missing table
```

→ DDL 스크립트 실행 확인:
```bash
psql -h localhost -p 5432 -U postgres -d postgres -c "\dt"
```

### Telegram 메시지 전송 실패

→ Bot Token과 Chat ID 확인:
```bash
curl "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/getMe"
```
