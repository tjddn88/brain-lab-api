package com.brainlab.common

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

@Component
class RateLimitStore {

    // 예외 IP (개발자 IP, 로컬호스트)
    private val exemptIps = setOf(
        "0:0:0:0:0:0:0:1",
        "::1",
        "127.0.0.1",
        "210.179.225.99"
    )

    // key: "ip:YYYY-MM-DD(KST)", TTL: 25시간 (날짜가 바뀌어도 안전하게 만료)
    private val submitCache: Cache<String, Boolean> = Caffeine.newBuilder()
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

    fun canSubmit(ip: String): Boolean {
        if (ip in exemptIps) return true
        return submitCache.getIfPresent(todayKey(ip)) == null
    }

    fun record(ip: String) {
        if (ip in exemptIps) return
        submitCache.put(todayKey(ip), true)
    }

    fun canSubmitFeedback(ip: String): Boolean {
        if (ip in exemptIps) return true
        return feedbackCache.getIfPresent(ip) == null
    }

    fun recordFeedback(ip: String) {
        if (ip in exemptIps) return
        feedbackCache.put(ip, true)
    }
}
