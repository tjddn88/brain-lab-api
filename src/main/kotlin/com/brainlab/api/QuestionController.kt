package com.brainlab.api

import com.brainlab.api.dto.QuestionDto
import com.brainlab.common.ApiResponse
import com.brainlab.domain.question.QuestionService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/questions")
class QuestionController(
    private val questionService: QuestionService
) {

    @GetMapping
    fun getQuestions(): ApiResponse<List<QuestionDto>> {
        val questions = questionService.getRandomQuestions()
            .map { q ->
                QuestionDto(
                    id = q.id,
                    content = q.content,
                    options = q.options,
                    answer = q.answer,
                    difficulty = q.difficulty,
                    orderNum = q.orderNum,
                    category = q.category,
                    correctRate = if (q.totalAttempts == 0) null
                                  else Math.round(q.correctCount.toDouble() / q.totalAttempts * 1000) / 10.0
                )
            }
        return ApiResponse.ok(questions)
    }
}
