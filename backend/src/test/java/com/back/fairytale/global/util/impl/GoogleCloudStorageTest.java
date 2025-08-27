package com.back.fairytale.global.util.impl;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@ActiveProfiles("dev")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
public class GoogleCloudStorageTest {

    @Autowired
    private GoogleCloudStorage googleCloudStorage;

    @BeforeAll
    void setUp() {
        ImageIO.scanForPlugins();
    }

    private List<MultipartFile> generateMockImages(int count) throws IOException {
        List<MultipartFile> files = new ArrayList<>();
        byte[] imageBytes = new ClassPathResource("pexels-christian-heitz-285904-842711.jpg").getInputStream().readAllBytes();

        for (int i = 0; i < count; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file" + i,
                    "image_" + i + ".jpg",
                    "image/jpeg",
                    imageBytes
            );
            files.add(file);
        }
        return files;
    }
    
    @Test
    @DisplayName("4K 이미지 업로드 (3개)")
    void performanceTest_10Images() throws IOException {
        List<MultipartFile> files = generateMockImages(3);

        List<String> urls = googleCloudStorage.uploadImages(files);

        assertEquals(3, urls.size());
    }
}