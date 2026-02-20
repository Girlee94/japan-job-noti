# Ready Japan 프로젝트 컨벤션 요약

## 프로젝트 정보
- 언어: Kotlin 1.9.25
- 프레임워크: Spring Boot 3.4.2
- DB: Supabase PostgreSQL
- JDK: 21

## 멀티 모듈 구조
- module-core: 순수 도메인, 엔티티, 공통 유틸
- module-infrastructure: DB, 외부 API, 크롤러
- module-batch: 스케줄링, 배치
- module-api: REST API

## 핵심 규칙
1. 생성자 주입만 사용
2. Entity 비즈니스 로직 캡슐화
3. DTO는 data class + from() 패턴
4. BusinessException 계층 예외 처리
5. KotlinLogging 로깅
6. !! 연산자 금지

## 네이밍
- 패키지: lowercase, 단수형
- 클래스: PascalCase
- 함수: camelCase, 동사 시작
- 상수: SCREAMING_SNAKE_CASE

## 참조
상세 컨벤션은 프로젝트 루트의 CONVENTIONS.md 참조
