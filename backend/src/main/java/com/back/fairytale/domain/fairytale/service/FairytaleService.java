package com.back.fairytale.domain.fairytale.service;

import com.back.fairytale.domain.fairytale.dto.*;
import com.back.fairytale.domain.fairytale.entity.Fairytale;
import com.back.fairytale.domain.fairytale.exception.FairytaleNotFoundException;
import com.back.fairytale.domain.fairytale.exception.UserNotFoundException;
import com.back.fairytale.domain.fairytale.repository.FairytaleRepository;
import com.back.fairytale.domain.keyword.entity.Keyword;
import com.back.fairytale.domain.keyword.enums.KeywordType;
import com.back.fairytale.domain.keyword.repository.KeywordRepository;
import com.back.fairytale.domain.user.entity.User;
import com.back.fairytale.domain.user.repository.UserRepository;
import com.back.fairytale.external.ai.client.GeminiClient;
import com.back.fairytale.external.ai.client.HuggingFaceClient;
import com.back.fairytale.global.util.impl.GoogleCloudStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FairytaleService {

    private final FairytaleRepository fairytaleRepository;
    private final KeywordRepository keywordRepository;
    private final UserRepository userRepository;
    private final GeminiClient geminiClient;
    private final HuggingFaceClient huggingFaceClient;

    @Autowired
    private final GoogleCloudStorage googleCloudStorage;

    // 동화 전체 조회
    @Transactional(readOnly = true)
    public List<FairytaleListResponse>getAllFairytalesByUserId(Long userId) {
        List<Fairytale> fairytales = fairytaleRepository.findAllByUserIdOrderByCreatedAtDesc(userId);

        if (fairytales.isEmpty()) {
            throw new FairytaleNotFoundException("등록된 동화가 없습니다.");
        }

        log.info("동화 전체 조회 - 총 {}개의 동화를 조회했습니다.", fairytales.size());

        return fairytales.stream()
                .map(FairytaleListResponse::from)
                .collect(Collectors.toList());
    }

    // 동화 상세 조회
    @Transactional(readOnly = true)
    public FairytaleDetailResponse getFairytaleByIdAndUserId(Long fairytaleId, Long userId) {
        Fairytale fairytale = fairytaleRepository.findByIdAndUserIdWithKeywordsFetch(fairytaleId, userId)
                .orElseThrow(() -> new FairytaleNotFoundException("동화를 찾을 수 없거나 접근 권한이 없습니다. ID: " + fairytaleId));

        log.info("동화 상세 조회 - ID: {}, 제목: {}", fairytale.getId(), fairytale.getTitle());

        return FairytaleDetailResponse.from(fairytale);
    }

    // 동화 삭제
    public void deleteFairytaleByIdAndUserId(Long fairytaleId, Long userId) {
        Fairytale fairytale = fairytaleRepository.findByIdAndUserIdWithKeywordsFetch(fairytaleId, userId)
                .orElseThrow(() -> new FairytaleNotFoundException("동화를 찾을 수 없거나 삭제 권한이 없습니다. ID: " + fairytaleId));

        // 이미지가 있으면 Google Cloud Storage에서 삭제
        if (fairytale.getImageUrl() != null && !fairytale.getImageUrl().isEmpty()) {
            try {
                String fileName = extractFileNameFromUrl(fairytale.getImageUrl());
                googleCloudStorage.deleteImages(List.of(fairytale.getImageUrl()));
                log.info("이미지 삭제 완료 - 파일명: {}", fileName);
            } catch (Exception e) {
                log.error("이미지 삭제 실패 - URL: {}, 에러: {}", fairytale.getImageUrl(), e.getMessage());
                // 이미지 삭제 실패해도 동화는 삭제되도록
            }
        }

        fairytaleRepository.delete(fairytale);

        log.info("동화 삭제 완료 - ID: {}", fairytaleId);
    }

    // 동화 생성
    public FairytaleResponse createFairytale(FairytaleCreateRequest request, Long userId) {
        // 프롬프트 생성
        String prompt = buildPrompt(request);

        // Gemini API 호출
        String generatedContent = geminiClient.generateFairytale(prompt);

        // 제목과 내용 분리
        String[] titleAndContent = extractTitleAndContent(generatedContent);
        String title = titleAndContent[0];
        String content = titleAndContent[1];

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));

        // 이미지 생성
        String imageUrl = null;
        try {
            String imagePrompt = buildImagePrompt(request);
            byte[] imageData = huggingFaceClient.generateImage(imagePrompt);

            //String fileName = "fairytale_" + System.currentTimeMillis() + ".png";
            imageUrl = googleCloudStorage.uploadImageBytesToCloud(imageData);

        } catch (Exception e) {
            log.error("이미지 생성 실패, 동화만 저장합니다.", e);
            // 이미지 생성 실패해도 동화는 저장
        }

        // 동화 저장
        Fairytale fairytale = Fairytale.builder()
                .user(user)
                .title(title)
                .content(content)
                .imageUrl(imageUrl)
                .isPublic(false)
                .build();

        Fairytale savedFairytale = fairytaleRepository.save(fairytale);

        // 키워드 저장
        saveKeyword(savedFairytale, request.childName(), KeywordType.CHILD_NAME);
        saveKeyword(savedFairytale, request.childRole(), KeywordType.CHILD_ROLE);
        saveKeywords(savedFairytale, request.characters(), KeywordType.CHARACTERS);
        saveKeywords(savedFairytale, request.place(), KeywordType.PLACE);
        saveKeywords(savedFairytale, request.lesson(), KeywordType.LESSON);
        saveKeywords(savedFairytale, request.mood(), KeywordType.MOOD);

        log.info("동화 생성 완료 - ID: {}, 제목: {}, 사용자: {}", savedFairytale.getId(), title, user.getName());

        return FairytaleResponse.from(savedFairytale);
    }

    // 이미지 프롬프트 생성
    private String buildImagePrompt(FairytaleCreateRequest request) {
        StringBuilder prompt = new StringBuilder();

        prompt.append(request.place()).append("에서 ");
        prompt.append("어리고 귀여운 ").append(request.childRole()).append("이(가) ");
        prompt.append("어리고 귀여운 ").append(request.characters()).append("과(와) 함께 ");
        prompt.append("웃는 장면, ");
        prompt.append(request.mood()).append(" 분위기, ");
        prompt.append("어린이 동화책 삽화 스타일, 따뜻한 색감, 어린이를 위한,");
        prompt.append("1:1 비율, 정사각형 이미지");

        // Gemini에게 번역 요청 (이미지 생성 모델이 한국어를 인식 못하는듯 하다.. 그래서 번역!)
        try {
            String translatePrompt = "다음 한국어 문장을 영어로 번역해주세요. 이미지 생성용 프롬프트이므로 자연스럽고 정확하게 번역해주세요. 번역 결과만 답변해주세요:\n\n" + prompt.toString();

            String englishPrompt = geminiClient.generateFairytale(translatePrompt);

            log.info("이미지 프롬프트 번역 완료:");
            log.info("한글: {}", prompt.toString());
            log.info("영어: {}", englishPrompt);

            return englishPrompt;

        } catch (Exception e) {
            log.error("프롬프트 번역 실패, 한글 프롬프트 사용", e);
            return prompt.toString(); // 실패시 한글 그대로 사용
        }
    }

    // 단일 키워드 저장
    private void saveKeyword(Fairytale fairytale, String keywordValue, KeywordType type) {
        if (keywordValue != null && !keywordValue.trim().isEmpty()) {
            Keyword keyword = keywordRepository.findByKeywordAndKeywordType(keywordValue.trim(), type)
                    .orElseGet(() -> keywordRepository.save(
                            Keyword.of(keywordValue.trim(), type)));

            // usage_count 증가
            keyword.incrementUsageCount();
            keywordRepository.save(keyword);

            fairytale.addKeyword(keyword);
        }
    }

    // 다중 키워드 저장
    private void saveKeywords(Fairytale fairytale, String input, KeywordType type) {
        if (input != null && !input.trim().isEmpty()) {
            String[] keywords = input.split(",");
            for (String keywordValue : keywords) {
                String trimmedKeyword = keywordValue.trim();
                if (!trimmedKeyword.isEmpty()) {
                    Keyword keyword = keywordRepository.findByKeywordAndKeywordType(trimmedKeyword, type)
                            .orElseGet(() -> keywordRepository.save(
                                    Keyword.of(trimmedKeyword, type)));

                    // usage_count 증가
                    keyword.incrementUsageCount();
                    keywordRepository.save(keyword);

                    fairytale.addKeyword(keyword);
                }
            }
        }
    }

    private String buildPrompt(FairytaleCreateRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("어린이를 위한 동화를 만들어주세요.\n");
        prompt.append("응답 형식: [제목: 동화제목] 다음 줄부터 동화 내용\n\n");
        prompt.append("조건:\n");
        prompt.append("- 아이 이름: ").append(request.childName()).append("\n");
        prompt.append("- 아이 역할: ").append(request.childRole()).append("\n");
        prompt.append("- 등장인물: ").append(request.characters()).append("\n");
        prompt.append("- 장소: ").append(request.place()).append("\n");
        prompt.append("- 교훈: ").append(request.lesson()).append("\n");
        prompt.append("- 분위기: ").append(request.mood()).append("\n");

        prompt.append("\n").append(request.childName()).append("이(가) ").append(request.childRole()).append(" 역할로 나오는 ");
        prompt.append("900-1300자 정도의 완성된 동화를 만들어주세요.");

        return prompt.toString();
    }

    private String[] extractTitleAndContent(String generatedContent) {
        String title = "제목 없음";
        String content = generatedContent;

        if (generatedContent.contains("[제목:") && generatedContent.contains("]")) {
            int titleStart = generatedContent.indexOf("[제목:") + 4;
            int titleEnd = generatedContent.indexOf("]", titleStart);
            if (titleEnd > titleStart) {
                title = generatedContent.substring(titleStart, titleEnd).trim();
                content = generatedContent.substring(titleEnd + 1).trim();
            }
        }

        return new String[]{title, content};
    }

    // 이미지 URL에서 파일명 추출
    private String extractFileNameFromUrl(String imageUrl) {
        String[] parts = imageUrl.split("/");
        return parts[parts.length - 1]; // 마지막 부분이 파일명
    }

    // 갤러리에서 공개 동화 조회 (페이징 포함)
    @Transactional(readOnly = true)
    public Page<FairytalePublicListResponse> getPublicFairytalesForGallery(Pageable pageable) {
        Page<Fairytale> publicFairytales = fairytaleRepository.findPublicFairytalesForGallery(pageable);

        if (publicFairytales.getTotalElements() == 0) {
            throw new FairytaleNotFoundException("공개된 동화가 없습니다.");
        }

        log.info("갤러리 공개 동화 조회 - 총 {}개의 동화를 조회했습니다.", publicFairytales.getTotalElements());

        return publicFairytales.map(FairytalePublicListResponse::from);
    }

    // 특정 사용자의 공개 동화 조회
    @Transactional(readOnly = true)
    public List<FairytalePublicListResponse> getPublicFairytalesByUserId(Long userId) {
        List<Fairytale> publicFairytales = fairytaleRepository.findPublicFairytalesByUserId(userId);

        if (publicFairytales.isEmpty()) {
            throw new FairytaleNotFoundException("해당 사용자의 공개 동화가 없습니다.");
        }

        log.info("사용자 공개 동화 조회 - 사용자 ID: {}, 총 {}개의 동화를 조회했습니다.", userId, publicFairytales.size());

        return publicFairytales.stream()
                .map(FairytalePublicListResponse::from)
                .collect(Collectors.toList());
    }

    // 동화 공개/비공개 설정
    public void updateFairytaleVisibility(Long fairytaleId, Long userId, Boolean isPublic) {
        Fairytale fairytale = fairytaleRepository.findByIdAndUserIdWithKeywordsFetch(fairytaleId, userId)
                .orElseThrow(() -> new FairytaleNotFoundException("동화를 찾을 수 없거나 수정 권한이 없습니다. ID: " + fairytaleId));

        fairytale.setPublic(isPublic);
        fairytaleRepository.save(fairytale);

        log.info("동화 공개 설정 변경 - ID: {}, 공개여부: {}", fairytaleId, isPublic);
    }

    // 공개 동화 상세 조회 (갤러리용)
    @Transactional(readOnly = true)
    public FairytaleDetailResponse getPublicFairytaleById(Long fairytaleId) {
        Fairytale fairytale = fairytaleRepository.findByIdWithKeywordsFetch(fairytaleId)
                .orElseThrow(() -> new FairytaleNotFoundException("동화를 찾을 수 없습니다. ID: " + fairytaleId));

        if (!fairytale.getIsPublic()) {
            throw new FairytaleNotFoundException("비공개 동화입니다. ID: " + fairytaleId);
        }

        log.info("공개 동화 상세 조회 - ID: {}, 제목: {}", fairytale.getId(), fairytale.getTitle());

        return FairytaleDetailResponse.from(fairytale);
    }
}