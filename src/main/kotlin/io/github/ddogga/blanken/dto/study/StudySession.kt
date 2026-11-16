package io.github.ddogga.blanken.dto.study

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import java.time.Instant



/**
 * Redis Hash에 저장되는 학습 세션 한 건을 표현하는 도메인 객체.
 *
 * study:session:{userId}:{quizSetId}  (Hash, TTL 24h 롤링)
 *   sessionId : UUID 문자열 (완주 flush 멱등 키)
 *   quizzes   : QuizSnapshot 리스트의 JSON
 *   startedAt : epoch seconds
 *   r:{quizId}: CORRECT | GAVE_UP
 */
@Schema(description = "Redis에 저장된 학습 세션 (내부 모델)")
data class StudySession(
    @field:Schema(description = "세션 ID (UUID). 완주 시 학습 기록 저장의 멱등 키로 사용", example = "3f2b8c1e-7d4a-4e9b-a1c2-5d6e7f8a9b0c")
    val sessionId: String,

    @field:Schema(description = "세션 시작 시점의 퀴즈 스냅샷 목록 (출제 순서)")
    val quizzes: List<QuizSnapshot>,

    @field:Schema(description = "세션 시작 시각", example = "2026-11-15T05:13:00Z")
    val startedAt: Instant,

    @field:Schema(description = "문제별 결과 (quizId → 결과). 아직 풀지 않은 문제는 포함되지 않음")
    val results: Map<Long, StudyResult>,

    @field:Schema(description = "세션 만료 예정 시각. 마지막 학습 활동 기준 24시간", example = "2026-11-16T05:13:00Z")
    val expiresAt: Instant,
)

/**
 * 세션 시작 시점의 퀴즈 내용 스냅샷.
 * 이어풀기 응답은 DB가 아니라 이 스냅샷에서 나가므로, 원본이 수정돼도 세션은 시작 시점 내용으로 유지 됨.
 */
@Schema(description = "세션 시작 시점에 고정된 퀴즈 내용. 원본 퀴즈가 수정돼도 세션 동안 바뀌지 않음")
data class QuizSnapshot(
    @field:Schema(description = "퀴즈 ID", example = "12")
    val quizId: Long,

    @field:Schema(description = "스냅샷을 만든 시점의 퀴즈 리비전 ID. 학습 기록이 이 버전을 참조함", example = "340")
    val revisionId: Long,

    @field:Schema(description = "빈칸이 {{}}로 표시된 영어 문장", example = "She {{}} up early every morning.")
    val sentence: String,

    @field:Schema(description = "빈칸 정답 목록. 문장의 {{}} 순서와 같음 (클라이언트 채점용)", example = "[\"gets\"]")
    val answers: List<String>,

    @field:Schema(description = "문장의 한국어 번역", example = "그녀는 매일 아침 일찍 일어난다.", nullable = true)
    val translation: String?,

    @field:Schema(description = "힌트", example = "'일어나다'를 뜻하는 구동사", nullable = true)
    val hint: String?,
)

enum class StudyResult { CORRECT, GAVE_UP }

/** 세션 조회/생성 결과. created=true 이면 이번 요청에서 새로 만든 세션*/
@Schema(description = "세션 조회/생성 결과 (내부 모델)")
data class StudySessionStartResult(
    @field:Schema(description = "학습 세션")
    val session: StudySession,

    @field:Schema(description = "이번 요청에서 새로 생성했으면 true, 기존 세션이면 false")
    val created: Boolean,
)


data class StudySessionResponse(
    @field:Schema(description = "세션 ID (UUID). 결과 저장·완주 요청 시 사용", example = "3f2b8c1e-7d4a-4e9b-a1c2-5d6e7f8a9b0c")
    val sessionId: String,

    @field:Schema(description = "기존 세션을 이어서 푸는 경우 true, 새로 시작한 경우 false")
    val resumed: Boolean,

    @field:Schema(description = "세션 시작 시각", example = "2026-11-15T05:13:00Z")
    val startedAt: Instant,

    @field:Schema(description = "세션 만료 예정 시각. 이 시각이 지나면 처음부터 다시 풀어야 함", example = "2026-11-16T05:13:00Z")
    val expiresAt: Instant,

    @field:Schema(description = "출제 순서대로 정렬된 퀴즈 목록. 클라이언트 채점용으로 정답 포함")
    val quizzes: List<QuizSnapshot>,

    @field:Schema(
        description = "이미 푼 문제의 결과 (quizId → 결과). JSON에서 키는 문자열로 직렬화됨",
        example = "{\"12\": \"CORRECT\", \"15\": \"GAVE_UP\"}",
    )
    val results: Map<Long, StudyResult>,

    @field:Schema(description = "이어서 풀 문제의 인덱스 (quizzes 기준, 0부터). 모두 풀었으면 quizzes.size", example = "2")
    val nextQuizIndex: Int,
) {

    companion object {
        fun from(result: StudySessionStartResult): StudySessionResponse {
            val session = result.session
            val next = session.quizzes.indexOfFirst { it.quizId !in session.results }
                .let { if (it == -1) session.quizzes.size else it }

            return StudySessionResponse(
                sessionId = session.sessionId,
                resumed = !result.created,
                startedAt = session.startedAt,
                expiresAt = session.expiresAt,
                quizzes = session.quizzes,
                results = session.results,
                nextQuizIndex = next,
            )
        }
    }

}


@Schema(description = "학습 결과 저장 요청")
data class StudySessionResultsRequest(
    @field:NotEmpty
    @field:Valid
    @field:Schema(description = "저장할 문제별 결과 목록. 같은 quizId를 다시 보내면 마지막 값으로 덮어씀")
    val results: List<QuizResult>,
)


@Schema(description = "문제 하나의 결과")
data class QuizResult(
    @field:Schema(description = "퀴즈 ID (세션의 quizzes에 있는 ID여야 함)", example = "12")
    val quizId: Long,

    @field:Schema(description = "결과. CORRECT = 맞힘, GAVE_UP = 포기", example = "CORRECT")
    val result: StudyResult,
)

@Schema(description = "학습 결과 저장 응답")
data class StudySessionProgressResponse(
    @field:Schema(description = "세션 ID", example = "3f2b8c1e-7d4a-4e9b-a1c2-5d6e7f8a9b0c")
    val sessionId: String,

    @field:Schema(description = "결과가 저장된 문제 수", example = "7")
    val solvedCount: Int,

    @field:Schema(description = "세션의 전체 문제 수", example = "20")
    val totalCount: Int,

    @field:Schema(description = "모든 문제를 풀었으면 true. 이 값이 true면 완주 API를 호출하면 됨")
    val completable: Boolean,

    @field:Schema(description = "연장된 세션 만료 예정 시각", example = "2026-11-17T02:15:00Z")
    val expiresAt: Instant,
)

/** 결과 저장 스크립트 결과. 성공/실패 사유를 타입으로 구분 */
sealed interface SaveResultsOutcome {
    data class Saved(val solvedCount: Int, val totalCount: Int, val expiresAt: Instant) : SaveResultsOutcome
    data object NotFound : SaveResultsOutcome
    data object SessionMismatch : SaveResultsOutcome
    data class InvalidQuiz(val quizId: Long) : SaveResultsOutcome
}