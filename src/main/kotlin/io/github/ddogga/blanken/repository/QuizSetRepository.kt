package io.github.ddogga.blanken.repository

import io.github.ddogga.blanken.domain.QuizSet
import io.github.ddogga.blanken.repository.querydsl.QuizSetCustomRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface QuizSetRepository : JpaRepository<QuizSet, Long>, QuizSetCustomRepository{


	@Query(
		"""
		select qs from QuizSet qs
		join fetch qs.owner
		join fetch qs.category
		where qs.id = :id
		"""
	)
	fun findWithCategoryById(id: Long): QuizSet?


    fun existsByOwnerIdAndTitle(ownerId: Long, title: String): Boolean

    // 원자적 update를 위해 더티 체킹 대신 @Query를 사용
    @Modifying(clearAutomatically = true)
    @Query(
        """
        update QuizSet qs
        set qs.likeCount = qs.likeCount + 1
        where qs.id = :id
        """
    )
    fun addLikeCount(id: Long): Int

}
