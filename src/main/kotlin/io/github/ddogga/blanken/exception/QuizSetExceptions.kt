package io.github.ddogga.blanken.exception

class QuizSetNotFoundException(val quizSetId: Long) :
        BusinessException(ErrorCode.QUIZ_SET_NOT_FOUND, "퀴즈 셋을 찾을 수 없습니다. (id=$quizSetId)")

class QuizSetTitleDuplicationException(val title: String) :
        BusinessException(ErrorCode.QUIZ_SET_TITLE_DUPLICATION, "똑같은 이름 ($title) 의 퀴즈셋이 이미 존재합니다.")

class QuizSetLikeDuplicationException(val quizSetId: Long) :
        BusinessException(ErrorCode.QUIZ_SET_LIKE_DUPLICATION, "이미 좋아요를 누른 퀴즈셋 입니다. (id=$quizSetId)")

class QuizSetAccessDeniedException(val quizSetId: Long) :
        BusinessException(ErrorCode.QUIZ_SET_ACCESS_DENIED, "접근 권한이 없는 퀴즈셋입니다. (id=$quizSetId)")

class NotEnoughQuizzesException(
    val quizSetId: Long,
    val required: Int,
    val actual: Int,
) : BusinessException(ErrorCode.NOT_ENOUGH_QUIZZES,"퀴즈가 $required 개 이상인 퀴즈셋만 학습 가능합니다. 현재 퀴즈 개수 : $actual (id=$quizSetId)")