package com.brainlab.api.dto

data class QuestionDto(
    val id: Long,
    val content: String,
    val options: List<String>,
    val answer: Int,
    val difficulty: Int,
    val orderNum: Int,
    val category: String,
    val correctRate: Double?  // null = 아직 아무도 안 풀었음, 0.0~100.0
)
