package io.github.ddogga.blanken.repository

import io.github.ddogga.blanken.config.QuerydslConfig
import io.github.ddogga.blanken.domain.QuizSetOrderEnum
import io.github.ddogga.blanken.support.PostgresTestContainerConfig
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.jdbc.Sql
import kotlin.test.assertEquals


@DataJpaTest
@Import(PostgresTestContainerConfig::class, QuerydslConfig::class)
@Sql("/data.sql")
class QuizSetRepositoryTest (
    @Autowired private val entityManager: EntityManager,
    @Autowired private val quizSetRepository: QuizSetRepository,
    @Autowired private val categoryRepository: CategoryRepository,
) {

    @BeforeEach()
    fun init() {

        val category = categoryRepository.findByName("토익")
        searchCategoryId = category.id!!
    }


    @Test
    fun `키워드가_제목에_포함된_퀴즈셋만_반환한다`() {

        // given
        val pageable = PageRequest.of(0, 10)

        // when
        val searchQuizSet = quizSetRepository.searchQuizSet(KEYWORD, null, QuizSetOrderEnum.CREATE_AT_DESC, pageable)

        // then
        assertTrue(searchQuizSet.content[7].title.contains("토익"))
        assertEquals(11, searchQuizSet.totalElements)

    }

    @Test
    fun `카테고리_기반_검색_성공`() {
        // given
        val pageable = PageRequest.of(0, 10)


        // when
        val searchQuizSet = quizSetRepository.searchQuizSet(null, searchCategoryId, QuizSetOrderEnum.CREATE_AT_DESC, pageable)

        // then
        assertEquals(searchCategoryId, searchQuizSet.content[2].category.id)
        assertEquals(10, searchQuizSet.totalElements)

    }

    @Test
    fun `비공개_퀴즈셋은_검색되지_않는다`() {
        // given
        val pageable = PageRequest.of(0, 10)

        // when
        val searchQuizSet = quizSetRepository.searchQuizSet(null, null, QuizSetOrderEnum.CREATE_AT_DESC, pageable)

        // then
        assertEquals(10, searchQuizSet.size)
        assertEquals(10, searchQuizSet.totalPages)
        assertEquals(93, searchQuizSet.totalElements)
    }


    companion object {
        private const val KEYWORD = "토익"
        private var searchCategoryId = 1L


    }

}