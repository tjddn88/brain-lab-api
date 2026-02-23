package com.brainlab.common

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

@Component
class SessionStore {

    private val cache: Cache<String, Instant> = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(10000)
        .build()

    fun create(): String {
        val token = UUID.randomUUID().toString()
        cache.put(token, Instant.now())
        return token
    }

    fun getStartTime(token: String): Instant? = cache.getIfPresent(token)

    fun invalidate(token: String) = cache.invalidate(token)
}
