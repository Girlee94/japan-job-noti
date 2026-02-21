package com.readyjapan.infrastructure.orchestration.persistence

import com.readyjapan.core.domain.entity.CommunityPost
import com.readyjapan.core.domain.repository.CommunityPostRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 감정 분석 결과 DB 조회/저장 전용 서비스
 * Spring AOP proxy를 통해 @Transactional이 정상 동작하도록 별도 빈으로 분리
 *
 * LLM API 호출은 포함하지 않음 — 순수 DB 작업만 담당.
 * 읽기 메서드로 반환된 엔티티는 트랜잭션 종료 후 detached 상태가 됨.
 * OrchestrationService에서 LLM 호출 후 변경된 엔티티를 saveAll로 전달하면
 * JPA merge를 통해 변경사항이 반영됨 (DB에서 fetch된 엔티티는 항상 유효한 ID 보유).
 */
@Service
@Transactional(readOnly = true)
class SentimentPersistenceService(
    private val communityPostRepository: CommunityPostRepository
) {

    fun findAllNeedingSentimentAnalysis(): List<CommunityPost> =
        communityPostRepository.findAllNeedingSentimentAnalysis()

    @Transactional
    fun saveAllAnalyzed(posts: List<CommunityPost>) {
        if (posts.isNotEmpty()) {
            communityPostRepository.saveAll(posts)
        }
    }
}
