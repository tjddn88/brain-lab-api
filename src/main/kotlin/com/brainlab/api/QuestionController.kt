package com.brainlab.api

import com.brainlab.api.dto.QuestionDto
import com.brainlab.api.dto.QuestionsResponse
import com.brainlab.common.ApiResponse
import com.brainlab.common.RateLimitStore
import com.brainlab.common.RequestUtils
import com.brainlab.common.SessionStore
import com.brainlab.domain.question.QuestionService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/questions")
class QuestionController(
    private val questionService: QuestionService,
    private val sessionStore: SessionStore,
    private val rateLimitStore: RateLimitStore
) {

    @GetMapping
    fun getQuestions(): ApiResponse<QuestionsResponse> {
        val questions = questionService.getRandomQuestions()
            .map { q ->
                QuestionDto(
                    id = q.id,
                    content = q.content,
                    options = q.options,
                    difficulty = q.difficulty,
                    orderNum = q.orderNum,
                    category = q.category,
                    correctRate = if (q.totalAttempts == 0) null
                                  else Math.round(q.correctCount.toDouble() / q.totalAttempts * 1000) / 10.0,
                    explanation = q.explanation
                )
            }
        val sessionToken = sessionStore.create()
        return ApiResponse.ok(QuestionsResponse(sessionToken = sessionToken, questions = questions))
    }

    @GetMapping("/eligibility")
    fun checkEligibility(request: HttpServletRequest): ApiResponse<Map<String, Boolean>> {
        val ip = RequestUtils.getClientIp(request)
        val canSubmit = rateLimitStore.canSubmit(ip)
        return ApiResponse.ok(mapOf("canSubmit" to canSubmit))
    }
}
