# Ready Japan 코드 컨벤션 가이드라인

> 이 문서는 프로젝트의 일관된 코드 스타일과 아키텍처 규칙을 정의합니다.
> 모든 기여자와 AI 어시스턴트는 이 가이드라인을 엄격히 준수해야 합니다.

## 1. 프로젝트 아키텍처

### 1.1 멀티 모듈 구조

```
ready-japan/
├── module-core/           # 순수 도메인 로직, 엔티티, 공통 유틸리티
├── module-infrastructure/ # 외부 시스템 연동 (DB, API, 크롤러)
├── module-batch/          # 스케줄링, 배치 작업
└── module-api/            # REST API (추후 확장용)
```

### 1.2 모듈 의존성 규칙

```
module-api ─────┐
                ├──▶ module-infrastructure ──▶ module-core
module-batch ───┘
```

- `module-core`: 어떤 모듈에도 의존하지 않음 (순수 도메인)
- `module-infrastructure`: `module-core`에만 의존
- `module-batch`, `module-api`: `module-core`, `module-infrastructure`에 의존

### 1.3 레이어별 책임

| 레이어 | 책임 | 위치 |
|--------|------|------|
| Domain | 엔티티, 값 객체, 도메인 서비스 | `module-core` |
| Application | 유스케이스, 트랜잭션 경계 | `module-batch`, `module-api` |
| Infrastructure | DB, 외부 API, 크롤러 구현 | `module-infrastructure` |
| Presentation | Controller, DTO | `module-api` |

---

## 2. 네이밍 규칙

### 2.1 패키지

| 규칙 | 예시 |
|------|------|
| lowercase 사용 | `com.readyjapan.core.domain` |
| 단수형 사용 | `entity` (~~entities~~) |
| 기능 기반 분류 | `crawler`, `telegram`, `llm` |

### 2.2 클래스/인터페이스

| 대상 | 규칙 | 예시 |
|------|------|------|
| 클래스 | PascalCase | `JobPostingService` |
| 인터페이스 | 접두사 없음 | `JobPostingRepository` |
| 추상 클래스 | `Abstract` 접두사 | `AbstractCrawler` |
| 예외 클래스 | `Exception` 접미사 | `EntityNotFoundException` |
| 설정 클래스 | `Config` 접미사 | `JpaConfig` |

### 2.3 함수

| 규칙 | 예시 |
|------|------|
| camelCase 사용 | `fetchLatestPostings()` |
| 동사로 시작 | `create`, `find`, `update`, `delete` |
| Boolean 반환 | `is`, `has`, `can` 접두사 |
| 컬렉션 반환 | 복수형 사용 | `findAllPostings()` |

```kotlin
// ✅ Good
fun findById(id: Long): JobPosting?
fun findAllByStatus(status: Status): List<JobPosting>
fun isExpired(): Boolean
fun hasApplied(userId: Long): Boolean

// ❌ Bad
fun getJobPosting(id: Long): JobPosting?  // get 대신 find 사용
fun fetchData(): List<Any>                 // 모호한 이름
```

### 2.4 변수/상수

| 대상 | 규칙 | 예시 |
|------|------|------|
| 변수 | camelCase | `jobTitle`, `companyName` |
| 상수 | SCREAMING_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Companion object 상수 | 동일 | `DEFAULT_PAGE_SIZE` |

---

## 3. 클래스 설계

### 3.1 Entity

```kotlin
@Entity
@Table(name = "job_postings")
class JobPosting(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var title: String,

    @Column(nullable = false)
    var companyName: String,

    @Enumerated(EnumType.STRING)
    var status: PostingStatus = PostingStatus.ACTIVE,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    // 비즈니스 로직은 엔티티 내부에 캡슐화
    fun expire() {
        this.status = PostingStatus.EXPIRED
        this.updatedAt = LocalDateTime.now()
    }
}
```

**규칙:**
- `id`는 `val`로 선언, 기본값 0
- 불변 필드는 `val`, 가변 필드는 `var`
- `createdAt`은 `updatable = false`
- 비즈니스 로직은 엔티티 메서드로 캡슐화

### 3.2 DTO

