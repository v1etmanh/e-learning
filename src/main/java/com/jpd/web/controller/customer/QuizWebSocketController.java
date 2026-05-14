package com.jpd.web.controller.customer;

import com.jpd.web.dto.*;
import com.jpd.web.exception.QuizCompletedException;
import com.jpd.web.model.*;
import com.jpd.web.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.util.List;
import java.util.Map;

@Controller
public class QuizWebSocketController {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private SessionService sessionService;
    
    /**
     * Student join qua WebSocket
     */
    @MessageMapping("/quiz/join/{sessionCode}")
    public void joinSession(
            @DestinationVariable String sessionCode,
            @Payload JoinSessionRequest request) {
        
        try {
           
            // Join session
            ParticipantInfo participant = sessionService.joinSession(request);
            
            // Broadcast đến teacher và tất cả clients
            messagingTemplate.convertAndSend(
                "/topic/quiz/" + sessionCode + "/participants",
                Map.of(
                    "type", "PARTICIPANT_JOINED",
                    "participant", participant,
                    "totalParticipants", sessionService.getSession(sessionCode).getTotalParticipants()
                )
            );
            
           
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Teacher request danh sách participants hiện tại
     */
    @MessageMapping("/quiz/{sessionCode}/get-participants")
    public void getParticipants(@DestinationVariable String sessionCode) {
        try {
            List<ParticipantInfo> participants = sessionService.getParticipants(sessionCode);
            
            messagingTemplate.convertAndSend(
                "/topic/quiz/" + sessionCode + "/participants",
                Map.of(
                    "type", "PARTICIPANTS_LIST",
                    "participants", participants,
                    "totalParticipants", participants.size()
                )
            );
            
             
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // ==================== QUIZ CONTROL ====================
    
    /**
     * 🎮 Teacher START QUIZ
     */
    @MessageMapping("/quiz/{sessionCode}/start")
    public void startQuiz(@DestinationVariable String sessionCode) {
        try {
           
            SessionInfo session = sessionService.getSession(sessionCode);
            if (session == null) {
                throw new RuntimeException("Session not found");
            }
            
            // Update status
            session.setStatus(SessionStatus.ACTIVE);
            session.setCurrentQuestionIndex(-1);
            sessionService.saveSession(sessionCode, session);
            
            // Broadcast QUIZ_STARTED
            messagingTemplate.convertAndSend(
                "/topic/quiz/" + sessionCode,
                Map.of(
                    "type", "QUIZ_STARTED",
                    "message", "Quiz is starting!",
                    "totalQuestions", session.getTotalQuestions()
                )
            );
            
           
            // ✨✨✨ THÊM PHẦN NÀY - Tự động start câu hỏi đầu tiên sau 2 giây
            new Thread(() -> {
                try {
                    Thread.sleep(2000); // Delay 2 giây
                    
                    StartQuestionResponse response = sessionService.startNextQuestion(sessionCode);
                    
                    messagingTemplate.convertAndSend(
                        "/topic/quiz/" + sessionCode,
                        Map.of(
                            "type", "QUESTION_STARTED",
                            "questionId", response.getQuestionId(),
                            "questionNumber", response.getQuestionNumber(),
                            "question", response.getQuestion(),
                            "timeLimit", response.getTimeLimit(),
                            "serverTime", response.getServerTime(),
                            "totalQuestions", response.getTotalQuestions()
                        )
                    );
                    
                    
                } catch (Exception e) {
                    System.err.println("❌ Failed to auto-start first question: " + e.getMessage());
                }
            }).start();
            // ✨✨✨ KẾT THÚC PHẦN THÊM
            
        } catch (Exception e) {
            System.err.println("❌ Start quiz error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 📤 Teacher NEXT QUESTION
     */
    @MessageMapping("/quiz/{sessionCode}/next-question")
    public void nextQuestion(@DestinationVariable String sessionCode) {
        try {
           
            StartQuestionResponse response = sessionService.startNextQuestion(sessionCode);
            
            // Broadcast câu hỏi MỚI đến TẤT CẢ participants
            messagingTemplate.convertAndSend(
                "/topic/quiz/" + sessionCode,
                Map.of(
                    "type", "QUESTION_STARTED",
                    "questionId", response.getQuestionId(),
                    "questionNumber", response.getQuestionNumber(),
                    "question", response.getQuestion(),
                    "timeLimit", response.getTimeLimit(),
                    "serverTime", response.getServerTime(),
                    "totalQuestions", response.getTotalQuestions()
                )
            );
            

            
        } catch (QuizCompletedException e) {
            // Hết câu hỏi rồi → Broadcast QUIZ_ENDED
           
            messagingTemplate.convertAndSend(
                "/topic/quiz/" + sessionCode,
                Map.of(
                    "type", "QUIZ_ENDED",
                    "message", "Quiz has ended! All questions completed.",
                    "finalLeaderboard", e.getFinalLeaderboard()
                )
            );
            
           
        } catch (Exception e) {
            System.err.println("❌ Next question error: " + e.getMessage());
            e.printStackTrace();
            
            // Broadcast error
            messagingTemplate.convertAndSend(
                "/topic/quiz/" + sessionCode,
                Map.of(
                    "type", "ERROR",
                    "message", e.getMessage()
                )
            );
        }
    }
    
    /**
     * 📥 Student SUBMIT ANSWER
     */
    @MessageMapping("/quiz/{sessionCode}/submit-answer")
    public void submitAnswer(
            @DestinationVariable String sessionCode,
            @Payload SubmitAnswerRequest request) {
        
        try {
            SubmitAnswerResponse response = sessionService.submitAnswer(request);
            
            // Broadcast CẬP NHẬT SỐ NGƯỜI ĐÃ TRẢ LỜI
            messagingTemplate.convertAndSend(
                "/topic/quiz/" + sessionCode,
                Map.of(
                    "type", "ANSWER_SUBMITTED",
                    "totalAnswered", response.getTotalAnswered(),
                    "totalParticipants", response.getTotalParticipants()
                )
            );
            
            
        } catch (Exception e) {
            System.err.println("❌ Submit answer error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 🏁 Teacher END QUESTION (chấm điểm)
     */
    @MessageMapping("/quiz/{sessionCode}/end-question")
    public void endQuestion(@DestinationVariable String sessionCode) {
        try {
           
            QuestionResultResponse result = sessionService.endQuestion(sessionCode);
            
            // Broadcast KẾT QUẢ + ĐÁNH GIÁ đến TẤT CẢ
            messagingTemplate.convertAndSend(
                "/topic/quiz/" + sessionCode,
                Map.of(
                    "type", "QUESTION_ENDED",
                    "questionId", result.getQuestionId(),
                    "correctAnswer", result.getCorrectAnswer(),
                    "results", result.getResults()
                )
            );
            
            
        } catch (Exception e) {
            System.err.println("❌ End question error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 📊 Teacher SHOW LEADERBOARD
     */
    @MessageMapping("/quiz/{sessionCode}/show-leaderboard")
    public void showLeaderboard(@DestinationVariable String sessionCode) {
        try {
            List<ParticipantInfo> participants = sessionService.getParticipants(sessionCode);
            
            // Sort by score (cao → thấp)
            participants.sort((a, b) -> Integer.compare(b.getCurrentScore(), a.getCurrentScore()));
            
            // Broadcast LEADERBOARD
            messagingTemplate.convertAndSend(
                "/topic/quiz/" + sessionCode,
                Map.of(
                    "type", "LEADERBOARD",
                    "participants", participants
                )
            );
            
            
        } catch (Exception e) {
            System.err.println("❌ Leaderboard error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 🎯 Teacher END QUIZ
     */
    @MessageMapping("/quiz/{sessionCode}/end-quiz")
    public void endQuiz(@DestinationVariable String sessionCode) {
        try {
           
            SessionInfo session = sessionService.getSession(sessionCode);
            session.setStatus(SessionStatus.FINISHED);
            sessionService.saveSession(sessionCode, session);
            
            // Get final leaderboard
            List<ParticipantInfo> participants = sessionService.getParticipants(sessionCode);
            participants.sort((a, b) -> Integer.compare(b.getCurrentScore(), a.getCurrentScore()));
            
            // Broadcast QUIZ_ENDED
            messagingTemplate.convertAndSend(
                "/topic/quiz/" + sessionCode,
                Map.of(
                    "type", "QUIZ_ENDED",
                    "message", "Quiz has ended!",
                    "finalLeaderboard", participants
                )
            );
            
            
        } catch (Exception e) {
            System.err.println("❌ End quiz error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Test ping message
     */
    @MessageMapping("/quiz/ping")
    public void ping() {
        messagingTemplate.convertAndSend("/topic/quiz/test", Map.of("message", "PONG"));
        }
}