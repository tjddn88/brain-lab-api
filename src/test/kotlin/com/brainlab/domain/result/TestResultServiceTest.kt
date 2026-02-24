package com.brainlab.domain.result

import com.brainlab.api.dto.AnswerItem
import com.brainlab.api.dto.ResultRequest
import com.brainlab.common.NicknameValidator
import com.brainlab.common.RateLimitStore
import com.brainlab.common.SessionStore
import com.brainlab.common.exception.NotFoundException
import com.brainlab.common.exception.RateLimitException
import com.brainlab.common.exception.ValidationException
import com.brainlab.domain.question.Question
import com.brainlab.domain.question.QuestionRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class TestResultServiceTest {

    @Mock private lateinit var resultRepository: TestResultRepository
    @Mock private lateinit var questionRepository: QuestionRepository
    @Mock private lateinit var sessionStore: SessionStore
    @Mock private lateinit var rateLimitStore: RateLimitStore
    @Mock private lateinit var nicknameValidator: NicknameValidator

    @InjectMocks private lateinit var service: TestResultService

    private val testIp = "127.0.0.1"
    private val testToken = "test-session-token"
    private val testNickname = "테스터"

    // ---- 헬퍼 ----

    private fun makeQuestion(id: Long, difficulty: Int, answer: Int = 0) = Question(
        id = id,
        content = "문제 $id",
        options = listOf("A", "B", "C", "D"),
        answer = answer,
        difficulty = difficulty,
        orderNum = id.toInt(),
        category = "수리논리"
    )

    private fun makeRankInfo(higherCount: Long, total: Long): RankInfo = object : RankInfo {
        override fun getHigherCount() = higherCount
        override fun getTotal() = total
    }

    private fun makeSavedResult(
        id: Long = 1L,
        score: Int = 0,
        correctCount: Int = 0,
        timeSeconds: Int = 100,
        estimatedIq: Int = 75,
        shareToken: UUID = UUID.randomUUID()
    ) = TestResult(
        id = id,
        shareToken = shareToken,
        nickname = testNickname,
        score = score,
        correctCount = correctCount,
        timeSeconds = timeSeconds,
        estimatedIq = estimatedIq,
        ipAddress = testIp
    )

    private fun makeRequest(
        answers: List<AnswerItem>,
        nickname: String = testNickname
    ) = ResultRequest(nickname = nickname, answers = answers, sessionToken = testToken)

    /** 정상 제출 경로에 필요한 기본 mock 설정 */
    private fun setupBaseMocks(
        startTime: Instant = Instant.now(),
        questions: List<Question>,
        rankInfo: RankInfo = makeRankInfo(0, 0)
    ) {
        whenever(rateLimitStore.submitRejectReason(testIp)).thenReturn(null)
        whenever(sessionStore.getStartTime(testToken)).thenReturn(startTime)
        whenever(questionRepository.findAllById(any<Iterable<Long>>())).thenReturn(questions)
        whenever(resultRepository.getRankInfo(any())).thenReturn(rankInfo)
        whenever(resultRepository.save(any<TestResult>())).thenAnswer {
            it.arguments[0] as TestResult
        }
    }

    // ---- saveResult ----

    @Test
    fun `saveResult_when_nicknameContainsBadWord_should_throwValidationException`() {
        doThrow(ValidationException("사용 불가 단어")).whenever(nicknameValidator).validate(testNickname)
        val request = makeRequest(listOf(AnswerItem(1L, 0)))

        assertThatThrownBy { service.saveResult(request, testIp) }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("사용 불가 단어")
    }

    @Test
    fun `saveResult_when_dailyRateLimitExceeded_should_throwRateLimitException`() {
        whenever(rateLimitStore.submitRejectReason(testIp)).thenReturn("오늘은 제출 불가")
        val request = makeRequest(listOf(AnswerItem(1L, 0)))

        assertThatThrownBy { service.saveResult(request, testIp) }
            .isInstanceOf(RateLimitException::class.java)
            .hasMessageContaining("오늘은 제출 불가")
    }

    @Test
    fun `saveResult_when_sessionTokenInvalid_should_throwValidationException`() {
        whenever(rateLimitStore.submitRejectReason(testIp)).thenReturn(null)
        whenever(sessionStore.getStartTime(testToken)).thenReturn(null)
        val request = makeRequest(listOf(AnswerItem(1L, 0)))

        assertThatThrownBy { service.saveResult(request, testIp) }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("세션")
    }

    @Test
    fun `saveResult_when_questionIdNotFound_should_throwValidationException`() {
        whenever(rateLimitStore.submitRejectReason(testIp)).thenReturn(null)
        whenever(sessionStore.getStartTime(testToken)).thenReturn(Instant.now())
        // questionId 2가 DB에 없음
        whenever(questionRepository.findAllById(any<Iterable<Long>>()))
            .thenReturn(listOf(makeQuestion(1L, 1)))
        val request = makeRequest(listOf(AnswerItem(1L, 0), AnswerItem(2L, 0)))

        assertThatThrownBy { service.saveResult(request, testIp) }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("유효하지 않은 문제")
    }

    @Test
    fun `saveResult_when_allCorrectFastTime_should_returnMaxBaseScorePlusTimeBonus`() {
        // 5문제 × 난이도 1·2·3 = 15문제 전부 정답, 시작 시간 ≈ 현재 (0초)
        val questions = (1..5).flatMap { i ->
            listOf(
                makeQuestion((i * 3 - 2).toLong(), difficulty = 1, answer = 0),
                makeQuestion((i * 3 - 1).toLong(), difficulty = 2, answer = 0),
                makeQuestion((i * 3).toLong(),     difficulty = 3, answer = 0)
            )
        }
        setupBaseMocks(startTime = Instant.now(), questions = questions)
        val answers = questions.map { AnswerItem(it.id, 0) }

        val response = service.saveResult(makeRequest(answers), testIp)

        // base = 5×50 + 5×100 + 5×150 = 1500, timeBonus ≈ 600 (t ≈ 0)
        assertThat(response.score).isGreaterThanOrEqualTo(1500)
        assertThat(response.correctCount).isEqualTo(15)
    }

    @Test
    fun `saveResult_when_timeExceeds300Seconds_should_returnNoTimeBonus`() {
        val questions = listOf(
            makeQuestion(1L, difficulty = 1, answer = 0), // 50점
            makeQuestion(2L, difficulty = 2, answer = 0)  // 100점
        )
        setupBaseMocks(startTime = Instant.now().minusSeconds(400), questions = questions)
        val answers = questions.map { AnswerItem(it.id, 0) } // 모두 정답

        val response = service.saveResult(makeRequest(answers), testIp)

        // base = 150, timeBonus = max(0, (300-400)×2) = 0
        assertThat(response.score).isEqualTo(150)
    }

    @Test
    fun `saveResult_when_allWrong_should_returnZeroScore`() {
        val questions = listOf(makeQuestion(1L, difficulty = 3, answer = 0))
        setupBaseMocks(startTime = Instant.now().minusSeconds(400), questions = questions)
        val answers = listOf(AnswerItem(1L, 1)) // 오답

        val response = service.saveResult(makeRequest(answers), testIp)

        assertThat(response.score).isEqualTo(0)
        assertThat(response.correctCount).isEqualTo(0)
    }

    @Test
    fun `saveResult_when_timeoutAnswer_should_countAsWrong`() {
        val questions = listOf(makeQuestion(1L, difficulty = 2, answer = 0))
        setupBaseMocks(startTime = Instant.now().minusSeconds(400), questions = questions)
        val answers = listOf(AnswerItem(1L, -1)) // 시간 초과

        val response = service.saveResult(makeRequest(answers), testIp)

        assertThat(response.correctCount).isEqualTo(0)
        val feedback = response.answerFeedback.first()
        assertThat(feedback.isCorrect).isFalse()
        assertThat(feedback.userAnswer).isEqualTo(-1)
    }

    @Test
    fun `saveResult_when_scoreIsZero_should_returnMinIq`() {
        val questions = listOf(makeQuestion(1L, difficulty = 1, answer = 0))
        setupBaseMocks(startTime = Instant.now().minusSeconds(400), questions = questions)
        val answers = listOf(AnswerItem(1L, 1)) // 오답

        val response = service.saveResult(makeRequest(answers), testIp)

        // IQ = (75 + 0/25).coerceIn(75, 150) = 75
        assertThat(response.estimatedIq).isEqualTo(75)
    }

    @Test
    fun `saveResult_when_rankInfoHas10HigherScores_should_returnRank11AndCorrectTopPercent`() {
        val questions = listOf(makeQuestion(1L, difficulty = 1, answer = 0))
        setupBaseMocks(
            startTime = Instant.now().minusSeconds(400),
            questions = questions,
            rankInfo = makeRankInfo(higherCount = 10, total = 99)
        )
        val answers = listOf(AnswerItem(1L, 0)) // 정답

        val response = service.saveResult(makeRequest(answers), testIp)

        // rank = 10 + 1 = 11, totalParticipants = 99 + 1 = 100
        assertThat(response.rank).isEqualTo(11)
        assertThat(response.totalParticipants).isEqualTo(100)
        assertThat(response.topPercent).isEqualTo(11.0)
    }

    @Test
    fun `saveResult_when_firstParticipant_should_returnRank1AndTopPercent100`() {
        val questions = listOf(makeQuestion(1L, difficulty = 1, answer = 0))
        setupBaseMocks(
            startTime = Instant.now(),
            questions = questions,
            rankInfo = makeRankInfo(higherCount = 0, total = 0)
        )

        val response = service.saveResult(makeRequest(listOf(AnswerItem(1L, 0))), testIp)

        assertThat(response.rank).isEqualTo(1)
        assertThat(response.totalParticipants).isEqualTo(1)
        assertThat(response.topPercent).isEqualTo(100.0)
    }

    @Test
    fun `saveResult_when_answersIncludeCorrectAndWrong_should_returnCorrectFeedback`() {
        val questions = listOf(
            makeQuestion(1L, difficulty = 1, answer = 0), // 정답 = 0
            makeQuestion(2L, difficulty = 2, answer = 1)  // 정답 = 1
        )
        setupBaseMocks(startTime = Instant.now(), questions = questions)
        val answers = listOf(
            AnswerItem(1L, 0), // 정답
            AnswerItem(2L, 3)  // 오답
        )

        val response = service.saveResult(makeRequest(answers), testIp)

        val feedbacks = response.answerFeedback
        assertThat(feedbacks).hasSize(2)
        assertThat(feedbacks.find { it.questionId == 1L }?.isCorrect).isTrue()
        assertThat(feedbacks.find { it.questionId == 2L }?.isCorrect).isFalse()
        assertThat(feedbacks.find { it.questionId == 2L }?.correctAnswer).isEqualTo(1)
    }

    // ---- getResult ----

    @Test
    fun `getResult_when_shareTokenNotFound_should_throwNotFoundException`() {
        val token = UUID.randomUUID().toString()
        whenever(resultRepository.findByShareToken(UUID.fromString(token))).thenReturn(null)

        assertThatThrownBy { service.getResult(token) }
            .isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `getResult_when_validShareToken_should_returnResultWithRankInfo`() {
        val token = UUID.randomUUID()
        val saved = makeSavedResult(id = 5L, score = 750, shareToken = token)
        whenever(resultRepository.findByShareToken(token)).thenReturn(saved)
        whenever(resultRepository.getRankInfo(750)).thenReturn(makeRankInfo(9, 50))

        val response = service.getResult(token.toString())

        assertThat(response.id).isEqualTo(5L)
        assertThat(response.score).isEqualTo(750)
        assertThat(response.rank).isEqualTo(10)
        assertThat(response.totalParticipants).isEqualTo(50)
        assertThat(response.answerFeedback).isEmpty()
    }

    // ---- getRanking ----

    @Test
    fun `getRanking_when_has20Participants_should_returnTop10Entries`() {
        val results = (1..20).map { i ->
            TestResult(
                id = i.toLong(),
                nickname = "User$i",
                score = (21 - i) * 100,
                correctCount = i % 15,
                timeSeconds = i * 10,
                estimatedIq = 100,
                ipAddress = "10.0.0.$i"
            )
        }
        whenever(resultRepository.findAllDeduped()).thenReturn(results)

        val response = service.getRanking()

        assertThat(response.topEntries).hasSize(10)
        assertThat(response.topEntries.first().rank).isEqualTo(1)
        assertThat(response.totalCount).isEqualTo(20)
    }

    @Test
    fun `getRanking_when_hasLessThan10Participants_should_returnAllEntries`() {
        val results = (1..5).map { i ->
            TestResult(
                id = i.toLong(),
                nickname = "User$i",
                score = (6 - i) * 100,
                correctCount = 5,
                timeSeconds = i * 10,
                estimatedIq = 100,
                ipAddress = "10.0.0.$i"
            )
        }
        whenever(resultRepository.findAllDeduped()).thenReturn(results)

        val response = service.getRanking()

        assertThat(response.topEntries).hasSize(5)
        assertThat(response.percentileEntries).isEmpty()
        assertThat(response.totalCount).isEqualTo(5)
    }

    @Test
    fun `getRanking_when_hasNoParticipants_should_returnEmptyResponse`() {
        whenever(resultRepository.findAllDeduped()).thenReturn(emptyList())

        val response = service.getRanking()

        assertThat(response.topEntries).isEmpty()
        assertThat(response.percentileEntries).isEmpty()
        assertThat(response.totalCount).isEqualTo(0)
    }
}
