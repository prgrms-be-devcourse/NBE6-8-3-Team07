package com.back.fairytale.domain.keyword.enums;

public enum KeywordType {
    CHILD_NAME("아이이름"),
    CHILD_ROLE("아이역할"),
    CHARACTERS("캐릭터들"),
    PLACE("장소"),
    MOOD("분위기"),
    LESSON("교훈");

    private final String koreanName;  // 한글명을 저장할 필드

    KeywordType(String koreanName) {
        this.koreanName = koreanName;
    }

    public String getKoreanName() {   // 한글명 반환 메서드
        return koreanName;
    }

    public String getEnglishName() {  // 영어명 반환 메서드
        return this.name();
    }
}