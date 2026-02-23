package com.brainlab.domain.result

import com.brainlab.api.dto.PercentileEntry
import com.brainlab.api.dto.QuestionFeedback
import com.brainlab.api.dto.RankingEntry
import com.brainlab.api.dto.RankingResponse
import com.brainlab.api.dto.ResultRequest
import com.brainlab.api.dto.ResultResponse
import com.brainlab.common.RateLimitStore
import com.brainlab.common.SessionStore
import com.brainlab.common.exception.NotFoundException
import java.util.UUID
import com.brainlab.common.exception.RateLimitException
import com.brainlab.common.exception.ValidationException
import com.brainlab.domain.question.QuestionRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import kotlin.math.max

@Service
class TestResultService(
    private val resultRepository: TestResultRepository,
    private val questionRepository: QuestionRepository,
    private val sessionStore: SessionStore,
    private val rateLimitStore: RateLimitStore,
    private val nicknameValidator: com.brainlab.common.NicknameValidator
) {
    @CacheEvict("ranking", allEntries = true)
    @Transactional
    fun saveResult(request: ResultRequest, ipAddress: String): ResultResponse {
        // 닉네임 비속어 검사
        nicknameValidator.validate(request.nickname)

        // IP 제한 체크 (2분 쿨다운, 위반 시 당일 전체 차단)
        val rejectReason = rateLimitStore.submitRejectReason(ipAddress)
        if (rejectReason != null) throw RateLimitException(rejectReason)

        // 세션 검증 및 서버 기준 시간 계산
        val startTime = sessionStore.getStartTime(request.sessionToken)
            ?: throw ValidationException("유효하지 않은 세션입니다. 테스트를 다시 시작해주세요.")
        val timeSeconds = Duration.between(startTime, Instant.now()).seconds
            .coerceIn(0L, 600L).toInt()

        val questionIds = request.answers.map { it.questionId }
        val questionMap = questionRepository.findAllById(questionIds).associateBy { it.id }

        if (questionMap.size != questionIds.distinct().size) {
            throw ValidationException("유효하지 않은 문제 ID가 포함되어 있습니다.")
        }

        val correctIds = request.answers
            .filter { item -> questionMap[item.questionId]?.answer == item.answer }
            .map { it.questionId }
        val correctCount = correctIds.size

        val score = calculateScore(correctIds, questionMap, timeSeconds)

        val rankInfo = resultRepository.getRankInfo(score)
        val higherCount = rankInfo.getHigherCount()
        // getRankInfo는 save 이전 호출 → 현재 제출 건은 미포함이므로 +1
        val totalParticipants = (rankInfo.getTotal() + 1).coerceAtLeast(1)
        val rank = (higherCount + 1).toInt()
        val topPercent = Math.round(rank.toDouble() / totalParticipants * 1000) / 10.0
        val estimatedIq = estimateIq(score)

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

        // save 성공 후 세션 무효화 → DB 오류 시 유저가 재시도 가능
        sessionStore.invalidate(request.sessionToken)
        rateLimitStore.record(ipAddress)

        val answerFeedback = request.answers.map { item ->
            val correctAnswer = questionMap[item.questionId]?.answer ?: -1
            QuestionFeedback(
                questionId = item.questionId,
                userAnswer = item.answer,
                correctAnswer = correctAnswer,
                isCorrect = item.answer != -1 && item.answer == correctAnswer,
                category = questionMap[item.questionId]?.category ?: ""
            )
        }

        return ResultResponse(
            id = saved.id,
            shareToken = saved.shareToken.toString(),
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
    fun getResult(shareToken: String): ResultResponse {
        val result = resultRepository.findByShareToken(UUID.fromString(shareToken))
            ?: throw NotFoundException("결과를 찾을 수 없습니다.")

        val rankInfo = resultRepository.getRankInfo(result.score)
        val higherCount = rankInfo.getHigherCount()
        // 이미 저장된 결과 조회 → DB의 실제 총 참여자수 사용
        val totalParticipants = rankInfo.getTotal().coerceAtLeast(1)
        val rank = (higherCount + 1).toInt()
        val topPercent = Math.round(rank.toDouble() / totalParticipants * 1000) / 10.0

        return ResultResponse(
            id = result.id,
            shareToken = result.shareToken.toString(),
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
    fun getRanking(): RankingResponse {
        val all = resultRepository.findAllDeduped()
        val total = all.size

        val topEntries = all.take(10).mapIndexed { index, r ->
            RankingEntry(
                rank = index + 1,
                nickname = r.nickname,
                score = r.score,
                correctCount = r.correctCount,
                timeSeconds = r.timeSeconds,
                estimatedIq = r.estimatedIq
            )
        }

        val seenPositions = mutableSetOf<Int>()
        val percentileEntries = listOf(30, 50, 70, 90).mapNotNull { pct ->
            val pos = (total * pct / 100.0).toInt().coerceIn(10, total - 1)
            if (pos < 10 || pos >= total || !seenPositions.add(pos)) return@mapNotNull null
            val r = all[pos]
            PercentileEntry(
                topPercent = pct,
                rank = pos + 1,
                nickname = r.nickname,
                score = r.score,
                correctCount = r.correctCount,
                timeSeconds = r.timeSeconds,
                estimatedIq = r.estimatedIq
            )
        }

        return RankingResponse(topEntries = topEntries, percentileEntries = percentileEntries, totalCount = total)
    }

    private fun calculateScore(
        correctIds: List<Long>,
        questionMap: Map<Long, com.brainlab.domain.question.Question>,
        timeSeconds: Int
    ): Int {
        // 난이도별 배점: 1=50점, 2=100점, 3=150점
        val baseScore = correctIds.map { id ->
            when (questionMap[id]?.difficulty) {
                1 -> 50
                2 -> 100
                3 -> 150
                else -> 100
            }
        }.sum()
        // 시간보너스: 5분(300초) 기준, 빠를수록 최대 600점
        val timeBonus = max(0, (300 - timeSeconds) * 2)
        return baseScore + timeBonus
    }

    private fun estimateIq(score: Int): Int =
        (75 + score / 25).coerceIn(75, 150)
}
