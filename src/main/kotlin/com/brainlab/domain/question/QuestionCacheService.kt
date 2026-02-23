package com.brainlab.domain.question

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
class QuestionCacheService(private val questionRepository: QuestionRepository) {

    @Cacheable("allQuestions")
    fun findAll(): List<Question> = questionRepository.findAll()
}
