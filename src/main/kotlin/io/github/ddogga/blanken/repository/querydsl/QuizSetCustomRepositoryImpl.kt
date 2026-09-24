package io.github.ddogga.blanken.repository.querydsl

import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import io.github.ddogga.blanken.domain.QCategory.category
import io.github.ddogga.blanken.domain.QQuizSet.quizSet
import io.github.ddogga.blanken.domain.QUser.user
import io.github.ddogga.blanken.domain.QuizSetOrderEnum
import io.github.ddogga.blanken.domain.Visibility
import io.github.ddogga.blanken.dto.category.CategoryResponse
import io.github.ddogga.blanken.dto.quiz.QuizSetResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.stereotype.Repository


@Repository
class QuizSetCustomRepositoryImpl (
    private val queryFactory: JPAQueryFactory
) : QuizSetCustomRepository {


    override fun searchQuizSet(keyword: String?, categoryId: Long?, orderEnum: QuizSetOrderEnum, pageable: Pageable): Page<QuizSetResponse> {

        val content = queryFactory
            .select(
                Projections.constructor(
                    QuizSetResponse::class.java,
                    quizSet.id,
                    user.id,
                    user.nickname,
                    quizSet.title,
                    quizSet.description,
                    quizSet.visibility,
                    quizSet.likeCount,
                    quizSet.quizCount,
                    Projections.constructor(
                        CategoryResponse::class.java,
                        category.id,
                        category.name
                    ),
                    quizSet.createdAt,
                    quizSet.updatedAt
                )
            )
            .from(quizSet)
            .join(quizSet.category, category)
            .join(quizSet.owner, user)
            .where(*searchPredicates(keyword, categoryId))
            .orderBy(*orderSpecifiers(orderEnum))
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(quizSet.count())
            .from(quizSet)
            .where(*searchPredicates(keyword, categoryId))

        return PageableExecutionUtils.getPage(content, pageable) { countQuery.fetchOne() ?: 0L }
    }


    private fun searchPredicates(keyword: String?, categoryId: Long?): Array<Predicate?> {
        return arrayOf(
            quizSet.visibility.eq(Visibility.PUBLIC),
            keyword?.takeIf { it.isNotBlank() }?.let { quizSet.title.upper().contains(it.uppercase()) },
            categoryId?.let { quizSet.category.id.eq(categoryId) }
        )
    }


    private fun orderSpecifiers(orderEnum: QuizSetOrderEnum): Array<OrderSpecifier<*>> {

        return when (orderEnum) {
            QuizSetOrderEnum.CREATE_AT_DESC -> arrayOf(quizSet.createdAt.desc(), quizSet.id.desc())
            QuizSetOrderEnum.LIKE_COUNT_DESC -> arrayOf(quizSet.likeCount.desc(), quizSet.id.desc())
        }
    }

}