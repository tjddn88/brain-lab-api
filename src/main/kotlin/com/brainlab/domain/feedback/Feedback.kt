package com.brainlab.domain.feedback

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "feedbacks")
class Feedback(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Column(name = "ip_address", length = 45)
    val ipAddress: String? = null,

    @Column(name = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
