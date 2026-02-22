package com.brainlab.domain.question

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface QuestionRepository : JpaRepository<Question, Long> {

    fun findAllByOrderByOrderNumAsc(): List<Question>

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Question q SET q.totalAttempts = q.totalAttempts + 1 WHERE q.id IN :ids")
    fun incrementTotalAttempts(ids: List<Long>)

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Question q SET q.correctCount = q.correctCount + 1 WHERE q.id IN :ids")
    fun incrementCorrectCounts(ids: List<Long>)
}
