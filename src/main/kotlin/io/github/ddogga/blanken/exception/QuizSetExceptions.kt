package io.github.ddogga.blanken.exception

class QuizSetNotFoundExceptions(val quizSetId: Long) :
        BusinessException(ErrorCode.QUIZ_SET_NOT_FOUND, "퀴즈 셋을 찾을 수 없습니다. (id=$quizSetId)")

