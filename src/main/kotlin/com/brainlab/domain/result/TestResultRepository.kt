package com.brainlab.domain.result

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface RankInfo {
    fun getHigherCount(): Long
    fun getTotal(): Long
}

interface TestResultRepository : JpaRepository<TestResult, Long> {

    fun findByShareToken(shareToken: UUID): TestResult?

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

    @Query(
        nativeQuery = true,
        value = """
            SELECT * FROM (
                SELECT DISTINCT ON (ip_address) *
                FROM test_results
                ORDER BY ip_address, score DESC, time_seconds ASC
            ) sub
            ORDER BY score DESC, time_seconds ASC
        """
    )
    fun findAllDeduped(): List<TestResult>
}
