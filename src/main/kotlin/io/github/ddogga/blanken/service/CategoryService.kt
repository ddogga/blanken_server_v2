package io.github.ddogga.blanken.service

import io.github.ddogga.blanken.dto.category.CategoryResponse
import io.github.ddogga.blanken.repository.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional(readOnly = true)
class CategoryService(
    private val categoryRepository: CategoryRepository
) {

    fun getAll(): List<CategoryResponse> =
        categoryRepository.findAll().map{ category ->
            CategoryResponse.from(category)
        }



}