package io.github.ddogga.blanken.exception

class NotEnoughQuizzesException(
    val quizSetId: Long,
    val required: Int,
    val actual: Int,
) : BusinessException(ErrorCode.NOT_ENOUGH_QUIZZES,"퀴즈가 $required 개 이상인 퀴즈셋만 학습 가능합니다. 현재 퀴즈 개수 : $actual (id=$quizSetId)")


class StudySessionNotFoundException(
    val quizSetId: Long,
) : BusinessException(ErrorCode.STUDY_SESSION_NOT_FOUND, "학습 세션을 찾을 수 없습니다. (quizSetId=$quizSetId)")

class StudySessionMismatchException(
    val sessionId: String,
) : BusinessException(ErrorCode.STUDY_SESSION_MISMATCH, "학습 세션이 교체 되었습니다. (sessionId=$sessionId)")

class QuizNotInSessionException(
    val quizId: Long,
) : BusinessException(ErrorCode.QUIZ_NOT_IN_SESSION, "학습 세션에 포함되어 있지 않은 퀴즈 입니다. (quizId=$quizId)")