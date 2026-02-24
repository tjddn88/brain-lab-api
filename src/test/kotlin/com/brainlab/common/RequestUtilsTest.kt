package com.brainlab.common

import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class RequestUtilsTest {

    // MockitoExtension 없이 직접 mock 생성 (미사용 stub 엄격 검사 불필요)
    private val request: HttpServletRequest = mock(HttpServletRequest::class.java)

    @Test
    fun `getClientIp_when_xRealIpPresent_should_returnXRealIp`() {
        whenever(request.getHeader("X-Real-IP")).thenReturn("203.0.113.1")

        assertThat(RequestUtils.getClientIp(request)).isEqualTo("203.0.113.1")
    }

    @Test
    fun `getClientIp_when_xRealIpHasSpaces_should_returnTrimmedIp`() {
        whenever(request.getHeader("X-Real-IP")).thenReturn("  203.0.113.1  ")

        assertThat(RequestUtils.getClientIp(request)).isEqualTo("203.0.113.1")
    }

    @Test
    fun `getClientIp_when_onlyXForwardedForPresent_should_returnFirstIp`() {
        whenever(request.getHeader("X-Real-IP")).thenReturn(null)
        whenever(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2, 10.0.0.3")

        assertThat(RequestUtils.getClientIp(request)).isEqualTo("10.0.0.1")
    }

    @Test
    fun `getClientIp_when_xForwardedForHasSpaces_should_returnTrimmedFirstIp`() {
        whenever(request.getHeader("X-Real-IP")).thenReturn(null)
        whenever(request.getHeader("X-Forwarded-For")).thenReturn("  10.0.0.1  , 10.0.0.2")

        assertThat(RequestUtils.getClientIp(request)).isEqualTo("10.0.0.1")
    }

    @Test
    fun `getClientIp_when_noHeadersPresent_should_returnRemoteAddr`() {
        whenever(request.getHeader("X-Real-IP")).thenReturn(null)
        whenever(request.getHeader("X-Forwarded-For")).thenReturn(null)
        whenever(request.remoteAddr).thenReturn("192.168.0.1")

        assertThat(RequestUtils.getClientIp(request)).isEqualTo("192.168.0.1")
    }

    @Test
    fun `getClientIp_when_xRealIpAndXForwardedForBothPresent_should_prioritizeXRealIp`() {
        whenever(request.getHeader("X-Real-IP")).thenReturn("203.0.113.1")
        whenever(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1")

        assertThat(RequestUtils.getClientIp(request)).isEqualTo("203.0.113.1")
    }

    @Test
    fun `getClientIp_when_xRealIpIsBlank_should_fallbackToXForwardedFor`() {
        whenever(request.getHeader("X-Real-IP")).thenReturn("   ")
        whenever(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1")

        assertThat(RequestUtils.getClientIp(request)).isEqualTo("10.0.0.1")
    }

    @Test
    fun `getClientIp_when_allHeadersBlank_should_returnRemoteAddr`() {
        whenever(request.getHeader("X-Real-IP")).thenReturn("")
        whenever(request.getHeader("X-Forwarded-For")).thenReturn("")
        whenever(request.remoteAddr).thenReturn("172.16.0.1")

        assertThat(RequestUtils.getClientIp(request)).isEqualTo("172.16.0.1")
    }
}
