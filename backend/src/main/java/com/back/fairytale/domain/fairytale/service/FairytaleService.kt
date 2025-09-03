package com.back.fairytale.domain.fairytale.service

import com.back.fairytale.domain.fairytale.dto.*
import com.back.fairytale.domain.fairytale.entity.Fairytale
import com.back.fairytale.domain.fairytale.exception.FairytaleNotFoundException
import com.back.fairytale.domain.fairytale.exception.UserNotFoundException
import com.back.fairytale.domain.fairytale.repository.FairytaleRepository
import com.back.fairytale.domain.keyword.entity.Keyword
import com.back.fairytale.domain.keyword.enums.KeywordType
import com.back.fairytale.domain.keyword.repository.KeywordRepository
import com.back.fairytale.domain.user.repository.UserRepository
import com.back.fairytale.external.ai.client.GeminiClient
import com.back.fairytale.external.ai.client.HuggingFaceClient
import com.back.fairytale.global.util.impl.GoogleCloudStorage
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class FairytaleService(
    private val fairytaleRepository: FairytaleRepository,
    private val keywordRepository: KeywordRepository,
    private val userRepository: UserRepository,
    private val geminiClient: GeminiClient,
    private val huggingFaceClient: HuggingFaceClient,
    private val googleCloudStorage: GoogleCloudStorage
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // 동화 전체 조회
    @Transactional(readOnly = true)
    fun getAllFairytalesByUserId(userId: Long): List<FairytaleListResponse> {
        val fairytales = fairytaleRepository.findAllByUserIdOrderByCreatedAtDesc(userId)

        if (fairytales.isEmpty()) {
            throw FairytaleNotFoundException("등록된 동화가 없습니다.")
        }

        log.info("동화 전체 조회 - 총 {}개의 동화를 조회했습니다.", fairytales.size)
        return fairytales.map { FairytaleListResponse(it) }
    }

    // 동화 상세 조회
    @Transactional(readOnly = true)
    fun getFairytaleByIdAndUserId(fairytaleId: Long, userId: Long): FairytaleDetailResponse {
        val fairytale = fairytaleRepository.findByIdAndUserIdWithKeywordsFetch(fairytaleId, userId)
            ?: throw FairytaleNotFoundException("동화를 찾을 수 없거나 접근 권한이 없습니다. ID: $fairytaleId")

        log.info("동화 상세 조회 - ID: {}, 제목: {}", fairytale.id, fairytale.title)
        return FairytaleDetailResponse(fairytale)
    }

    // 동화 삭제
    fun deleteFairytaleByIdAndUserId(fairytaleId: Long, userId: Long) {
        val fairytale = fairytaleRepository.findByIdAndUserIdWithKeywordsFetch(fairytaleId, userId)
            ?: throw FairytaleNotFoundException("동화를 찾을 수 없거나 삭제 권한이 없습니다. ID: $fairytaleId")

        // 이미지가 있으면 Google Cloud Storage에서 삭제
        fairytale.imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
            try {
                val fileName = extractFileNameFromUrl(url)
                googleCloudStorage.deleteImages(listOf(url))
                log.info("이미지 삭제 완료 - 파일명: {}", fileName)
            } catch (e: Exception) {
                log.error("이미지 삭제 실패 - URL: {}, 에러: {}", url, e.message)
                // 이미지 삭제 실패해도 동화는 삭제되도록 계속 진행
            }
        }

        fairytaleRepository.delete(fairytale)
        log.info("동화 삭제 완료 - ID: {}", fairytaleId)
    }

    // 동화 생성
    fun createFairytale(request: FairytaleCreateRequest, userId: Long): FairytaleResponse {
        // 프롬프트 생성 및 Gemini 호출
        val prompt = buildPrompt(request)
        val generatedContent = geminiClient.generateFairytale(prompt)

        // 제목/내용 분리
        val (title, content) = extractTitleAndContent(generatedContent)

        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("사용자를 찾을 수 없습니다. ID: $userId") }

        // 이미지 생성
        var imageUrl: String? = null
        try {
            val imagePrompt = buildImagePrompt(request)
            val imageData: ByteArray = huggingFaceClient.generateImage(imagePrompt)
            imageUrl = googleCloudStorage.uploadImageBytesToCloud(imageData)
        } catch (e: Exception) {
            log.error("이미지 생성 실패, 동화만 저장합니다.", e)
            // 이미지 생성 실패해도 동화는 저장
        }

        // 동화 저장 (Kotlin 엔티티 생성자 사용)
        val fairytale = Fairytale(
            user = user,
            title = title,
            content = content,
            imageUrl = imageUrl,
            isPublic = false
        )
        val saved = fairytaleRepository.save(fairytale)

        // 키워드 저장
        saveKeyword(saved, request.childName, KeywordType.CHILD_NAME)
        saveKeyword(saved, request.childRole, KeywordType.CHILD_ROLE)
        saveKeywords(saved, request.characters, KeywordType.CHARACTERS)
        saveKeywords(saved, request.place, KeywordType.PLACE)
        saveKeywords(saved, request.lesson, KeywordType.LESSON)
        saveKeywords(saved, request.mood, KeywordType.MOOD)

        log.info("동화 생성 완료 - ID: {}, 제목: {}, 사용자: {}", saved.id, title, user.name)
        return FairytaleResponse(saved)
    }

    // 갤러리에서 공개 동화 조회 (페이징 포함)
    @Transactional(readOnly = true)
    fun getPublicFairytalesForGallery(pageable: Pageable): Page<FairytalePublicListResponse> {
        val publicFairytales = fairytaleRepository.findPublicFairytalesForGallery(pageable)

        log.info("갤러리 공개 동화 조회 - 총 {}개의 동화를 조회했습니다.", publicFairytales.totalElements)
        return publicFairytales.map { FairytalePublicListResponse(it) }
    }

    // 특정 사용자의 공개 동화 조회
    @Transactional(readOnly = true)
    fun getPublicFairytalesByUserId(userId: Long): List<FairytalePublicListResponse> {
        val publicFairytales = fairytaleRepository.findPublicFairytalesByUserId(userId)

        if (publicFairytales.isEmpty()) {
            throw FairytaleNotFoundException("해당 사용자의 공개 동화가 없습니다.")
        }

        log.info("사용자 공개 동화 조회 - 사용자 ID: {}, 총 {}개의 동화를 조회했습니다.", userId, publicFairytales.size)
        return publicFairytales.map { FairytalePublicListResponse(it) }
    }

    // 동화 공개/비공개 설정
    fun updateFairytaleVisibility(fairytaleId: Long, userId: Long, isPublic: Boolean) {
        val fairytale = fairytaleRepository.findByIdAndUserIdWithKeywordsFetch(fairytaleId, userId)
            ?: throw FairytaleNotFoundException("동화를 찾을 수 없거나 수정 권한이 없습니다. ID: $fairytaleId")

        fairytale.isPublic = isPublic
        fairytaleRepository.save(fairytale)

        log.info("동화 공개 설정 변경 - ID: {}, 공개여부: {}", fairytaleId, isPublic)
    }

    // 공개 동화 상세 조회 (갤러리용)
    @Transactional(readOnly = true)
    fun getPublicFairytaleById(fairytaleId: Long): FairytaleDetailResponse {
        val fairytale = fairytaleRepository.findByIdWithKeywordsFetch(fairytaleId)
            ?: throw FairytaleNotFoundException("동화를 찾을 수 없습니다. ID: $fairytaleId")

        if (!fairytale.isPublic) {
            throw FairytaleNotFoundException("비공개 동화입니다. ID: $fairytaleId")
        }

        log.info("공개 동화 상세 조회 - ID: {}, 제목: {}", fairytale.id, fairytale.title)
        return FairytaleDetailResponse(fairytale)
    }

    private fun buildImagePrompt(request: FairytaleCreateRequest): String {
        val prompt = buildString {
            append(request.place).append("에서 ")
            append("어리고 귀여운 ").append(request.childRole).append("이(가) ")
            append("어리고 귀여운 ").append(request.characters).append("과(와) 함께 ")
            append("웃는 장면, ")
            append(request.mood).append(" 분위기, ")
            append("어린이 동화책 삽화 스타일, 따뜻한 색감, 어린이를 위한,")
            append("1:1 비율, 정사각형 이미지")
        }

        return try {
            val translatePrompt =
                "다음 한국어 문장을 영어로 번역해주세요. 이미지 생성용 프롬프트이므로 자연스럽고 정확하게 번역해주세요. 번역 결과만 답변해주세요:\n\n$prompt"
            val englishPrompt = geminiClient.generateFairytale(translatePrompt)
            log.info("이미지 프롬프트 번역 완료:\n한글: {}\n영어: {}", prompt, englishPrompt)
            englishPrompt
        } catch (e: Exception) {
            log.error("프롬프트 번역 실패, 한글 프롬프트 사용", e)
            prompt // 실패시 한글 그대로 사용
        }
    }

    private fun saveKeyword(fairytale: Fairytale, keywordValue: String?, type: KeywordType) {
        if (!keywordValue.isNullOrBlank()) {
            val keyword = keywordRepository.findByKeywordAndKeywordType(keywordValue.trim(), type)
                .orElseGet { keywordRepository.save(Keyword.of(keywordValue.trim(), type)) }

            keyword.incrementUsageCount()
            keywordRepository.save(keyword)

            fairytale.addKeyword(keyword)
        }
    }

    private fun saveKeywords(fairytale: Fairytale, input: String?, type: KeywordType) {
        if (!input.isNullOrBlank()) {
            input.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { value ->
                    val keyword = keywordRepository.findByKeywordAndKeywordType(value, type)
                        .orElseGet { keywordRepository.save(Keyword.of(value, type)) }

                    keyword.incrementUsageCount()
                    keywordRepository.save(keyword)

                    fairytale.addKeyword(keyword)
                }
        }
    }

    private fun buildPrompt(request: FairytaleCreateRequest): String = buildString {
        append("어린이를 위한 동화를 만들어주세요.\n")
        append("응답 형식: [제목: 동화제목] 다음 줄부터 동화 내용\n\n")
        append("조건:\n")
        append("- 아이 이름: ").append(request.childName).append("\n")
        append("- 아이 역할: ").append(request.childRole).append("\n")
        append("- 등장인물: ").append(request.characters).append("\n")
        append("- 장소: ").append(request.place).append("\n")
        append("- 교훈: ").append(request.lesson).append("\n")
        append("- 분위기: ").append(request.mood).append("\n\n")
        append(request.childName).append("이(가) ").append(request.childRole)
            .append(" 역할로 나오는 900-1300자 정도의 완성된 동화를 만들어주세요.")
    }

    private fun extractTitleAndContent(generatedContent: String): Pair<String, String> {
        var title = "제목 없음"
        var content = generatedContent

        if (generatedContent.contains("[제목:") && generatedContent.contains("]")) {
            val titleStart = generatedContent.indexOf("[제목:") + 4
            val titleEnd = generatedContent.indexOf("]", startIndex = titleStart)
            if (titleEnd > titleStart) {
                title = generatedContent.substring(titleStart, titleEnd).trim()
                content = generatedContent.substring(titleEnd + 1).trim()
            }
        }
        return title to content
    }

    // 이미지 URL에서 파일명 추출
    private fun extractFileNameFromUrl(imageUrl: String): String =
        imageUrl.split("/").last()
}