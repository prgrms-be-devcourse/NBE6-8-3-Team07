package com.back.fairytale.domain.fairytale.controller;

import com.back.fairytale.domain.fairytale.dto.FairytaleDetailResponse;
import com.back.fairytale.domain.fairytale.dto.FairytaleListResponse;
import com.back.fairytale.domain.fairytale.dto.FairytaleResponse;
import com.back.fairytale.domain.fairytale.entity.Fairytale;
import com.back.fairytale.domain.fairytale.entity.FairytaleKeyword;
import com.back.fairytale.domain.fairytale.repository.FairytaleRepository;
import com.back.fairytale.domain.keyword.entity.Keyword;
import com.back.fairytale.domain.keyword.enums.KeywordType;
import com.back.fairytale.domain.keyword.repository.KeywordRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@Transactional
public class FairytaleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FairytaleRepository fairytaleRepository;

    @Autowired
    private KeywordRepository keywordRepository;

    // 동화 생성 메서드
    private FairytaleResponse createFairytale(String requestJson) throws Exception {
        MvcResult result = mockMvc.perform(post("/fairytales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        String responseBody = result.getResponse().getContentAsString();
        return objectMapper.readValue(responseBody, FairytaleResponse.class);
    }

    // 동화 생성만 하고 응답이 필요 없는 경우 메서드
    private void createFairytaleOnly(String requestJson) throws Exception {
        MvcResult result = mockMvc.perform(post("/fairytales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Gemini API를 통한 동화 생성 - 실제 API 호출")
    void t1() throws Exception {
        String requestJson = """
            {
                "childName": "지민",
                "childRole": "용감한 기사",
                "characters": "공주, 마법사",
                "place": "마법의 성",
                "lesson": "용기",
                "mood": "모험적인"
            }
            """;

        FairytaleResponse response = createFairytale(requestJson);

        assertThat(response.id()).isNotNull();
        assertThat(response.title()).isNotNull();
        assertThat(response.childName()).isEqualTo("지민");
        assertThat(response.childRole()).isEqualTo("용감한 기사");

        System.out.println("동화 생성 성공");
    }

    @Test
    @DisplayName("Validation 오류 테스트")
    void t2() throws Exception {
        // childName이 빈 값 (validation 실패)
        String invalidRequest = """
        {
            "childName": "",
            "childRole": "기사",
            "characters": "공주",
            "place": "성",
            "lesson": "용기",
            "mood": "모험"
        }
        """;

        MvcResult result = mockMvc.perform(post("/fairytales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andDo(print())
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("동화 전체 리스트 조회 - 성공")
    void t3() throws Exception {
        String requestJson = """
            {
                "childName": "지민",
                "childRole": "용감한 기사",
                "characters": "공주, 마법사",
                "place": "마법의 성",
                "lesson": "용기",
                "mood": "모험적인"
            }
            """;

        createFairytaleOnly(requestJson);

        MvcResult listResult = mockMvc.perform(get("/fairytales")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andReturn();

        assertThat(listResult.getResponse().getStatus()).isEqualTo(200);

        String responseBody = listResult.getResponse().getContentAsString();
        List<FairytaleListResponse> responseList = objectMapper.readValue(
                responseBody, new TypeReference<List<FairytaleListResponse>>() {}
        );

        assertThat(responseList).isNotEmpty();
        assertThat(responseList).hasSize(1);
        assertThat(responseList.get(0).id()).isNotNull();
        assertThat(responseList.get(0).title()).isNotNull();
        assertThat(responseList.get(0).createdAt()).isNotNull();

        System.out.println("사용자별 동화 전체 리스트 조회 성공");
    }

    @Test
    @DisplayName("동화 전체 리스트 조회 - 데이터 없음")
    void t4() throws Exception {
        MvcResult result = mockMvc.perform(get("/fairytales")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(404);
        assertThat(result.getResponse().getContentAsString()).isEqualTo("등록된 동화가 없습니다.");
    }

    @Test
    @DisplayName("동화 생성 시 키워드 저장 검증")
    void t5() throws Exception {
        String requestJson = """
            {
                "childName": "민수",
                "childRole": "마법사",
                "characters": "용, 공주, 왕자",
                "place": "마법의 숲, 높은 탑",
                "lesson": "용기와 지혜",
                "mood": "신비로운"
            }
            """;

        // 동화 생성
        FairytaleResponse response = createFairytale(requestJson);

        Optional<Fairytale> savedFairytale = fairytaleRepository.findById(response.id());
        assertThat(savedFairytale).isPresent();

        Fairytale fairytale = savedFairytale.get();

        // FairytaleKeyword를 통한 키워드 검증
        List<FairytaleKeyword> fairytaleKeywords = fairytale.getFairytaleKeywords();
        assertThat(fairytaleKeywords).isNotEmpty();

        assertThat(fairytaleKeywords)
                .extracting(fk -> fk.getKeyword().getKeywordType())
                .contains(
                        KeywordType.CHILD_NAME,
                        KeywordType.CHILD_ROLE,
                        KeywordType.CHARACTERS,
                        KeywordType.PLACE,
                        KeywordType.LESSON,
                        KeywordType.MOOD
                );

        // 아이이름 키워드 검증
        Optional<FairytaleKeyword> childNameKeyword = fairytaleKeywords.stream()
                .filter(fk -> fk.getKeyword().getKeywordType() == KeywordType.CHILD_NAME)
                .findFirst();
        assertThat(childNameKeyword).isPresent();
        assertThat(childNameKeyword.get().getKeyword().getKeyword()).isEqualTo("민수");

        // 등장인물 키워드 검증
        List<FairytaleKeyword> characterKeywords = fairytaleKeywords.stream()
                .filter(fk -> fk.getKeyword().getKeywordType() == KeywordType.CHARACTERS)
                .toList();
        assertThat(characterKeywords).hasSize(3);
        assertThat(characterKeywords)
                .extracting(fk -> fk.getKeyword().getKeyword())
                .containsExactlyInAnyOrder("용", "공주", "왕자");

        // 교훈 키워드 검증
        List<FairytaleKeyword> lessonKeywords = fairytaleKeywords.stream()
                .filter(fk -> fk.getKeyword().getKeywordType() == KeywordType.LESSON)
                .toList();
        assertThat(lessonKeywords).hasSize(1);
        assertThat(lessonKeywords.get(0).getKeyword().getKeyword()).isEqualTo("용기와 지혜");
    }

    @Test
    @DisplayName("같은 키워드 재사용 검증 - 중복 키워드는 새로 생성하지 않음")
    void t6() throws Exception {
        // 첫번째 동화 생성
        String firstRequestJson = """
            {
                "childName": "지민",
                "childRole": "기사",
                "characters": "공주",
                "place": "성",
                "lesson": "용기",
                "mood": "모험적인"
            }
            """;

        createFairytaleOnly(firstRequestJson);

        // 첫 번째 동화 생성 후 키워드 개수 확인
        long keywordCountAfterFirst = keywordRepository.count();

        // 동일한 키워드를 포함한 두 번째 동화 생성
        String secondRequestJson = """
            {
                "childName": "민수",
                "childRole": "기사",
                "characters": "공주",
                "place": "성",
                "lesson": "용기",
                "mood": "신비로운"
            }
            """;

        createFairytaleOnly(secondRequestJson);

        long keywordCountAfterSecond = keywordRepository.count();

        // 키워드 4개 중복되고 새로운 키워드는 2개니까 총 2개의 키워드만 추가되어야 함
        assertThat(keywordCountAfterSecond).isEqualTo(keywordCountAfterFirst + 2);

        // 전체에서 해당 키워드 개수 세기 -> 중복 저장인지 확인
        List<Keyword> allKeywords = keywordRepository.findAll();
        long countKeyword1 = allKeywords.stream()
                .filter(k -> k.getKeyword().equals("기사") && k.getKeywordType() == KeywordType.CHILD_ROLE)
                .count();
        assertThat(countKeyword1).isEqualTo(1); // 하나만 존재 (중복 저장 안되니까)

        long countKeyword2 = allKeywords.stream()
                .filter(k -> k.getKeyword().equals("공주") && k.getKeywordType() == KeywordType.CHARACTERS)
                .count();
        assertThat(countKeyword2).isEqualTo(1);
    }

    @Test
    @DisplayName("사용자별 동화 상세 조회 - 성공")
    void t7() throws Exception {
        String requestJson = """
        {
            "childName": "하늘",
            "childRole": "마법사",
            "characters": "용, 요정",
            "place": "마법의 숲",
            "lesson": "용기",
            "mood": "환상적인"
        }
        """;

        FairytaleResponse response = createFairytale(requestJson);
        Long fairytaleId = response.id();

        MvcResult detailResult = mockMvc.perform(get("/fairytales/" + fairytaleId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andReturn();

        assertThat(detailResult.getResponse().getStatus()).isEqualTo(200);

        String detailResponseBody = detailResult.getResponse().getContentAsString();
        FairytaleDetailResponse detailResponse = objectMapper.readValue(detailResponseBody, FairytaleDetailResponse.class);

        assertThat(detailResponse.id()).isEqualTo(fairytaleId);
        assertThat(detailResponse.title()).isNotNull();
        assertThat(detailResponse.content()).isNotNull();
        assertThat(detailResponse.childName()).isEqualTo("하늘");
        assertThat(detailResponse.childRole()).isEqualTo("마법사");
        assertThat(detailResponse.characters()).contains("용", "요정");
        assertThat(detailResponse.place()).isEqualTo("마법의 숲");
        assertThat(detailResponse.lesson()).isEqualTo("용기");
        assertThat(detailResponse.mood()).isEqualTo("환상적인");
        assertThat(detailResponse.createdAt()).isNotNull();

        System.out.println("사용자별 동화 상세 조회 성공");
    }

    @Test
    @DisplayName("동화 삭제 - 성공")
    void t10() throws Exception {
        String requestJson = """
        {
            "childName": "하늘",
            "childRole": "마법사",
            "characters": "용, 요정",
            "place": "마법의 숲",
            "lesson": "용기",
            "mood": "환상적인"
        }
        """;

        FairytaleResponse response = createFairytale(requestJson);
        Long fairytaleId = response.id();

        MvcResult deleteResult = mockMvc.perform(delete("/fairytales/" + fairytaleId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andReturn();

        // 삭제 성공 확인
        assertThat(deleteResult.getResponse().getStatus()).isEqualTo(204);

        // 삭제 후 존재하지 않는지 확인
        assertThat(fairytaleRepository.findById(fairytaleId)).isEmpty();

        System.out.println("동화 삭제 성공");
    }

    @Test
    @DisplayName("동화 생성과 이미지 생성 통합 테스트")
    void t11() throws Exception {
        String requestJson = """
        {
            "childName": "소라",
            "childRole": "바다 공주",
            "characters": "돌고래, 거북이",
            "place": "바다",
            "lesson": "친구의 소중함",
            "mood": "따듯한, 신비로운"
        }
        """;

        FairytaleResponse response = createFairytale(requestJson);

        // 동화 생성 확인
        assertThat(response.id()).isNotNull();
        assertThat(response.title()).isNotNull();
        assertThat(response.content()).isNotNull();
        assertThat(response.childName()).isEqualTo("소라");

        // 이미지 URL 확인 (현재는 null)
        System.out.println("생성된 동화 ID: " + response.id());
        System.out.println("이미지 URL: " + response.imageUrl()); // 현재는 null

        System.out.println("통합 테스트 - 동화 생성 성공!");
    }
}
