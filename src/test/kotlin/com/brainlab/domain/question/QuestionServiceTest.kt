package com.brainlab.domain.question

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class QuestionServiceTest {

    @Mock private lateinit var questionRepository: QuestionRepository
    @Mock private lateinit var questionCacheService: QuestionCacheService

    @InjectMocks private lateinit var service: QuestionService

    // ---- 헬퍼 ----

    private fun makeQuestion(id: Long, category: String, difficulty: Int) = Question(
        id = id,
        content = "문제 $id",
        options = listOf("A", "B", "C", "D"),
        answer = 0,
        difficulty = difficulty,
        orderNum = id.toInt(),
        category = category
    )

    /** 5개 카테고리 × 난이도 1·2·3 = 15문제 기본 세트 */
    private fun makeFullQuestionSet(): List<Question> {
        var id = 0L
        return QuestionService.CATEGORY_ORDER.flatMap { category ->
            listOf(1, 2, 3).map { diff -> makeQuestion(++id, category, diff) }
        }
    }

    // ---- 테스트 ----

    @Test
    fun `getRandomQuestions_when_called_should_return15Questions`() {
        whenever(questionCacheService.findAll()).thenReturn(makeFullQuestionSet())

        val result = service.getRandomQuestions()

        assertThat(result).hasSize(15)
    }

    @Test
    fun `getRandomQuestions_when_called_should_followFixedCategoryOrder`() {
        whenever(questionCacheService.findAll()).thenReturn(makeFullQuestionSet())

        val result = service.getRandomQuestions()

        val categories = result.map { it.category }
        val expected = QuestionService.CATEGORY_ORDER.flatMap { cat -> List(3) { cat } }
        assertThat(categories).containsExactlyElementsOf(expected)
    }

    @Test
    fun `getRandomQuestions_when_called_should_includeOneDifficultyPerLevelInEachCategory`() {
        whenever(questionCacheService.findAll()).thenReturn(makeFullQuestionSet())

        val result = service.getRandomQuestions()

        QuestionService.CATEGORY_ORDER.forEach { category ->
            val difficulties = result.filter { it.category == category }.map { it.difficulty }
            assertThat(difficulties).containsExactlyInAnyOrder(1, 2, 3)
        }
    }

    @Test
    fun `getRandomQuestions_when_called_should_sortByDifficultyAscendingWithinCategory`() {
        whenever(questionCacheService.findAll()).thenReturn(makeFullQuestionSet())

        val result = service.getRandomQuestions()

        QuestionService.CATEGORY_ORDER.forEach { category ->
            val difficulties = result.filter { it.category == category }.map { it.difficulty }
            assertThat(difficulties).containsExactly(1, 2, 3)
        }
    }

    @Test
    fun `getRandomQuestions_when_categoryMissing_should_skipMissingCategory`() {
        // 공간도형·패턴논리 제외, 3개 카테고리만 존재
        val questions = listOf("수리논리", "언어유추", "인지반사").flatMap { category ->
            (1..3).map { diff ->
                makeQuestion(category.hashCode().toLong() * 10 + diff, category, diff)
            }
        }
        whenever(questionCacheService.findAll()).thenReturn(questions)

        val result = service.getRandomQuestions()

        assertThat(result).hasSize(9) // 3 카테고리 × 3문제
        assertThat(result.map { it.category }.distinct())
            .containsExactlyInAnyOrder("수리논리", "언어유추", "인지반사")
    }

    @Test
    fun `getRandomQuestions_when_categoryHasNoDifficulty3_should_fillFromRemainingQuestions`() {
        // 모든 카테고리에 난이도 3 없음 → remaining fill 로직 실행
        val questions = QuestionService.CATEGORY_ORDER.flatMap { category ->
            listOf(
                makeQuestion(category.hashCode().toLong() + 100L, category, 1),
                makeQuestion(category.hashCode().toLong() + 200L, category, 2),
                makeQuestion(category.hashCode().toLong() + 300L, category, 1) // 추가 난이도 1
            )
        }
        whenever(questionCacheService.findAll()).thenReturn(questions)

        val result = service.getRandomQuestions()

        assertThat(result).hasSize(15)
    }

    @Test
    fun `getRandomQuestions_when_multipleSameDifficultyQuestions_should_pickExactlyOne`() {
        // 카테고리당 난이도별 3개씩 → 각 난이도에서 1개만 선택되어야 함
        var id = 0L
        val questions = QuestionService.CATEGORY_ORDER.flatMap { category ->
            (1..3).flatMap { diff ->
                (1..3).map { makeQuestion(++id, category, diff) }
            }
        }
        whenever(questionCacheService.findAll()).thenReturn(questions)

        val result = service.getRandomQuestions()

        assertThat(result).hasSize(15)
        QuestionService.CATEGORY_ORDER.forEach { category ->
            val perCategory = result.filter { it.category == category }
            assertThat(perCategory).hasSize(3)
            assertThat(perCategory.map { it.difficulty }).containsExactly(1, 2, 3)
        }
    }
}