```kotlin
// Request DTO
data class CreateJobPostingRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    val title: String,

    @field:NotBlank(message = "회사명은 필수입니다")
    val companyName: String,

    val description: String? = null
)

// Response DTO
data class JobPostingResponse(
    val id: Long,
    val title: String,
    val companyName: String,
    val status: String,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(entity: JobPosting): JobPostingResponse {
            return JobPostingResponse(
                id = entity.id,
                title = entity.title,
                companyName = entity.companyName,
                status = entity.status.name,
                createdAt = entity.createdAt
            )
        }
    }
}
```

**규칙:**
- Request: `Create*Request`, `Update*Request`
- Response: `*Response`
- `data class` 사용
- Entity → DTO 변환은 `companion object.from()` 패턴

### 3.3 Repository

```kotlin
// Core 모듈: 인터페이스만 정의
interface JobPostingRepository {
    fun findById(id: Long): JobPosting?
    fun findAllByStatus(status: PostingStatus): List<JobPosting>
    fun save(jobPosting: JobPosting): JobPosting
    fun deleteById(id: Long)
}

// Infrastructure 모듈: JPA 구현
interface JpaJobPostingRepository : JpaRepository<JobPosting, Long>, JobPostingRepository {
    override fun findAllByStatus(status: PostingStatus): List<JobPosting>
}
```

### 3.4 Service

```kotlin
@Service
@Transactional(readOnly = true)
class JobPostingService(
    private val jobPostingRepository: JobPostingRepository
) {
    fun findById(id: Long): JobPosting {
        return jobPostingRepository.findById(id)
            ?: throw EntityNotFoundException("JobPosting", id)
    }

    @Transactional
    fun create(request: CreateJobPostingRequest): JobPosting {
        val jobPosting = JobPosting(
            title = request.title,
            companyName = request.companyName
        )
        return jobPostingRepository.save(jobPosting)
    }
}
```

**규칙:**
- 클래스 레벨 `@Transactional(readOnly = true)`
- 쓰기 작업은 메서드 레벨 `@Transactional`
- 생성자 주입 (필드 주입 금지)

---

## 4. 의존성 주입

### 4.1 생성자 주입 (필수)

```kotlin
// ✅ Good: 생성자 주입
@Service
class JobPostingService(
    private val repository: JobPostingRepository,
    private val telegramClient: TelegramClient
)

// ❌ Bad: 필드 주입 금지
@Service
class JobPostingService {
    @Autowired
    lateinit var repository: JobPostingRepository
}
```

### 4.2 선택적 의존성

```kotlin
@Service
class NotificationService(
    private val telegramClient: TelegramClient,
    private val slackClient: SlackClient? = null  // 선택적 의존성
)
```

---

## 5. 예외 처리

### 5.1 예외 계층 구조

```kotlin
// module-core/common/exception/

// 기본 비즈니스 예외
sealed class BusinessException(
    val errorCode: String,
    override val message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

// 구체적인 예외들
class EntityNotFoundException(
    entityName: String,
    identifier: Any
) : BusinessException(
    errorCode = "ENTITY_NOT_FOUND",
    message = "$entityName not found with identifier: $identifier"
)

class ExternalApiException(
    apiName: String,
    cause: Throwable? = null
) : BusinessException(
    errorCode = "EXTERNAL_API_ERROR",
    message = "External API call failed: $apiName",
    cause = cause
)

class CrawlingException(
    source: String,
    reason: String
) : BusinessException(
    errorCode = "CRAWLING_ERROR",
    message = "Crawling failed for $source: $reason"
)
```

### 5.2 GlobalExceptionHandler

```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = KotlinLogging.logger {}

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ErrorResponse> {
        logger.warn { "Business exception: ${e.message}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of(e))
    }

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFound(e: EntityNotFoundException): ResponseEntity<ErrorResponse> {
        logger.warn { "Entity not found: ${e.message}" }
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse.of(e))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error(e) { "Unexpected error occurred" }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.of("INTERNAL_ERROR", "서버 오류가 발생했습니다"))
    }
}
```

---

## 6. 응답 포맷

### 6.1 성공 응답

