package com.brainlab.domain.feedback

import com.brainlab.common.RateLimitStore
import com.brainlab.common.exception.RateLimitException
import com.brainlab.common.exception.ValidationException
import org.springframework.stereotype.Service

@Service
class FeedbackService(
    private val feedbackRepository: FeedbackRepository,
    private val rateLimitStore: RateLimitStore
) {
    fun submit(content: String, ip: String) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) throw ValidationException("피드백 내용을 입력해주세요.")
        if (trimmed.length > 500) throw ValidationException("피드백은 500자 이하여야 합니다.")

        if (!rateLimitStore.canSubmitFeedback(ip)) {
            throw RateLimitException("1시간에 1회만 피드백을 제출할 수 있습니다.")
        }

        rateLimitStore.recordFeedback(ip)
        feedbackRepository.save(Feedback(content = trimmed, ipAddress = ip))
    }
}
