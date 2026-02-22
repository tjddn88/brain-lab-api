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

    @field:NotNull(message = "풀이 시간이 필요합니다.")
    val timeSeconds: Int
)

data class ResultResponse(
    val id: Long,
    val nickname: String,
    val score: Int,
    val correctCount: Int,
    val timeSeconds: Int,
    val rank: Int,
    val totalParticipants: Int,
    val topPercent: Double,
    val estimatedIq: Int
)

data class RankingEntry(
    val rank: Int,
    val nickname: String,
    val score: Int,
    val correctCount: Int,
    val timeSeconds: Int,
    val estimatedIq: Int
)
