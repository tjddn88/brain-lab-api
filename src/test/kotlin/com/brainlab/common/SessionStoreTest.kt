package com.brainlab.common

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class SessionStoreTest {

    private lateinit var store: SessionStore

    @BeforeEach
    fun setUp() {
        store = SessionStore()
    }

    @Test
    fun `create_when_called_should_returnNonNullToken`() {
        val token = store.create()

        assertThat(token).isNotNull()
        assertThat(token).isNotBlank()
    }

    @Test
    fun `create_when_calledTwice_should_returnUniqueTokens`() {
        val token1 = store.create()
        val token2 = store.create()

        assertThat(token1).isNotEqualTo(token2)
    }

    @Test
    fun `getStartTime_when_tokenCreated_should_returnInstantCloseToNow`() {
        val before = Instant.now()
        val token = store.create()
        val after = Instant.now()

        val startTime = store.getStartTime(token)

        assertThat(startTime).isNotNull()
        assertThat(startTime).isAfterOrEqualTo(before)
        assertThat(startTime).isBeforeOrEqualTo(after)
    }

    @Test
    fun `getStartTime_when_tokenDoesNotExist_should_returnNull`() {
        val result = store.getStartTime("non-existent-token")

        assertThat(result).isNull()
    }

    @Test
    fun `invalidate_when_tokenExists_should_removeToken`() {
        val token = store.create()
        assertThat(store.getStartTime(token)).isNotNull()

        store.invalidate(token)

        assertThat(store.getStartTime(token)).isNull()
    }

    @Test
    fun `invalidate_when_unknownToken_should_notThrow`() {
        assertThatCode { store.invalidate("unknown-token") }.doesNotThrowAnyException()
    }
}
