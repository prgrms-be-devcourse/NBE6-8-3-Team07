package com.back.fairytale.domain.keyword.service;

import com.back.fairytale.domain.keyword.dto.KeywordResponseDto;
import com.back.fairytale.domain.keyword.entity.Keyword;
import com.back.fairytale.domain.keyword.enums.KeywordType;
import com.back.fairytale.domain.keyword.repository.KeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KeywordService {

    private final KeywordRepository keywordRepository;

    // 모든 키워드 조회
    @Transactional(readOnly = true)
    public List<KeywordResponseDto> getAllKeywords() {
        return keywordRepository.findAll().stream()
                .map(KeywordResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 타입별 키워드 조회 (Enum 타입으로 받음)
    @Transactional(readOnly = true)
    public List<KeywordResponseDto> getKeywordsByType(KeywordType keywordType) {
        return keywordRepository.findByKeywordType(keywordType).stream()
                .map(KeywordResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 단건(특정 키워드) 조회
    @Transactional(readOnly = true)
    public KeywordResponseDto getKeywordById(Long id) {
        Keyword keyword = keywordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("키워드가 존재하지 않습니다."));
        return KeywordResponseDto.fromEntity(keyword);
    }

    // 키워드 유효성 검사
    public void validateKeyword(String keyword) {
        // 욕설 필터링
        List<String> badWords = Arrays.asList("욕설1", "욕설2", "부적절한단어");
        if (badWords.stream().anyMatch(bad -> keyword.toLowerCase().contains(bad))) {
            throw new IllegalArgumentException("부적절한 키워드입니다.");
        }
    }

    @Transactional
    public void incrementUsageCountWithOptimisticLock(Long keywordId) {
        Keyword keyword = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new IllegalArgumentException("키워드가 존재하지 않습니다."));

        keyword.incrementUsageCount(); // usage_count 1 증가
        keywordRepository.save(keyword); // @Version 필드로 동시성 체크
    }
}
