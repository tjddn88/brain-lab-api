package com.brainlab.domain.result

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface RankInfo {
    fun getHigherCount(): Long
    fun getTotal(): Long
}

interface TestResultRepository : JpaRepository<TestResult, Long> {

    @Query(
        nativeQuery = true,
        value = """
            SELECT
                COUNT(*) FILTER (WHERE score > :score) AS higher_count,
                COUNT(*) AS total
            FROM test_results
        """
    )
    fun getRankInfo(@Param("score") score: Int): RankInfo

    @Query("SELECT r FROM TestResult r ORDER BY r.score DESC, r.timeSeconds ASC")
    fun findTopResults(pageable: Pageable): List<TestResult>
}
