package com.brainlab.common

import com.brainlab.common.exception.ValidationException
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class NicknameValidatorTest {

    private val validator = NicknameValidator()

    @Test
    fun `validate_when_cleanNickname_should_notThrow`() {
        assertThatCode { validator.validate("BrainUser") }.doesNotThrowAnyException()
    }

    @Test
    fun `validate_when_cleanKoreanNickname_should_notThrow`() {
        assertThatCode { validator.validate("천재뇌왕") }.doesNotThrowAnyException()
    }

    @Test
    fun `validate_when_nicknameContainsBadWord_should_throwValidationException`() {
        assertThatThrownBy { validator.validate("씨발놈") }
            .isInstanceOf(ValidationException::class.java)
    }

    @Test
    fun `validate_when_badWordEmbeddedInNickname_should_throwValidationException`() {
        // 닉네임 중간에 비속어가 포함된 경우 (substring 탐지)
        assertThatThrownBy { validator.validate("나는씨발이야") }
            .isInstanceOf(ValidationException::class.java)
    }

    @Test
    fun `validate_when_uppercaseBadWord_should_detectCaseInsensitively`() {
        // badwords는 lowercase로 저장, 닉네임도 lowercase 변환 후 비교
        assertThatThrownBy { validator.validate("FUCK유") }
            .isInstanceOf(ValidationException::class.java)
    }

    @Test
    fun `validate_when_mixedCaseBadWord_should_detectCaseInsensitively`() {
        assertThatThrownBy { validator.validate("FuCk넌") }
            .isInstanceOf(ValidationException::class.java)
    }
}