```kotlin
data class ApiResponse<T>(
    val success: Boolean = true,
    val data: T? = null,
    val message: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun <T> success(data: T, message: String? = null): ApiResponse<T> {
            return ApiResponse(success = true, data = data, message = message)
        }

        fun success(message: String): ApiResponse<Unit> {
            return ApiResponse(success = true, message = message)
        }
    }
}
```

### 6.2 에러 응답

```kotlin
data class ErrorResponse(
    val success: Boolean = false,
    val errorCode: String,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val details: Map<String, Any>? = null
) {
    companion object {
        fun of(exception: BusinessException): ErrorResponse {
            return ErrorResponse(
                errorCode = exception.errorCode,
                message = exception.message
            )
        }

        fun of(errorCode: String, message: String): ErrorResponse {
            return ErrorResponse(errorCode = errorCode, message = message)
        }
    }
}
```

---

## 7. Kotlin 스타일 규칙

### 7.1 Null 안전성

```kotlin
// ✅ Elvis 연산자
val name = company?.name ?: "Unknown"

// ✅ Safe call chain
val city = user?.address?.city

// ✅ let 활용
user?.let {
    sendNotification(it)
}

// ❌ !! 사용 금지 (테스트 코드 제외)
val name = user!!.name
```

### 7.2 Scope 함수 활용

```kotlin
// apply: 객체 초기화
val jobPosting = JobPosting().apply {
    title = "Backend Developer"
    companyName = "Tech Corp"
}

// let: null 체크 + 변환
val response = entity?.let { JobPostingResponse.from(it) }

// also: 부가 작업 (로깅 등)
return repository.save(entity).also {
    logger.info { "Saved entity: ${it.id}" }
}

// run: 결과 계산
val isValid = jobPosting.run {
    title.isNotBlank() && companyName.isNotBlank()
}

// with: 같은 객체의 여러 메서드 호출
with(jobPosting) {
    println(title)
    println(companyName)
}
```

### 7.3 확장 함수

```kotlin
// 유틸리티성 확장 함수
fun String.toSlug(): String =
    this.lowercase()
        .replace(Regex("[^a-z0-9\\s-]"), "")
        .replace(Regex("\\s+"), "-")

fun LocalDateTime.toKoreanFormat(): String =
    this.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm"))
```

### 7.4 Collection 처리

```kotlin
// ✅ 함수형 스타일
val activePostings = postings
    .filter { it.status == PostingStatus.ACTIVE }
    .sortedByDescending { it.createdAt }
    .take(10)

// ✅ 시퀀스 (대용량 데이터)
val result = postings.asSequence()
    .filter { it.isActive() }
    .map { it.toResponse() }
    .toList()
```

---

## 8. 테스트 규칙

### 8.1 테스트 프레임워크

**Kotest BehaviorSpec** + **MockK**를 사용한다.

| 라이브러리 | 용도 |
|-----------|------|
| Kotest BehaviorSpec | Given/When/Then 구조의 테스트 DSL |
| MockK | Mock 객체 생성 (`mockk<T>()`) |
| Kotest Matchers | 검증 (`shouldBe`, `shouldHaveSize` 등) |

### 8.2 테스트 구조

```kotlin
class JobPostingServiceTest : BehaviorSpec({

    // Mock 선언: mockk<T>() 직접 생성
    val repository = mockk<JobPostingRepository>()
    val service = JobPostingService(repository)

    // 매 테스트 전 Mock 초기화
    beforeEach {
        clearMocks(repository)
    }

    // 테스트 헬퍼 함수
    fun createJob(id: Long = 1L): JobPosting = JobPosting(
        id = id,
        source = createSource(),
        externalId = "job$id",
        title = "Software Engineer $id",
        companyName = "Tech Corp",
        originalUrl = "https://example.com/job$id"
    )

    // Given(대상) → When(조건) → Then(기대결과)
    Given("findById") {
        When("존재하는 ID로 조회 시") {
            Then("JobPosting을 반환한다") {
                val expected = createJob(1L)
                every { repository.findById(1L) } returns expected

                val result = service.findById(1L)

                result.id shouldBe 1L
                result.title shouldBe "Software Engineer 1"
            }
        }
        When("존재하지 않는 ID로 조회 시") {
            Then("EntityNotFoundException이 발생한다") {
                every { repository.findById(999L) } returns null

                shouldThrow<EntityNotFoundException> {
                    service.findById(999L)
                }
            }
        }
    }
})
```

