package com.jpd.web.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SessionStatus", description = """
        Lifecycle state of a live quiz session.

        * `WAITING` — open for learners to join; the only state in which joining is allowed
        * `ACTIVE` — the quiz is running; joining is refused
        * `PAUSED` — temporarily halted by the host
        * `SHOWING_RESULTS` — displaying the results of the question that just closed
        * `FINISHED` — the quiz has ended; joining is refused
        """)
public enum SessionStatus {
    WAITING,        // Chờ học sinh join
    ACTIVE,         // Đang chơi
    PAUSED,         // Tạm dừng
    SHOWING_RESULTS, // Đang hiển thị kết quả câu hỏi
    FINISHED        // Kết thúc
}