import com.back.BackendApplication
import com.back.fairytale.domain.bookmark.entity.BookMark
import com.back.fairytale.domain.bookmark.exception.BookMarkAlreadyExistsException
import com.back.fairytale.domain.bookmark.exception.BookMarkNotFoundException
import com.back.fairytale.domain.bookmark.repository.BookMarkRepository
import com.back.fairytale.domain.bookmark.service.BookMarkService
import com.back.fairytale.domain.fairytale.entity.Fairytale
import com.back.fairytale.domain.fairytale.repository.FairytaleRepository
import com.back.fairytale.domain.user.entity.User
import com.back.fairytale.domain.user.repository.UserRepository
import com.back.fairytale.domain.user.service.AuthService
import com.back.fairytale.external.ai.client.GeminiClient
import com.back.fairytale.external.ai.client.HuggingFaceClient
import com.back.fairytale.global.security.jwt.JWTProvider
import com.back.fairytale.global.security.jwt.JWTUtil
import com.back.fairytale.global.util.impl.GoogleCloudStorage
import com.google.cloud.storage.Storage
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.Test

@SpringBootTest(classes = [BackendApplication::class])
@ActiveProfiles("test")
@Transactional
class BookMarkServiceIntegrationTest @Autowired constructor(
    private val bookMarkService: BookMarkService,
    private val bookMarkRepository: BookMarkRepository,
    private val userRepository: UserRepository,
    private val fairytaleRepository: FairytaleRepository
) {

    @MockkBean
    private lateinit var geminiClient: GeminiClient

    @MockkBean
    private lateinit var huggingFaceClient: HuggingFaceClient

    @MockkBean
    private lateinit var googleCloudStorage: GoogleCloudStorage

    @MockkBean
    private lateinit var storage: Storage

    @MockkBean
    private lateinit var authService: AuthService

    @MockkBean
    private lateinit var jwtProvider: JWTProvider

    @MockkBean
    private lateinit var jwtUtil: JWTUtil

    private lateinit var user: User
    private lateinit var fairytale: Fairytale

    @BeforeEach
    fun setUp() {
        bookMarkRepository.deleteAll()
        fairytaleRepository.deleteAll()
        userRepository.deleteAll()

        user = User(
            email = "test@naver.com",
            name = "홍길동",
            nickname = "길동",
            socialId = "1234"
        )
        userRepository.save(user)

        fairytale = Fairytale(
            user = user,
            title = "안녕",
            content = "반가워요",
            imageUrl = "www.naver.com"
        )
        fairytaleRepository.save(fairytale)
    }

    @Test
    @DisplayName("유저 북마크 목록 조회")
    fun getUserBookmarks() {
        val bookMark = BookMark(user = user, fairytale = fairytale)
        bookMarkRepository.save(bookMark)

        val result = bookMarkService.getBookMark(user.id!!)

        assertEquals(1, result.size)
        assertEquals(fairytale.id, result[0].fairytaleId)
    }

    @Test
    @DisplayName("북마크 추가")
    fun addBookmark() {
        val result = bookMarkService.addBookMark(fairytale.id!!, user.id!!)

        assertNotNull(result.id)
        assertEquals(user.id, result.user.id)
        assertEquals(fairytale.id, result.fairytale.id)
    }

    @Test
    @DisplayName("중복 북마크 추가 X")
    fun preventDuplicateBookmark() {
        bookMarkService.addBookMark(fairytale.id!!, user.id!!)

        assertThrows(BookMarkAlreadyExistsException::class.java) {
            bookMarkService.addBookMark(fairytale.id!!, user.id!!)
        }
    }

    @Test
    @DisplayName("북마크를 삭제")
    fun removeBookmark() {
        val bookMark = bookMarkService.addBookMark(fairytale.id!!, user.id!!)

        bookMarkService.removeBookMark(user.id!!, fairytale.id!!)

        assertFalse(bookMarkRepository.findById(bookMark.id!!).isPresent)
    }

    @Test
    @DisplayName("북마크되지 않은 동화를 삭제하면 예외가 발생")
    fun removeNonExistentBookmark() {
        assertThrows(BookMarkNotFoundException::class.java) {
            bookMarkService.removeBookMark(user.id!!, fairytale.id!!)
        }
    }

    @Test
    @DisplayName("북마크 여부를 확인")
    fun checkBookmarkExists() {
        bookMarkService.addBookMark(fairytale.id!!, user.id!!)

        val result = bookMarkService.isBookmarked(user.id!!, fairytale.id!!)

        assertTrue(result)
    }
}