### 8.3 Mocking (MockK 사용)

```kotlin
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

// Mock 생성: mockk<T>() 직접 생성 방식
val repository = mockk<JobPostingRepository>()

// 생성자 주입으로 테스트 대상에 전달
val service = JobPostingService(repository)

// 매 테스트 전 초기화 (beforeEach 블록 내)
clearMocks(repository)

// Stubbing
every { repository.findById(1L) } returns expected
every { repository.findAllByStatus(any()) } returns listOf(job1, job2)
every { repository.save(any()) } answers { firstArg() }

// 호출 검증 (필요 시)
verify(exactly = 1) { repository.findById(1L) }
```

### 8.4 검증 (Kotest Matchers)

```kotlin
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.assertions.throwables.shouldThrow

// 값 검증
response.success shouldBe true
response.data shouldNotBe null
response.data!!.id shouldBe 1L

// 컬렉션 검증
response.data!! shouldHaveSize 2

// 예외 검증
shouldThrow<EntityNotFoundException> {
    service.findById(999L)
}
```

### 8.5 테스트 네이밍

Given/When/Then 블록의 이름은 **한글**로 작성하여 가독성을 높인다.

```kotlin
Given("getJobs") {
    When("기본 상태(ACTIVE)로 조회 시") {
        Then("활성 채용공고 목록을 반환한다") { ... }
    }
    When("존재하지 않는 ID로 조회 시") {
        Then("EntityNotFoundException이 발생한다") { ... }
    }
}
```

---

## 9. 로깅 규칙

### 9.1 Kotlin Logging 사용

```kotlin
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class JobPostingService {

    fun findById(id: Long): JobPosting {
        logger.debug { "Finding job posting by id: $id" }

        return repository.findById(id)?.also {
            logger.info { "Found job posting: ${it.title}" }
        } ?: run {
            logger.warn { "Job posting not found: $id" }
            throw EntityNotFoundException("JobPosting", id)
        }
    }
}
```

### 9.2 로그 레벨 가이드

| 레벨 | 용도 | 예시 |
|------|------|------|
| ERROR | 즉시 조치 필요 | 외부 API 장애, DB 연결 실패 |
| WARN | 잠재적 문제 | 재시도, 데이터 누락 |
| INFO | 주요 비즈니스 이벤트 | 크롤링 완료, 메시지 발송 |
| DEBUG | 개발/디버깅용 | 메서드 진입, 파라미터 값 |

---

## 10. 설정 관리

### 10.1 application.yml 구조

```yaml
spring:
  profiles:
    active: local

---
spring:
  config:
    activate:
      on-profile: local
  datasource:
    url: jdbc:postgresql://localhost:5432/readyjapan
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}

---
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: ${DATABASE_URL}
```

### 10.2 @ConfigurationProperties 활용

```kotlin
@ConfigurationProperties(prefix = "app.telegram")
data class TelegramProperties(
    val botToken: String,
    val chatId: String,
    val enabled: Boolean = true
)

@Configuration
@EnableConfigurationProperties(TelegramProperties::class)
class TelegramConfig
```

---

## 11. 크롤러/외부 API 연동 패턴

> PR 리뷰를 통해 축적된 실전 패턴입니다. 새로운 크롤러나 외부 API 연동 시 반드시 참고하세요.

### 11.1 트랜잭션 경계 분리

HTTP 호출이 포함된 서비스에는 `@Transactional`을 선언하지 않는다.
DB 저장 로직은 별도 `PersistenceService`로 분리하여 Spring AOP 프록시가 정상 동작하도록 한다.

