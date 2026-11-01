package io.github.ddogga.blanken.exception

class QuizNotFoundException(val quizId: Long) :
        BusinessException(ErrorCode.QUIZ_NOT_FOUND, "퀴즈를 찾을 수 없습니다. (id=$quizId)")

class QuizRevisionNotFoundException(val revisionIds: Collection<Long>) :
        BusinessException(ErrorCode.QUIZ_REVISION_NOT_FOUND, "퀴즈 스냅샷을 찾을 수 없습니다. (ids=$revisionIds)")
