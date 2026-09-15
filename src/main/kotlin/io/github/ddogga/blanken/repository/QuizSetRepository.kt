package io.github.ddogga.blanken.repository

import io.github.ddogga.blanken.domain.QuizSet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface QuizSetRepository : JpaRepository<QuizSet, Long> {


	@Query(
		"""
		select qs from QuizSet qs
		join fetch qs.owner
		left join fetch qs.mutableCategories qc
		left join fetch qc.category
		where qs.id = :id
		"""
	)
	fun findWithCategoriesById(id: Long): QuizSet?
}
