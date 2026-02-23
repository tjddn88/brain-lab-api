package com.brainlab.api

import com.brainlab.api.dto.RankingResponse
import com.brainlab.api.dto.ResultRequest
import com.brainlab.api.dto.ResultResponse
import com.brainlab.common.ApiResponse
import com.brainlab.common.RequestUtils
import com.brainlab.domain.result.TestResultService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/results")
class ResultController(
    private val resultService: TestResultService
) {

    @PostMapping
    fun saveResult(
        @Valid @RequestBody request: ResultRequest,
        httpRequest: HttpServletRequest
    ): ApiResponse<ResultResponse> {
        val ip = RequestUtils.getClientIp(httpRequest)
        val result = resultService.saveResult(request, ip)
        return ApiResponse.ok(result)
    }

    @GetMapping("/{shareToken}")
    fun getResult(@PathVariable shareToken: String): ApiResponse<ResultResponse> {
        val result = resultService.getResult(shareToken)
        return ApiResponse.ok(result)
    }

    @GetMapping("/ranking")
    fun getRanking(): ApiResponse<RankingResponse> {
        val ranking = resultService.getRanking()
        return ApiResponse.ok(ranking)
    }
}
