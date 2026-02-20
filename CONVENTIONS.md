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

### 8.1 테스트 네이밍

```kotlin
class JobPostingServiceTest {

    @Test
    fun `findById - 존재하는 ID로 조회하면 JobPosting을 반환한다`() {
        // given
        // when
        // then
    }

    @Test
    fun `findById - 존재하지 않는 ID로 조회하면 EntityNotFoundException을 던진다`() {
        // given
        // when
        // then
    }
}
```

### 8.2 테스트 구조

```kotlin
@Test
fun `메서드명 - 조건 - 기대결과`() {
    // given: 테스트 데이터 준비
    val request = CreateJobPostingRequest(
        title = "Backend Developer",
        companyName = "Tech Corp"
    )

    // when: 테스트 대상 실행
    val result = service.create(request)

    // then: 결과 검증
    assertThat(result.title).isEqualTo("Backend Developer")
    assertThat(result.companyName).isEqualTo("Tech Corp")
}
```

### 8.3 Mocking (MockK 사용)

```kotlin
@ExtendWith(MockKExtension::class)
class JobPostingServiceTest {

    @MockK
    private lateinit var repository: JobPostingRepository

    @InjectMockKs
    private lateinit var service: JobPostingService

    @Test
    fun `findById 테스트`() {
        // given
        val expected = JobPosting(id = 1L, title = "Test", companyName = "Corp")
        every { repository.findById(1L) } returns expected

        // when
        val result = service.findById(1L)

        // then
        assertThat(result).isEqualTo(expected)
        verify(exactly = 1) { repository.findById(1L) }
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

## 부록: 체크리스트

### PR 전 체크리스트

- [ ] 코드 컨벤션 준수 여부
- [ ] 테스트 코드 작성 여부
- [ ] 로깅 적절성
- [ ] 예외 처리 완료
- [ ] 문서화 (필요시)

### 코드 리뷰 체크리스트

- [ ] 네이밍이 명확한가?
- [ ] 단일 책임 원칙을 따르는가?
- [ ] null 처리가 적절한가?
- [ ] 불필요한 복잡성이 없는가?
- [ ] 테스트가 충분한가?