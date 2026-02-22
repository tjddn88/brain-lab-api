package com.brainlab.domain.result

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "test_results")
class TestResult(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 20)
    val nickname: String,

    @Column(nullable = false)
    val score: Int,

    @Column(name = "correct_count", nullable = false)
    val correctCount: Int,

    @Column(name = "time_seconds", nullable = false)
    val timeSeconds: Int,

    @Column(name = "estimated_iq", nullable = false)
    val estimatedIq: Int,

    @Column(name = "ip_address", nullable = false, length = 45)
    val ipAddress: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
