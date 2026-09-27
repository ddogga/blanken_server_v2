package io.github.ddogga.blanken.exception

class QuizSetNotFoundException(val quizSetId: Long) :
        BusinessException(ErrorCode.QUIZ_SET_NOT_FOUND, "퀴즈 셋을 찾을 수 없습니다. (id=$quizSetId)")

class QuizSetTitleDuplicationException(val title: String) :
        BusinessException(ErrorCode.QUIZ_SET_TITLE_DUPLICATION, "똑같은 이름 ($title) 의 퀴즈셋이 이미 존재합니다.")

class QuizSetLikeDuplicationException(val quizSetId: Long) :
        BusinessException(ErrorCode.QUIZ_SET_LIKE_DUPLICATION, "이미 좋아요를 누른 퀴즈셋 입니다. (id=$quizSetId)")