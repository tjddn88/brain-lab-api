package com.brainlab.common

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.TimeUnit

@Component
class RateLimitStore {

    private val cache: Cache<String, Instant> = Caffeine.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .maximumSize(100000)
        .build()

    fun canSubmit(ip: String): Boolean = cache.getIfPresent(ip) == null

    fun record(ip: String) = cache.put(ip, Instant.now())
}
