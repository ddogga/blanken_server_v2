package io.github.ddogga.blanken.repository.querydsl

import com.querydsl.core.types.Predicate
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import io.github.ddogga.blanken.domain.QCategory.category
import io.github.ddogga.blanken.domain.QQuizSet.quizSet
import io.github.ddogga.blanken.domain.QQuizSetLike.quizSetLike
import io.github.ddogga.blanken.domain.QUser.user
import io.github.ddogga.blanken.domain.quiz.Visibility
import io.github.ddogga.blanken.dto.category.CategoryResponse
import io.github.ddogga.blanken.dto.quiz.QuizSetResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

class QuizSetLikeCustomRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : QuizSetLikeCustomRepository{


    override fun getLikeQuizSets(userId: Long, pageable: Pageable): Page<QuizSetResponse> {

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
            .from(quizSetLike)
            .join(quizSetLike.quizSet, quizSet)
            .join(quizSet.owner, user)
            .join(quizSet.category, category)
            .where(*getLikeQuizSetPredicates(userId))
            .orderBy(quizSetLike.createdAt.desc(), quizSet.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(quizSetLike.count())
            .from(quizSetLike)
            .join(quizSetLike.quizSet, quizSet)
            .where(*getLikeQuizSetPredicates(userId))

        return PageableExecutionUtils.getPage(content, pageable) { countQuery.fetchOne() ?: 0L}
    }


    private fun getLikeQuizSetPredicates(userId: Long): Array<Predicate?> {
        return arrayOf(
            quizSet.visibility.eq(Visibility.PUBLIC),
            quizSetLike.user.id.eq(userId)
        )
    }
}