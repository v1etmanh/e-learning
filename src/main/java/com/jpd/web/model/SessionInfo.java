package com.jpd.web.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@io.swagger.v3.oas.annotations.media.Schema(name = "SessionInfo", description = """
        Complete state of a live quiz session, held in Redis for the duration of the game.

        Note this exposes `questionIds` and the current question's identity to anyone who can read the
        session, including learners.
        """)
public class SessionInfo implements Serializable {

    // === Session Identity ===
    @io.swagger.v3.oas.annotations.media.Schema(description = "Internal UUID of the session.", example = "3f1a9c22-77bd-4e0a-9b41-5c2d8e6f0a11")
    private String sessionId;           // UUID unique cho session

    @io.swagger.v3.oas.annotations.media.Schema(description = "Six-digit PIN learners type to join.", example = "482913")
    private String sessionCode;         // Mã PIN 6 số để học sinh join

    // === Quiz Information ===
    @io.swagger.v3.oas.annotations.media.Schema(description = "Identifier of the Kahoot this session is running.", example = "77")
    private Long kahootId;              // Link đến KahootListFunction

    @io.swagger.v3.oas.annotations.media.Schema(description = "Title of the quiz.", example = "On tap Kanji N5")
    private String title;               // Tiêu đề quiz

    @io.swagger.v3.oas.annotations.media.Schema(description = "Identifiers of the questions to be played, in order.", example = "[9042, 9043, 9044]")
    private List<Long> questionIds;     // Danh sách mcId của các câu hỏi

    @io.swagger.v3.oas.annotations.media.Schema(description = "Number of questions in the session.", example = "15")
    private Integer totalQuestions;     // Tổng số câu hỏi

    // === Teacher Information ===
    @io.swagger.v3.oas.annotations.media.Schema(description = "Keycloak user id of the host who created the session.", example = "7f3c1a90-2b45-4e88-9f01-0a1b2c3d4e5f")
    private String teacherId;             // ID giáo viên

    @io.swagger.v3.oas.annotations.media.Schema(description = "Display name of the host.", example = "Co Thuy")
    private String teacherName;         // Tên giáo viên

    // === Session Status ===
    @io.swagger.v3.oas.annotations.media.Schema(description = "Current lifecycle state. Learners may only join while this is `WAITING`.", example = "WAITING")
    private SessionStatus status;       // WAITING, ACTIVE, PAUSED, FINISHED

    // === Current Question State ===
    @io.swagger.v3.oas.annotations.media.Schema(description = "Zero-based index of the question in play. Null before the quiz starts.", example = "3")
    private Integer currentQuestionIndex;   // Câu hỏi thứ mấy (0-based)

    @io.swagger.v3.oas.annotations.media.Schema(description = "Identifier of the question in play.", example = "9042")
    private Long currentQuestionId;         // mcId của câu hỏi hiện tại

    @io.swagger.v3.oas.annotations.media.Schema(description = "Kind of the question in play. Only `MULTIPLE_CHOICE` and `GAPFILL` are playable.", example = "MULTIPLE_CHOICE")
    private TypeOfContent currentQuestionType; // MULTIPLE_CHOICE, GAPFILL

    @io.swagger.v3.oas.annotations.media.Schema(description = "When the current question opened.", example = "2026-07-22T09:15:30.412")
    private LocalDateTime questionStartTime;         // Timestamp bắt đầu câu hỏi

    @io.swagger.v3.oas.annotations.media.Schema(description = "Seconds allowed for the current question.", example = "30")
    private Integer questionTimeLimit;      // Giới hạn thời gian (seconds)

    @io.swagger.v3.oas.annotations.media.Schema(description = "Whether answers are being accepted right now. Submissions are rejected when false.", example = "true")
    private boolean acceptingAnswers;       // Có đang nhận câu trả lời không

    @io.swagger.v3.oas.annotations.media.Schema(description = "When the current question closes. Answers arriving after this are rejected.", example = "2026-07-22T09:16:00.412")
    private LocalDateTime questionEndTime; // Thời gian kết thúc câu hỏi

    // === Session Timing ===
    @io.swagger.v3.oas.annotations.media.Schema(description = "When the session was created.", example = "2026-07-22T09:10:00.000")
    private LocalDateTime createdAt;    // Thời gian tạo session

    @io.swagger.v3.oas.annotations.media.Schema(description = "When the first question opened. Null while still `WAITING`.", example = "2026-07-22T09:15:00.000")
    private LocalDateTime startedAt;    // Thời gian bắt đầu quiz

    @io.swagger.v3.oas.annotations.media.Schema(description = "When the quiz ended. Null until `FINISHED`.", example = "null")
    private LocalDateTime finishedAt;   // Thời gian kết thúc

    // === Statistics ===
    @io.swagger.v3.oas.annotations.media.Schema(description = "How many learners have joined.", example = "24")
    private Integer totalParticipants;  // Tổng số người tham gia

    @io.swagger.v3.oas.annotations.media.Schema(description = "How many have answered the current question.", example = "18")
    private Integer currentAnswers;     // Số người đã trả lời câu hiện tại

    // === Settings ===
    @io.swagger.v3.oas.annotations.media.Schema(description = "Whether to show the leaderboard after every question.", example = "true")
    private boolean showLeaderboardAfterEachQuestion; // Hiển thị bảng xếp hạng sau mỗi câu

    @io.swagger.v3.oas.annotations.media.Schema(description = "Whether question order is shuffled.", example = "false")
    private boolean randomizeQuestions; // Xáo trộn câu hỏi

    @io.swagger.v3.oas.annotations.media.Schema(description = "Whether answer options are shuffled.", example = "false")
    private boolean randomizeOptions;   // Xáo trộn đáp án
}