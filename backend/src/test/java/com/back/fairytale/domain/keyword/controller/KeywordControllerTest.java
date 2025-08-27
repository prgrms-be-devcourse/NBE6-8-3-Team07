package com.back.fairytale.domain.keyword.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@Transactional
public class KeywordControllerTest {

    @Autowired
    private MockMvc mockMvc;


    @Test
    @DisplayName("키워드 전체 조회 API - 성공")
    void getAllKeywords() throws Exception {
        mockMvc.perform(get("/api/keywords")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        System.out.println("키워드 전체 조회 성공");
    }
    @Autowired
    private com.back.fairytale.domain.keyword.repository.KeywordRepository keywordRepository;

    @Test
    @DisplayName("키워드 단건(특정) 조회 API - 성공")
    void getKeywordById() throws Exception {
        //테스트용 키워드 저장
        com.back.fairytale.domain.keyword.entity.Keyword keyword = keywordRepository.save(
                com.back.fairytale.domain.keyword.entity.Keyword.builder()
                        .keyword("공주")
                        .keywordType(com.back.fairytale.domain.keyword.enums.KeywordType.CHARACTERS)
                        .usageCount(5)
                        .build()
        );

        //저장한 키워드의 id로 조회
        mockMvc.perform(get("/api/keywords/" + keyword.getKeywordId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        System.out.println("키워드 단건 조회 성공");
    }
}