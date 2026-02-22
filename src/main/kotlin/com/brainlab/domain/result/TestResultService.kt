package com.brainlab.domain.result

import com.brainlab.api.dto.RankingEntry
import com.brainlab.api.dto.ResultRequest
import com.brainlab.api.dto.ResultResponse
import com.brainlab.common.exception.NotFoundException
import com.brainlab.common.exception.ValidationException
import com.brainlab.domain.question.QuestionRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.max

@Service
class TestResultService(
    private val resultRepository: TestResultRepository,
    private val questionRepository: QuestionRepository
) {

    @Transactional
    fun saveResult(request: ResultRequest, ipAddress: String): ResultResponse {
        val questionIds = request.answers.map { it.questionId }
        val questionMap = questionRepository.findAllById(questionIds).associateBy { it.id }

        if (questionMap.size != questionIds.distinct().size) {
            throw ValidationException("유효하지 않은 문제 ID가 포함되어 있습니다.")
        }

        val correctIds = request.answers
            .filter { item -> questionMap[item.questionId]?.answer == item.answer }
            .map { it.questionId }
        val correctCount = correctIds.size

        val score = calculateScore(correctCount, request.timeSeconds)
        val totalParticipants = resultRepository.count() + 1
        val higherCount = resultRepository.countByScoreGreaterThan(score)
        val rank = (higherCount + 1).toInt()
        val topPercent = Math.round(rank.toDouble() / totalParticipants * 1000) / 10.0
        val estimatedIq = estimateIq(topPercent)

        // 문제별 정답율 카운터 원자적 업데이트
        questionRepository.incrementTotalAttempts(questionIds)
        if (correctIds.isNotEmpty()) {
            questionRepository.incrementCorrectCounts(correctIds)
        }

        val saved = resultRepository.save(
            TestResult(
                nickname = request.nickname,
                score = score,
                correctCount = correctCount,
                timeSeconds = request.timeSeconds,
                estimatedIq = estimatedIq,
                ipAddress = ipAddress
            )
        )

        return ResultResponse(
            id = saved.id,
            nickname = saved.nickname,
            score = saved.score,
            correctCount = saved.correctCount,
            timeSeconds = saved.timeSeconds,
            rank = rank,
            totalParticipants = totalParticipants.toInt(),
            topPercent = topPercent,
            estimatedIq = saved.estimatedIq
        )
    }

    @Transactional(readOnly = true)
    fun getResult(id: Long): ResultResponse {
        val result = resultRepository.findById(id)
            .orElseThrow { NotFoundException("결과를 찾을 수 없습니다. id=$id") }

        val totalParticipants = resultRepository.count()
        val higherCount = resultRepository.countByScoreGreaterThan(result.score)
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
