package com.brainlab.domain.result

import com.brainlab.api.dto.QuestionFeedback
import com.brainlab.api.dto.RankingEntry
import com.brainlab.api.dto.ResultRequest
import com.brainlab.api.dto.ResultResponse
import com.brainlab.common.RateLimitStore
import com.brainlab.common.SessionStore
import com.brainlab.common.exception.NotFoundException
import com.brainlab.common.exception.RateLimitException
import com.brainlab.common.exception.ValidationException
import com.brainlab.domain.question.QuestionRepository
import jakarta.annotation.PostConstruct
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

@Service
class TestResultService(
    private val resultRepository: TestResultRepository,
    private val questionRepository: QuestionRepository,
    private val sessionStore: SessionStore,
    private val rateLimitStore: RateLimitStore
) {
    private val participantCount = AtomicLong(0)

    @PostConstruct
    fun init() {
        participantCount.set(resultRepository.count())
    }

    @CacheEvict("ranking", allEntries = true)
    @Transactional
    fun saveResult(request: ResultRequest, ipAddress: String): ResultResponse {
        // IP 제한 체크 (5분에 1번)
        if (!rateLimitStore.canSubmit(ipAddress)) {
            throw RateLimitException("5분 후에 다시 시도해주세요.")
        }

        // 세션 검증 및 서버 기준 시간 계산
        val startTime = sessionStore.getStartTime(request.sessionToken)
            ?: throw ValidationException("유효하지 않은 세션입니다. 테스트를 다시 시작해주세요.")
        val timeSeconds = Duration.between(startTime, Instant.now()).seconds
            .coerceIn(0L, 600L).toInt()
        sessionStore.invalidate(request.sessionToken)

        val questionIds = request.answers.map { it.questionId }
        val questionMap = questionRepository.findAllById(questionIds).associateBy { it.id }

        if (questionMap.size != questionIds.distinct().size) {
            throw ValidationException("유효하지 않은 문제 ID가 포함되어 있습니다.")
        }

        val correctIds = request.answers
            .filter { item -> questionMap[item.questionId]?.answer == item.answer }
            .map { it.questionId }
        val correctCount = correctIds.size

        val score = calculateScore(correctCount, timeSeconds)

        val rankInfo = resultRepository.getRankInfo(score)
        val higherCount = rankInfo.getHigherCount()
        val totalParticipants = participantCount.get() + 1
        val rank = (higherCount + 1).toInt()
        val topPercent = Math.round(rank.toDouble() / totalParticipants * 1000) / 10.0
        val estimatedIq = estimateIq(topPercent)

        questionRepository.incrementTotalAttempts(questionIds)
        if (correctIds.isNotEmpty()) {
            questionRepository.incrementCorrectCounts(correctIds)
        }

        val saved = resultRepository.save(
            TestResult(
                nickname = request.nickname,
                score = score,
                correctCount = correctCount,
                timeSeconds = timeSeconds,
                estimatedIq = estimatedIq,
                ipAddress = ipAddress
            )
        )

        rateLimitStore.record(ipAddress)
        participantCount.incrementAndGet()

        val answerFeedback = request.answers.map { item ->
            val correctAnswer = questionMap[item.questionId]?.answer ?: -1
            QuestionFeedback(
                questionId = item.questionId,
                userAnswer = item.answer,
                correctAnswer = correctAnswer,
                isCorrect = item.answer != -1 && item.answer == correctAnswer
            )
        }

        return ResultResponse(
            id = saved.id,
            nickname = saved.nickname,
            score = saved.score,
            correctCount = saved.correctCount,
            timeSeconds = saved.timeSeconds,
            rank = rank,
            totalParticipants = totalParticipants.toInt(),
            topPercent = topPercent,
            estimatedIq = saved.estimatedIq,
            answerFeedback = answerFeedback
        )
    }

    @Transactional(readOnly = true)
    fun getResult(id: Long): ResultResponse {
        val result = resultRepository.findById(id)
            .orElseThrow { NotFoundException("결과를 찾을 수 없습니다. id=$id") }

        val rankInfo = resultRepository.getRankInfo(result.score)
        val higherCount = rankInfo.getHigherCount()
        val totalParticipants = participantCount.get()
        val rank = (higherCount + 1).toInt()
        val topPercent = Math.round(rank.toDouble() / totalParticipants * 1000) / 10.0

        return ResultResponse(
            id = result.id,
            nickname = result.nickname,
            score = result.score,
            correctCount = result.correctCount,
            timeSeconds = result.timeSeconds,
            rank = rank,
            totalParticipants = totalParticipants.toInt(),
            topPercent = topPercent,
            estimatedIq = result.estimatedIq
        )
    }

    @Cacheable("ranking")
    @Transactional(readOnly = true)
    fun getRanking(): List<RankingEntry> {
        val results = resultRepository.findTopResults(PageRequest.of(0, 50))
        return results.mapIndexed { index, r ->
            RankingEntry(
                rank = index + 1,
                nickname = r.nickname,
                score = r.score,
                correctCount = r.correctCount,
                timeSeconds = r.timeSeconds,
                estimatedIq = r.estimatedIq
            )
        }
    }

    private fun calculateScore(correctCount: Int, timeSeconds: Int): Int {
        val baseScore = correctCount * 100
        val timeBonus = max(0, (600 - timeSeconds) / 6)
        return baseScore + timeBonus
    }

    private fun estimateIq(topPercent: Double): Int = when {
        topPercent <= 2.0  -> 130
        topPercent <= 5.0  -> 125
        topPercent <= 10.0 -> 119
        topPercent <= 25.0 -> 110
        topPercent <= 50.0 -> 100
        topPercent <= 75.0 -> 90
        topPercent <= 90.0 -> 81
        else               -> 75
    }
}
