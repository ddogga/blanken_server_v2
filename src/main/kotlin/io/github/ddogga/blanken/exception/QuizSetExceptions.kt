package io.github.ddogga.blanken.exception

class QuizSetNotFoundExceptions(val quizSetId: Long) :
        BusinessException(ErrorCode.QUIZ_SET_NOT_FOUND, "퀴즈 셋을 찾을 수 없습니다. (id=$quizSetId)")

class QuizSetTitleDuplicationException(val title: String) :
        BusinessException(ErrorCode.QUIZ_SET_TITLE_DUPLICATION, "똑같은 이름 ($title) 의 퀴즈셋이 이미 존재합니다.")