```kotlin
// ❌ Bad: HTTP 호출 + @Transactional 혼합 (self-invocation 문제)
@Service
@Transactional(readOnly = true)
class CrawlerService {
    @Transactional
    fun saveCrawledItems() { ... } // AOP 프록시 무시됨
}

// ✅ Good: 서비스 분리
@Service
class CrawlerService(private val persistenceService: ItemPersistenceService) {
    fun crawl() {
        val items = httpCall()                          // 트랜잭션 밖
        persistenceService.saveCrawledItems(items)      // 별도 트랜잭션
    }
}

@Service
@Transactional(readOnly = true)
class ItemPersistenceService {
    @Transactional
    fun saveCrawledItems(items: List<Item>) { ... }
}
```

### 11.2 N+1 방지: 배치 조회

중복 체크 시 개별 `findBy` 대신 `findAllByXxxIn`으로 한 번에 조회한다.

```kotlin
// ❌ Bad: N+1 쿼리
for (item in items) {
    val existing = repository.findByExternalId(item.id) // N번 쿼리
}

// ✅ Good: 1회 배치 조회
val existingMap = repository
    .findAllBySourceIdAndExternalIdIn(sourceId, items.map { it.id })
    .associateBy { it.externalId }
```

**주의:** Adapter에서 빈 리스트 가드를 추가하여 불필요한 `IN ()` 쿼리를 방지한다.
```kotlin
override fun findAllBySourceIdAndExternalIdIn(sourceId: Long, externalIds: List<String>): List<CommunityPost> =
    if (externalIds.isEmpty()) emptyList() else jpa.findAllBySourceIdAndExternalIdIn(sourceId, externalIds)
```

### 11.3 saveAll() 배치 저장

루프 내 개별 `save()` 대신 리스트에 모아서 `saveAll()`로 일괄 저장한다.

```kotlin
// ❌ Bad: 개별 저장
for (item in items) {
    repository.save(item)
}

// ✅ Good: 배치 저장
val toInsert = mutableListOf<Entity>()
val toUpdate = mutableListOf<Entity>()
// ... 분류 로직 ...
if (toInsert.isNotEmpty()) repository.saveAll(toInsert)
if (toUpdate.isNotEmpty()) repository.saveAll(toUpdate)
```

### 11.4 DB-level 필터링

메모리 필터링 대신 Repository 쿼리에서 직접 필터링한다.

```kotlin
// ❌ Bad: 애플리케이션 레벨 필터
val sources = repository.findEnabledBySourceType(COMMUNITY)
    .filter { it.platform == QIITA }

// ✅ Good: DB-level 필터
val sources = repository.findEnabledBySourceTypeAndPlatform(COMMUNITY, QIITA)
```

### 11.5 Regex/ObjectMapper 등 비용 있는 객체는 companion object에

호출마다 재생성하지 않고 상수로 선언한다.

```kotlin
// ❌ Bad: 매 호출마다 재컴파일
fun detectLanguage(text: String): String {
    val japanesePattern = Regex("[\\u3040-\\u309F]")
    ...
}

// ✅ Good: companion object 상수
companion object {
    private val JAPANESE_PATTERN = Regex("[\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FAF]")
    private val OBJECT_MAPPER = jacksonObjectMapper()
}
```

### 11.6 로거에 예외 객체 전달

catch 블록에서 예외를 로깅할 때 스택 트레이스가 남도록 예외 객체를 전달한다.

```kotlin
// ❌ Bad: 스택 트레이스 손실
catch (e: Exception) {
    logger.warn { "Failed: ${e.message}" }
}

// ✅ Good: 예외 객체 포함
catch (e: Exception) {
    logger.warn(e) { "Failed: ${e.message}" }
}
```

### 11.7 URI 인코딩

비ASCII 문자(일본어 등)가 포함된 URL은 `UriBuilder`를 사용한다.

```kotlin
// ❌ Bad: 수동 문자열 조합
val url = "https://api.example.com/items?query=tag:日本"

// ✅ Good: UriBuilder
webClient.get()
    .uri { uriBuilder ->
        uriBuilder.path("/api/v2/items")
            .queryParam("query", "tag:$tag")
            .queryParam("page", page)
            .build()
    }
```

### 11.8 컨트롤러 비동기 처리

크롤링 등 장시간 작업을 수행하는 엔드포인트는 `Callable<T>`를 반환하여 서블릿 스레드를 즉시 반환한다.
Callable 내부에서 try-catch로 감싸서 항상 `ApiResponse`로 응답 포맷을 통일한다.

