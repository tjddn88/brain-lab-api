package com.brainlab.common

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

@Component
class RateLimitStore {

    // 2분 쿨다운: 마지막 제출 후 2분간 재제출 차단
    private val submitRecentCache: Cache<String, Boolean> = Caffeine.newBuilder()
        .expireAfterWrite(2, TimeUnit.MINUTES)
        .maximumSize(100000)
        .build()

    // 하루 제한: 쿨다운 중 재시도 시 당일 전체 차단
    private val submitDayBanCache: Cache<String, Boolean> = Caffeine.newBuilder()
        .expireAfterWrite(25, TimeUnit.HOURS)
        .maximumSize(100000)
        .build()

    // 피드백 rate limit: IP당 1시간 1회
    private val feedbackCache: Cache<String, Boolean> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(100000)
        .build()

    private fun todayKey(ip: String): String {
        val today = LocalDate.now(ZoneId.of("Asia/Seoul")).toString()
        return "$ip:$today"
    }

    /**
     * 제출 가능 여부 확인 (side effect 없음, eligibility check용)
     */
    fun canSubmit(ip: String): Boolean {
        return submitDayBanCache.getIfPresent(todayKey(ip)) == null
    }

    /**
     * 실제 제출 시 거부 사유 반환 (null = 허용)
     * 쿨다운 중 재시도 시 하루 제한으로 자동 업그레이드
     */
    fun submitRejectReason(ip: String): String? {
        if (submitDayBanCache.getIfPresent(todayKey(ip)) != null)
            return "오늘은 더 이상 테스트를 제출할 수 없습니다. 내일 다시 도전해주세요."
        if (submitRecentCache.getIfPresent(ip) != null) {
            submitDayBanCache.put(todayKey(ip), true)
            return "중복 제출이 감지되었습니다. 오늘 하루 테스트가 제한됩니다."
        }
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
