package com.brainlab.domain.question

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.*

@Entity
@Table(name = "questions")
class Question(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Convert(converter = StringListConverter::class)
    @Column(nullable = false, columnDefinition = "TEXT")
    val options: List<String>,

    @Column(nullable = false)
    val answer: Int,

    @Column(nullable = false)
    val difficulty: Int,

    @Column(name = "order_num", nullable = false)
    val orderNum: Int,

    @Column(nullable = false, length = 20)
    val category: String,

    @Column(name = "correct_count", nullable = false)
    val correctCount: Int = 0,

    @Column(name = "total_attempts", nullable = false)
    val totalAttempts: Int = 0,

    @Column(nullable = true, columnDefinition = "TEXT")
    val explanation: String? = null
)

@Converter
class StringListConverter : AttributeConverter<List<String>, String> {
    private val mapper = ObjectMapper()
    private val type = object : TypeReference<List<String>>() {}

    override fun convertToDatabaseColumn(attribute: List<String>): String =
        mapper.writeValueAsString(attribute)

    override fun convertToEntityAttribute(dbData: String): List<String> =
        mapper.readValue(dbData, type)
}
