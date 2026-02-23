package com.brainlab.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class AnswerItem(
    @field:NotNull(message = "문제 ID가 필요합니다.")
    val questionId: Long,

    @field:NotNull(message = "답변이 필요합니다.")
    val answer: Int
)

data class ResultRequest(
    @field:NotBlank(message = "닉네임을 입력해주세요.")
    @field:Size(max = 20, message = "닉네임은 20자 이하여야 합니다.")
    val nickname: String,

    @field:NotEmpty(message = "답변이 필요합니다.")
    @field:Valid
    val answers: List<AnswerItem>,

    @field:NotBlank(message = "세션 토큰이 필요합니다.")
    val sessionToken: String
)

data class QuestionFeedback(
    val questionId: Long,
    val userAnswer: Int,
    val correctAnswer: Int,
    val isCorrect: Boolean,
    val category: String = ""
)

data class ResultResponse(
    val id: Long,
    val shareToken: String,
    val nickname: String,
    val score: Int,
    val correctCount: Int,
    val timeSeconds: Int,
    val rank: Int,
    val totalParticipants: Int,
    val topPercent: Double,
    val estimatedIq: Int,
    val answerFeedback: List<QuestionFeedback> = emptyList()
)

data class RankingEntry(
    val rank: Int,
    val nickname: String,
    val score: Int,
    val correctCount: Int,
    val timeSeconds: Int,
    val estimatedIq: Int
)

data class PercentileEntry(
    val topPercent: Int,
    val rank: Int,
    val nickname: String,
    val score: Int,
    val correctCount: Int,
    val timeSeconds: Int,
    val estimatedIq: Int
)

data class RankingResponse(
    val topEntries: List<RankingEntry>,
    val percentileEntries: List<PercentileEntry>,
    val totalCount: Int
)
