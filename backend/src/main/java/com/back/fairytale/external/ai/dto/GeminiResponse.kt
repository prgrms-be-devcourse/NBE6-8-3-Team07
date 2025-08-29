package com.back.fairytale.external.ai.dto

// Gemini API로부터 받은 응답 JSON을 Java 객체로 파싱하기 위한 구조
data class GeminiResponse(
    val candidates: List<Candidate> = emptyList()
) {
    data class Candidate(
        val content: Content? = null
    )

    data class Content(
        val parts: List<Part> = emptyList()
    )

    data class Part(
        val text: String? = null
    )

    fun getGeneratedText(): String =
        candidates.firstOrNull()
            ?.content
            ?.parts?.firstOrNull()
            ?.text
            ?: ""
}