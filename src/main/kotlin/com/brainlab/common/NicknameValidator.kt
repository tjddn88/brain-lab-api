package com.brainlab.common

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

@Component
class NicknameValidator {
    private val badWords: Set<String> = run {
        val json = NicknameValidator::class.java.getResourceAsStream("/badwords.json")!!
            .bufferedReader().readText()
        ObjectMapper().readValue(json, object : TypeReference<List<String>>() {})
            .map { it.lowercase() }
            .toHashSet()
    }

    fun validate(nickname: String) {
        val lower = nickname.lowercase()
        val found = badWords.any { lower.contains(it) }
        if (found) throw com.brainlab.common.exception.ValidationException("닉네임에 사용할 수 없는 단어가 포함되어 있습니다.")
    }
}
