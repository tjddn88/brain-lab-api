package com.brainlab.api

import com.brainlab.common.ApiResponse
import com.brainlab.common.RequestUtils
import com.brainlab.domain.feedback.FeedbackService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class FeedbackRequest(val content: String = "")

@RestController
@RequestMapping("/api/feedbacks")
class FeedbackController(
    private val feedbackService: FeedbackService
) {
    @PostMapping
    fun submit(
        @RequestBody req: FeedbackRequest,
        request: HttpServletRequest
    ): ApiResponse<Unit> {
        val ip = RequestUtils.getClientIp(request)
        feedbackService.submit(req.content, ip)
        return ApiResponse.ok(Unit)
    }
}
