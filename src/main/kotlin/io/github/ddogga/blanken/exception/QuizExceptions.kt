package io.github.ddogga.blanken.exception

class QuizNotFoundException(val quizId: Long) :
        BusinessException(ErrorCode.QUIZ_NOT_FOUND, "퀴즈를 찾을 수 없습니다. (id=$quizId)")