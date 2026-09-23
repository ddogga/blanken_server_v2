package io.github.ddogga.blanken.repository

import io.github.ddogga.blanken.domain.QuizSet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface QuizSetRepository : JpaRepository<QuizSet, Long> {


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

}
