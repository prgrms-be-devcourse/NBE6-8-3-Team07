package com.back.fairytale.global.util.impl

import com.back.BackendApplication
import com.back.fairytale.domain.user.service.AuthService
import com.back.fairytale.external.ai.client.GeminiClient
import com.back.fairytale.external.ai.client.HuggingFaceClient
import com.back.fairytale.global.security.jwt.JWTProvider
import com.back.fairytale.global.security.jwt.JWTUtil
import com.google.cloud.storage.Blob
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import java.io.IOException
import org.springframework.test.util.ReflectionTestUtils


@SpringBootTest(classes = [BackendApplication::class])
@ActiveProfiles("test")
class GoogleCloudStorageTest {

    @Autowired
    private lateinit var googleCloudStorage: GoogleCloudStorage

    @MockkBean
    private lateinit var geminiClient: GeminiClient

    @MockkBean
    private lateinit var huggingFaceClient: HuggingFaceClient

    @MockkBean
    private lateinit var authService: AuthService

    @MockkBean
    private lateinit var jwtProvider: JWTProvider

    @MockkBean
    private lateinit var jwtUtil: JWTUtil

    @MockkBean
    private lateinit var storage: Storage

    private val bucketName = "test-bucket"

    private lateinit var sampleImageBytes: ByteArray

    @BeforeEach
    fun setUp() {
        ReflectionTestUtils.setField(googleCloudStorage, "bucketName", bucketName)

        val inputStream = this::class.java.getResourceAsStream("/pexels-christian-heitz-285904-842711.jpg")
        sampleImageBytes = inputStream.readAllBytes()
    }

    @Test
    @DisplayName("이미지 업로드 ")
    fun uploadImages() {
        val files = listOf(
            MockMultipartFile("file1", "test1.jpg", "image/jpeg", sampleImageBytes),
            MockMultipartFile("file2", "test2.jpg", "image/jpeg", sampleImageBytes)
        )
        val blob = mockk<Blob>()

        every { storage.create(any<BlobInfo>(), any<ByteArray>()) } returns blob
        every { blob.name } returns "mocked-uuid"
        every { blob.blobId.name } returns "mocked-uuid"

        val imageUrls = googleCloudStorage.uploadImages(files)

        assertEquals(2, imageUrls.size)
        verify(exactly = 2) { storage.create(any<BlobInfo>(), any<ByteArray>()) }
    }

    @Test
    @DisplayName("이미지 삭제")
    fun deleteImages() {
        val imageUrls = listOf(
            "https://storage.googleapis.com/$bucketName/test1.jpg",
            "https://storage.googleapis.com/$bucketName/test2.jpg"
        )

        every { storage.delete(any<BlobId>()) } returns true

        googleCloudStorage.deleteImages(imageUrls)

        verify(exactly = 2) { storage.delete(any<BlobId>()) }
    }

    @Test
    @DisplayName("이미지 삭제 실패")
    fun deleteImagesFail() {
        val imageUrls = listOf("https://storage.googleapis.com/$bucketName/test1.jpg")

        every { storage.delete(any<BlobId>()) } returns false

        assertThrows(RuntimeException::class.java) {
            googleCloudStorage.deleteImages(imageUrls)
        }
        verify(exactly = 1) { storage.delete(any<BlobId>()) }
    }

    @Test
    @DisplayName("이미지 업데이트")
    fun updateImages() {
        val oldImageUrls = listOf("https://storage.googleapis.com/$bucketName/old.jpg")
        val newFiles = listOf(MockMultipartFile("newfile", "new.jpg", "image/jpeg", sampleImageBytes))
        val blob = mockk<Blob>()

        every { storage.delete(any<BlobId>()) } returns true
        every { storage.create(any<BlobInfo>(), any<ByteArray>()) } returns blob
        every { blob.name } returns "mocked-uuid"
        every { blob.blobId.name } returns "mocked-uuid"

        googleCloudStorage.updateImages(oldImageUrls, newFiles)

        verify(exactly = 1) { storage.delete(any<BlobId>()) }
        verify(exactly = 1) { storage.create(any<BlobInfo>(), any<ByteArray>()) }
    }

    @Test
    @DisplayName("이미지 업로드 IO 예외")
    fun uploadImageIOException() {
        val files = listOf(MockMultipartFile("file", "test.jpg", "image/jpeg", sampleImageBytes))

        every { storage.create(any<BlobInfo>(), any<ByteArray>()) } throws IOException("Test Exception")

        assertThrows(RuntimeException::class.java) {
            googleCloudStorage.uploadImages(files)
        }
    }
}