```kotlin
// ❌ Bad: 동기 호출 (서블릿 스레드 장시간 블로킹)
@PostMapping("/crawl/run")
fun runCrawl(): ApiResponse<Result> {
    val histories = crawlerService.crawlAllSources() // 수 분 소요 가능
    return ApiResponse.success(toResult(histories))
}

// ✅ Good: Callable + 일관된 응답 포맷
@PostMapping("/crawl/run")
fun runCrawl(): Callable<ApiResponse<Result>> {
    if (!crawlInProgress.compareAndSet(false, true)) {
        throw ResponseStatusException(HttpStatus.CONFLICT, "이미 실행 중")
    }
    return Callable {
        try {
            val histories = crawlerService.crawlAllSources()
            ApiResponse.success(toResult(histories), "완료")
        } catch (e: Exception) {
            logger.error(e) { "크롤링 오류" }
            ApiResponse.error("오류: ${e.message}")
        } finally {
            crawlInProgress.set(false)
        }
    }
}
```

### 11.9 독립적인 크롤러 실행

여러 크롤러를 순차 실행할 때, 하나의 실패가 다른 크롤러에 영향을 주지 않도록 각각 try-catch로 감싼다.

```kotlin
// ❌ Bad: 하나 실패 시 전체 중단
val redditHistories = redditCrawlerService.crawlAllSources()
val qiitaHistories = qiitaCrawlerService.crawlAllSources()

// ✅ Good: 독립적 실행
val redditHistories = try {
    redditCrawlerService.crawlAllSources()
} catch (e: Exception) {
    logger.error(e) { "Reddit crawl failed" }
    emptyList()
}
val qiitaHistories = try {
    qiitaCrawlerService.crawlAllSources()
} catch (e: Exception) {
    logger.error(e) { "Qiita crawl failed" }
    emptyList()
}
```

### 11.10 Repository 3-layer 패턴

새로운 쿼리 메서드 추가 시 반드시 3개 레이어를 모두 수정한다:
1. `module-core` — 도메인 인터페이스 (`CrawlSourceRepository`)
2. `module-infrastructure` — JPA 인터페이스 + `@Query` (`JpaCrawlSourceRepository`)
3. `module-infrastructure` — Adapter 위임 (`CrawlSourceRepositoryAdapter`)

---

## 부록: 체크리스트

### PR 전 체크리스트

- [ ] 코드 컨벤션 준수 여부
- [ ] 테스트 코드 작성 여부
- [ ] 로깅 적절성 (catch 블록에서 예외 객체 전달 확인)
- [ ] 예외 처리 완료
- [ ] 문서화 (필요시)

### 코드 리뷰 체크리스트 (기본)

- [ ] 네이밍이 명확한가?
- [ ] 단일 책임 원칙을 따르는가?
- [ ] null 처리가 적절한가?
- [ ] 불필요한 복잡성이 없는가?
- [ ] 테스트가 충분한가?

### 코드 리뷰 체크리스트 (크롤러/외부 연동)

- [ ] HTTP 호출과 `@Transactional`이 같은 클래스에 있지 않은가?
- [ ] N+1 쿼리 없이 배치 조회(`findAllByXxxIn`)를 사용하는가?
- [ ] 개별 `save()` 대신 `saveAll()` 배치 저장을 하는가?
- [ ] 메모리 필터 대신 DB-level 필터 쿼리를 사용하는가?
- [ ] Regex, ObjectMapper 등이 companion object 상수인가?
- [ ] 로거에 예외 객체(`logger.warn(e) { ... }`)를 전달하는가?
- [ ] 비ASCII URL에 `UriBuilder`를 사용하는가?
- [ ] 장시간 엔드포인트가 `Callable<T>` 반환으로 비동기 처리되는가?
- [ ] Callable 내부에서 try-catch로 `ApiResponse` 응답 포맷이 통일되는가?
- [ ] 여러 크롤러 순차 실행 시 각각 독립 try-catch로 격리되는가?
- [ ] Repository 3-layer(도메인 인터페이스 → JPA → Adapter)를 모두 수정했는가?