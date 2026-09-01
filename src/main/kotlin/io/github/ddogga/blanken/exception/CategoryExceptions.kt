package io.github.ddogga.blanken.exception

class CategoryNotFoundException(val categoryIds: List<Long>) :
        BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "카테고리를 찾을 수 없습니다. (id=$categoryIds)")