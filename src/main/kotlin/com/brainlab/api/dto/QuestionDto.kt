package com.brainlab.api.dto

data class QuestionDto(
    val id: Long,
    val content: String,
    val options: List<String>,
    val difficulty: Int,
    val orderNum: Int,
    val category: String,
    val correctRate: Double?
)

data class QuestionsResponse(
    val sessionToken: String,
    val questions: List<QuestionDto>
)
