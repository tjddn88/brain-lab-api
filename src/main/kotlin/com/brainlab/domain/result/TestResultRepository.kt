package com.brainlab.domain.result

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TestResultRepository : JpaRepository<TestResult, Long> {

    fun countByScoreGreaterThan(score: Int): Long

    @Query("SELECT r FROM TestResult r ORDER BY r.score DESC, r.timeSeconds ASC")
    fun findTopResults(pageable: Pageable): List<TestResult>
}
