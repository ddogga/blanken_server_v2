package io.github.ddogga.blanken.repository

import io.github.ddogga.blanken.domain.quiz.Category
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRepository : JpaRepository<Category, Long>{


    fun findByName(name : String) : Category




}