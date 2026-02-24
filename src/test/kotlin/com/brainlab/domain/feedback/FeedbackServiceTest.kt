package com.brainlab.domain.feedback

import com.brainlab.common.RateLimitStore
import com.brainlab.common.exception.RateLimitException
import com.brainlab.common.exception.ValidationException
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class FeedbackServiceTest {

    @Mock private lateinit var feedbackRepository: FeedbackRepository
    @Mock private lateinit var rateLimitStore: RateLimitStore

    @InjectMocks private lateinit var service: FeedbackService

    private val testIp = "127.0.0.1"

    // ---- 유효성 검사 ----

    @Test
    fun `submit_when_contentIsEmpty_should_throwValidationException`() {
        assertThatThrownBy { service.submit("", testIp) }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("입력")
    }

    @Test
    fun `submit_when_contentIsOnlyWhitespace_should_throwValidationException`() {
        assertThatThrownBy { service.submit("   ", testIp) }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("입력")
    }

    @Test
    fun `submit_when_contentExceeds500Chars_should_throwValidationException`() {
        val tooLong = "a".repeat(501)

        assertThatThrownBy { service.submit(tooLong, testIp) }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("500자")
    }

    @Test
    fun `submit_when_contentIs500Chars_should_saveFeedback`() {
        val exactly500 = "a".repeat(500)
        whenever(rateLimitStore.canSubmitFeedback(testIp)).thenReturn(true)
        whenever(feedbackRepository.save(any<Feedback>())).thenAnswer { it.arguments[0] as Feedback }

        assertThatCode { service.submit(exactly500, testIp) }.doesNotThrowAnyException()
    }

    // ---- Rate limit ----

    @Test
    fun `submit_when_rateLimitExceeded_should_throwRateLimitException`() {
        whenever(rateLimitStore.canSubmitFeedback(testIp)).thenReturn(false)

        assertThatThrownBy { service.submit("정상적인 피드백입니다", testIp) }
            .isInstanceOf(RateLimitException::class.java)
            .hasMessageContaining("1시간")
    }

    // ---- 정상 저장 ----

    @Test
    fun `submit_when_valid_should_saveToRepositoryAndRecordRateLimit`() {
        whenever(rateLimitStore.canSubmitFeedback(testIp)).thenReturn(true)
        whenever(feedbackRepository.save(any<Feedback>())).thenAnswer { it.arguments[0] as Feedback }

        assertThatCode { service.submit("좋은 서비스입니다", testIp) }.doesNotThrowAnyException()

        verify(feedbackRepository).save(any<Feedback>())
        verify(rateLimitStore).recordFeedback(testIp)
    }

    @Test
    fun `submit_when_contentHasLeadingAndTrailingSpaces_should_trimBeforeSaving`() {
        whenever(rateLimitStore.canSubmitFeedback(testIp)).thenReturn(true)
        val captured = mutableListOf<Feedback>()
        whenever(feedbackRepository.save(any<Feedback>())).thenAnswer {
            val f = it.arguments[0] as Feedback
            captured.add(f)
            f
        }

        service.submit("  공백 포함 피드백  ", testIp)

        assertThat(captured).hasSize(1)
        assertThat(captured[0].content).isEqualTo("공백 포함 피드백")
    }

    @Test
    fun `submit_when_valid_should_saveWithCorrectIpAddress`() {
        whenever(rateLimitStore.canSubmitFeedback(testIp)).thenReturn(true)
        val captured = mutableListOf<Feedback>()
        whenever(feedbackRepository.save(any<Feedback>())).thenAnswer {
            val f = it.arguments[0] as Feedback
            captured.add(f)
            f
        }

        service.submit("피드백 내용", testIp)

        assertThat(captured[0].ipAddress).isEqualTo(testIp)
    }
}
