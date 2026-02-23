package com.brainlab.domain.question

import org.springframework.stereotype.Service

@Service
class QuestionService(
    private val questionRepository: QuestionRepository,
    private val questionCacheService: QuestionCacheService
) {

    companion object {
        // 카테고리 출제 순서 고정
        val CATEGORY_ORDER = listOf("수리논리", "언어유추", "인지반사", "공간도형", "패턴논리")
    }

    /**
     * 카테고리별 3문제 순서대로 출제 (총 15문제)
     * - 카테고리 순서: 수리논리 → 언어유추 → 인지반사 → 공간도형 → 패턴논리
     * - 각 카테고리 내 난이도 순: 1 → 2 → 3
     * - 같은 난이도 내에서는 랜덤 선택
     */
    fun getRandomQuestions(): List<Question> {
        val byCategory = questionCacheService.findAll().groupBy { it.category }

        val selected = mutableListOf<Question>()

        for (category in CATEGORY_ORDER) {
            val questions = byCategory[category] ?: continue

            val easy   = questions.filter { it.difficulty == 1 }.shuffled()
            val medium = questions.filter { it.difficulty == 2 }.shuffled()
            val hard   = questions.filter { it.difficulty == 3 }.shuffled()

            val picked = mutableListOf<Question>()
            if (easy.isNotEmpty())   picked.add(easy.first())
            if (medium.isNotEmpty()) picked.add(medium.first())
            if (hard.isNotEmpty())   picked.add(hard.first())

            // 3개 미만이면 미선택 문제에서 채움
            if (picked.size < 3) {
                val remaining = questions.filter { it !in picked }.shuffled()
                picked.addAll(remaining.take(3 - picked.size))
            }

            // 난이도 오름차순 정렬 (1 → 2 → 3)
            selected.addAll(picked.sortedBy { it.difficulty })
        }

        return selected
    }
}
