package com.back.fairytale.external.ai.dto

// Gemini에게 보낼 메시지를 JSON 형식으로 만들기 위한 구조
data class GeminiRequest(
    val contents: List<Content>
) {
    data class Content(
        val parts: List<Part>
    )

    data class Part(
        val text: String
    )

    companion object {
        fun of(prompt: String): GeminiRequest =
            GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt)
                        )
                    )
                )
            )
    }
}
