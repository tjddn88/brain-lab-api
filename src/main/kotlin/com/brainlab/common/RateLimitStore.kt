package com.brainlab.common

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class RateLimitStore {

    // 2분 쿨다운: 제출 응답 대기 중 중복 제출 방지
    private val submitRecentCache: Cache<String, Boolean> = Caffeine.newBuilder()
        .expireAfterWrite(2, TimeUnit.MINUTES)
        .maximumSize(100000)
        .build()

    // 피드백 rate limit: IP당 1시간 1회
    private val feedbackCache: Cache<String, Boolean> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(100000)
        .build()

    /**
     * 제출 가능 여부 확인 (eligibility check용)
     * 세션 토큰 기반으로 실제 보호가 이루어지므로 여기선 쿨다운만 확인
     */
    fun canSubmit(ip: String): Boolean {
        return submitRecentCache.getIfPresent(ip) == null
    }

    /**
     * 실제 제출 시 거부 사유 반환 (null = 허용)
     */
    fun submitRejectReason(ip: String): String? {
        if (submitRecentCache.getIfPresent(ip) != null)
            return "잠시 후 다시 시도해주세요."
        return null
    }

    fun record(ip: String) {
        submitRecentCache.put(ip, true)
    }

    fun canSubmitFeedback(ip: String): Boolean {
        return feedbackCache.getIfPresent(ip) == null
    }

    fun recordFeedback(ip: String) {
        feedbackCache.put(ip, true)
    }
}
