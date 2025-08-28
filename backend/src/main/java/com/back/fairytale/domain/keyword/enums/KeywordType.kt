package com.back.fairytale.domain.keyword.enums

enum class KeywordType(val koreanName: String) {
    CHILD_NAME("아이이름"),
    CHILD_ROLE("아이역할"),
    CHARACTERS("캐릭터들"),
    PLACE("장소"),
    MOOD("분위기"),
    LESSON("교훈");

    val englishName: String
        get() = this.name
}
