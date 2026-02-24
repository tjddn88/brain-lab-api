package com.brainlab.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RateLimitStoreTest {

    private lateinit var store: RateLimitStore

    private val testIp = "192.168.1.1"

    @BeforeEach
    fun setUp() {
        store = RateLimitStore()
    }

    // ---- canSubmit ----

    @Test
    fun `canSubmit_when_noPreviousSubmission_should_returnTrue`() {
        assertThat(store.canSubmit(testIp)).isTrue()
    }

    @Test
    fun `canSubmit_when_recentSubmission_should_returnFalseWithinCooldown`() {
        // record()로 2분 쿨다운 등록 → 즉시 canSubmit은 false
        store.record(testIp)

        assertThat(store.canSubmit(testIp)).isFalse()
    }

    // ---- submitRejectReason ----

    @Test
    fun `submitRejectReason_when_firstSubmission_should_returnNull`() {
        val reason = store.submitRejectReason(testIp)

        assertThat(reason).isNull()
    }

    @Test
    fun `submitRejectReason_when_afterRecord_should_returnCooldownMessage`() {
        // record() → 2분 쿨다운 등록 → 재시도 시 쿨다운 메시지
        store.record(testIp)

        val reason = store.submitRejectReason(testIp)

        assertThat(reason).isNotNull()
        assertThat(reason).contains("잠시")
    }

    @Test
    fun `submitRejectReason_when_cooldownActive_should_returnRejectMessage`() {
        store.record(testIp) // 쿨다운 등록

        val reason = store.submitRejectReason(testIp)

        assertThat(reason).isNotNull()
        assertThat(reason).isEqualTo("잠시 후 다시 시도해주세요.")
    }

    @Test
    fun `submitRejectReason_when_differentIp_should_returnNull`() {
        store.record(testIp) // testIp만 차단

        val reason = store.submitRejectReason("10.0.0.1")

        assertThat(reason).isNull()
    }

    // ---- feedback rate limit ----

    @Test
    fun `canSubmitFeedback_when_noPreviousSubmission_should_returnTrue`() {
        assertThat(store.canSubmitFeedback(testIp)).isTrue()
    }

    @Test
    fun `canSubmitFeedback_when_alreadySubmitted_should_returnFalse`() {
        store.recordFeedback(testIp)

        assertThat(store.canSubmitFeedback(testIp)).isFalse()
    }

    @Test
    fun `recordFeedback_when_called_should_preventSubsequentSubmissions`() {
        assertThat(store.canSubmitFeedback(testIp)).isTrue()

        store.recordFeedback(testIp)

        assertThat(store.canSubmitFeedback(testIp)).isFalse()
    }

    @Test
    fun `canSubmitFeedback_when_differentIp_should_returnTrue`() {
        store.recordFeedback(testIp)

        assertThat(store.canSubmitFeedback("10.0.0.2")).isTrue()
    }
}
