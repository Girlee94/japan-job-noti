package com.readyjapan.infrastructure.orchestration.persistence

import com.readyjapan.core.domain.entity.CommunityPost
import com.readyjapan.core.domain.entity.JobPosting
import com.readyjapan.core.domain.entity.NewsArticle
import com.readyjapan.core.domain.repository.CommunityPostRepository
import com.readyjapan.core.domain.repository.JobPostingRepository
import com.readyjapan.core.domain.repository.NewsArticleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 번역 결과 DB 조회/저장 전용 서비스
 * Spring AOP proxy를 통해 @Transactional이 정상 동작하도록 별도 빈으로 분리
 *
 * LLM API 호출은 포함하지 않음 — 순수 DB 작업만 담당.
 * 읽기 메서드로 반환된 엔티티는 트랜잭션 종료 후 detached 상태가 됨.
 * OrchestrationService에서 LLM 호출 후 변경된 엔티티를 saveAll로 전달하면
 * JPA merge를 통해 변경사항이 반영됨 (DB에서 fetch된 엔티티는 항상 유효한 ID 보유).
 */
@Service
@Transactional(readOnly = true)
class TranslationPersistenceService(
    private val jobPostingRepository: JobPostingRepository,
    private val newsArticleRepository: NewsArticleRepository,
    private val communityPostRepository: CommunityPostRepository
) {

    fun findAllJobsNeedingTranslation(): List<JobPosting> =
        jobPostingRepository.findAllNeedingTranslation()

    fun findAllNewsNeedingTranslation(): List<NewsArticle> =
        newsArticleRepository.findAllNeedingTranslation()

    fun findAllPostsNeedingTranslation(): List<CommunityPost> =
        communityPostRepository.findAllNeedingTranslation()

    @Transactional
    fun saveAllJobPostings(jobPostings: List<JobPosting>) {
        if (jobPostings.isNotEmpty()) {
            jobPostingRepository.saveAll(jobPostings)
        }
    }

    @Transactional
    fun saveAllNewsArticles(newsArticles: List<NewsArticle>) {
        if (newsArticles.isNotEmpty()) {
            newsArticleRepository.saveAll(newsArticles)
        }
    }

    @Transactional
    fun saveAllCommunityPosts(communityPosts: List<CommunityPost>) {
        if (communityPosts.isNotEmpty()) {
            communityPostRepository.saveAll(communityPosts)
        }
    }
}
