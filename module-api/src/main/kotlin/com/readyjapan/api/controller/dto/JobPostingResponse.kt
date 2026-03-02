package com.readyjapan.api.controller.dto

import com.readyjapan.core.domain.entity.JobPosting

data class JobPostingResponse(
    val id: Long,
    val title: String,
    val titleTranslated: String?,
    val companyName: String,
    val location: String?,
    val employmentType: String?,
    val salary: String?,
    val description: String?,
    val descriptionTranslated: String?,
    val requirements: String?,
    val requirementsTranslated: String?,
    val originalUrl: String,
    val language: String,
    val status: String,
    val postedAt: String?,
    val createdAt: String
) {
    companion object {
        fun from(entity: JobPosting): JobPostingResponse {
            return JobPostingResponse(
                id = entity.id,
                title = entity.title,
                titleTranslated = entity.titleTranslated,
                companyName = entity.companyName,
                location = entity.location,
                employmentType = entity.employmentType?.name,
                salary = entity.salary,
                description = entity.description?.take(300),
                descriptionTranslated = entity.descriptionTranslated?.take(300),
                requirements = null,
                requirementsTranslated = null,
                originalUrl = entity.originalUrl,
                language = entity.language,
                status = entity.status.name,
                postedAt = entity.postedAt?.toString(),
                createdAt = entity.createdAt.toString()
            )
        }

        fun detail(entity: JobPosting): JobPostingResponse {
            return JobPostingResponse(
                id = entity.id,
                title = entity.title,
                titleTranslated = entity.titleTranslated,
                companyName = entity.companyName,
                location = entity.location,
                employmentType = entity.employmentType?.name,
                salary = entity.salary,
                description = entity.description,
                descriptionTranslated = entity.descriptionTranslated,
                requirements = entity.requirements,
                requirementsTranslated = entity.requirementsTranslated,
                originalUrl = entity.originalUrl,
                language = entity.language,
                status = entity.status.name,
                postedAt = entity.postedAt?.toString(),
                createdAt = entity.createdAt.toString()
            )
        }
    }
}
