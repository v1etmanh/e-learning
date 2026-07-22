package com.jpd.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpd.web.dto.CreateSessionRequest;
import com.jpd.web.dto.CreateSessionResponse;
import com.jpd.web.dto.ParticipantInfo;
import com.jpd.web.dto.QuestionResultResponse;
import com.jpd.web.dto.StartQuestionResponse;
import com.jpd.web.dto.SubmitAnswerRequest;
import com.jpd.web.dto.SubmitAnswerResponse;
import com.jpd.web.exception.QuizCompletedException;
import com.jpd.web.model.Creator;
import com.jpd.web.model.GapFillAnswer;
import com.jpd.web.model.GapFillQuestion;
import com.jpd.web.model.JoinSessionRequest;
import com.jpd.web.model.KahootListFunction;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.MultipleChoiceOption;
import com.jpd.web.model.MultipleChoiceQuestion;
import com.jpd.web.model.SessionInfo;
import com.jpd.web.model.SessionStatus;
import com.jpd.web.model.TypeOfContent;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.KahootRepository;
import com.jpd.web.repository.ModuleContentRepository;
import com.jpd.web.testutil.CreatorTestDataBuilder;
import com.jpd.web.testutil.KahootTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private KahootRepository kahootRepository;
    @Mock
    private CreatorRepository creatorRepository;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ModuleContentRepository moduleContentRepository;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private SetOperations<String, String> setOps;
    @Mock
    private HashOperations<String, Object, Object> hashOps;

    private SessionService sessionService;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        sessionService = new SessionService();
        ReflectionTestUtils.setField(sessionService, "kahootRepository", kahootRepository);
        ReflectionTestUtils.setField(sessionService, "creatorRepository", creatorRepository);
        ReflectionTestUtils.setField(sessionService, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(sessionService, "objectMapper", mapper);
        ReflectionTestUtils.setField(sessionService, "moduleContentRepository", moduleContentRepository);
    }

    private ModuleContent aQuestion(long mcId, TypeOfContent type) {
        ModuleContent mc = MultipleChoiceQuestion.builder().build();
        mc.setMcId(mcId);
        mc.setTypeOfContent(type);
        return mc;
    }

    private String toJson(Object o) throws Exception {
        return mapper.writeValueAsString(o);
    }

    @Nested
    class CreateSession {

        @Test
        void throwsRuntimeException_whenKahootMissing() {
            when(kahootRepository.findById(1L)).thenReturn(Optional.empty());
            CreateSessionRequest req = new CreateSessionRequest(1L, "teacher");

            assertThrows(RuntimeException.class, () -> sessionService.createSession(req, "creator-1"));
        }

        @Test
        void throwsUnauthorized_whenNotOwner() {
            KahootListFunction kahoot = KahootTestDataBuilder.aKahootOwnedBy("owner-1");
            when(kahootRepository.findById(1L)).thenReturn(Optional.of(kahoot));
            when(creatorRepository.findById("someone-else"))
                    .thenReturn(Optional.of(CreatorTestDataBuilder.aCreatorWithId("someone-else")));
            CreateSessionRequest req = new CreateSessionRequest(1L, "teacher");

            assertThrows(com.jpd.web.exception.UnauthorizedException.class,
                    () -> sessionService.createSession(req, "someone-else"));
        }

        @Test
        void throwsRuntimeException_whenNoMcOrGapfillQuestions() {
            KahootListFunction kahoot = KahootTestDataBuilder.aKahootOwnedBy("creator-1");
            kahoot.setModuleContent(List.of(aQuestion(1L, TypeOfContent.READING)));
            when(kahootRepository.findById(1L)).thenReturn(Optional.of(kahoot));
            when(creatorRepository.findById("creator-1"))
                    .thenReturn(Optional.of(CreatorTestDataBuilder.aCreatorWithId("creator-1")));
            CreateSessionRequest req = new CreateSessionRequest(1L, "teacher");

            assertThrows(RuntimeException.class, () -> sessionService.createSession(req, "creator-1"));
        }

        @Test
        void happyPath_savesSessionAndClearsParticipants() {
            KahootListFunction kahoot = KahootTestDataBuilder.aKahootOwnedBy("creator-1");
            kahoot.setTitle("My Quiz");
            kahoot.setModuleContent(List.of(
                    aQuestion(1L, TypeOfContent.MULTIPLE_CHOICE),
                    aQuestion(2L, TypeOfContent.GAPFILL),
                    aQuestion(3L, TypeOfContent.READING)));
            when(kahootRepository.findById(1L)).thenReturn(Optional.of(kahoot));
            when(creatorRepository.findById("creator-1"))
                    .thenReturn(Optional.of(CreatorTestDataBuilder.aCreatorWithId("creator-1")));
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            CreateSessionRequest req = new CreateSessionRequest(1L, "teacher");

            CreateSessionResponse response = sessionService.createSession(req, "creator-1");

            assertThat(response.getTotalQuestions()).isEqualTo(2);
            assertThat(response.getSessionCode()).matches("^[A-Z0-9]{6}$");
            verify(valueOps).set(eq("quiz:session:" + response.getSessionCode()), any(String.class), eq(3L), eq(TimeUnit.HOURS));
            verify(stringRedisTemplate).delete("quiz:session:" + response.getSessionCode() + ":participants");
        }
    }

    @Nested
    class GenerateUniqueSessionCode {

        @Test
        void producesSixCharacterUppercaseAlphanumericCode() {
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(any())).thenReturn(null);

            String code = ReflectionTestUtils.invokeMethod(sessionService, "generateUniqueSessionCode");

            assertThat(code).matches("^[A-Z0-9]{6}$");
        }

        @Test
        void retriesUntilNoCollision() throws Exception {
            SessionInfo existing = SessionInfo.builder().sessionCode("EXIST1").status(SessionStatus.WAITING).build();
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(any())).thenReturn(toJson(existing), (String) null);

            String code = ReflectionTestUtils.invokeMethod(sessionService, "generateUniqueSessionCode");

            assertThat(code).matches("^[A-Z0-9]{6}$");
            verify(valueOps, org.mockito.Mockito.times(2)).get(any());
        }
    }

    @Nested
    class JoinSession {

        @Test
        void throwsRuntimeException_whenSessionNotFound() {
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get("quiz:session:ABC123")).thenReturn(null);
            JoinSessionRequest req = new JoinSessionRequest("ABC123", "Alice");

            assertThrows(RuntimeException.class, () -> sessionService.joinSession(req));
        }

        @Test
        void throwsRuntimeException_whenSessionFinished() throws Exception {
            SessionInfo session = SessionInfo.builder().sessionCode("ABC123").status(SessionStatus.FINISHED).build();
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get("quiz:session:ABC123")).thenReturn(toJson(session));
            JoinSessionRequest req = new JoinSessionRequest("ABC123", "Alice");

            assertThrows(RuntimeException.class, () -> sessionService.joinSession(req));
        }

        @Test
        void throwsRuntimeException_whenSessionAlreadyActive() throws Exception {
            SessionInfo session = SessionInfo.builder().sessionCode("ABC123").status(SessionStatus.ACTIVE).build();
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get("quiz:session:ABC123")).thenReturn(toJson(session));
            JoinSessionRequest req = new JoinSessionRequest("ABC123", "Alice");

            assertThrows(RuntimeException.class, () -> sessionService.joinSession(req));
        }

        @Test
        void addsParticipant_andIncrementsTotalParticipants() throws Exception {
            SessionInfo session = SessionInfo.builder().sessionCode("ABC123").status(SessionStatus.WAITING)
                    .totalParticipants(2).build();
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get("quiz:session:ABC123")).thenReturn(toJson(session));
            when(stringRedisTemplate.opsForSet()).thenReturn(setOps);
            JoinSessionRequest req = new JoinSessionRequest("ABC123", "Alice");

            ParticipantInfo participant = sessionService.joinSession(req);

            assertThat(participant.getName()).isEqualTo("Alice");
            verify(setOps).add(eq("quiz:session:ABC123:participants"), any(String.class));
            verify(valueOps).set(eq("quiz:session:ABC123"), any(String.class), eq(3L), eq(TimeUnit.HOURS));
        }
    }

    @Nested
    class SubmitAnswer {

        @Test
        void throwsRuntimeException_whenNotAcceptingAnswers() throws Exception {
            SessionInfo session = SessionInfo.builder().sessionCode("ABC123").acceptingAnswers(false).build();
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get("quiz:session:ABC123")).thenReturn(toJson(session));
            SubmitAnswerRequest req = SubmitAnswerRequest.builder()
                    .sessionCode("ABC123").participantId("p1").questionId(1L).answer("A").build();

            RuntimeException ex = assertThrows(RuntimeException.class, () -> sessionService.submitAnswer(req));
            assertThat(ex.getMessage()).contains("Not accepting answers");
        }

        @Test
        void throwsRuntimeException_whenTimeExpired() throws Exception {
            SessionInfo session = SessionInfo.builder().sessionCode("ABC123").acceptingAnswers(true)
                    .questionEndTime(LocalDateTime.now().minusSeconds(5)).build();
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get("quiz:session:ABC123")).thenReturn(toJson(session));
            SubmitAnswerRequest req = SubmitAnswerRequest.builder()
                    .sessionCode("ABC123").participantId("p1").questionId(1L).answer("A").build();

            RuntimeException ex = assertThrows(RuntimeException.class, () -> sessionService.submitAnswer(req));
            assertThat(ex.getMessage()).contains("Time's up");
        }

        @Test
        void throwsRuntimeException_whenParticipantNotFound() throws Exception {
            SessionInfo session = SessionInfo.builder().sessionCode("ABC123").acceptingAnswers(true).build();
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get("quiz:session:ABC123")).thenReturn(toJson(session));
            when(valueOps.get("quiz:session:ABC123:participant:p1")).thenReturn(null);
            SubmitAnswerRequest req = SubmitAnswerRequest.builder()
                    .sessionCode("ABC123").participantId("p1").questionId(1L).answer("A").build();

            RuntimeException ex = assertThrows(RuntimeException.class, () -> sessionService.submitAnswer(req));
            assertThat(ex.getMessage()).contains("Participant not found");
        }

        @Test
        void happyPath_storesAnswerAndIncrementsCurrentAnswers() throws Exception {
            SessionInfo session = SessionInfo.builder().sessionCode("ABC123").acceptingAnswers(true)
                    .currentAnswers(0).totalParticipants(1).build();
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get("quiz:session:ABC123")).thenReturn(toJson(session));
            ParticipantInfo participant = ParticipantInfo.builder().participantId("p1").name("Alice").build();
            when(valueOps.get("quiz:session:ABC123:participant:p1")).thenReturn(toJson(participant));
            when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);
            SubmitAnswerRequest req = SubmitAnswerRequest.builder()
                    .sessionCode("ABC123").participantId("p1").questionId(1L).answer("A").build();

            SubmitAnswerResponse response = sessionService.submitAnswer(req);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getTotalAnswered()).isEqualTo(1);
            verify(hashOps).put(eq("quiz:session:ABC123:question:1:answers"), eq("p1"), any(String.class));
        }
    }

    @Nested
    class CheckAnswerAndScoring {

        private MultipleChoiceQuestion mcQuestion(long correctOptionId) {
            MultipleChoiceQuestion q = MultipleChoiceQuestion.builder()
                    .options(List.of(
                            MultipleChoiceOption.builder().mcoId(correctOptionId).optionText("Right").isCorrect(true).build(),
                            MultipleChoiceOption.builder().mcoId(correctOptionId + 1).optionText("Wrong").isCorrect(false).build()))
                    .build();
            q.setTypeOfContent(TypeOfContent.MULTIPLE_CHOICE);
            return q;
        }

        private GapFillQuestion gapFillQuestion(String... correctAnswers) {
            GapFillQuestion q = new GapFillQuestion();
            q.setTypeOfContent(TypeOfContent.GAPFILL);
            List<GapFillAnswer> answers = new ArrayList<>();
            for (String a : correctAnswers) {
                GapFillAnswer ga = new GapFillAnswer();
                ga.setAnswer(a);
                answers.add(ga);
            }
            q.setAnswers(answers);
            return q;
        }

        @Test
        void multipleChoice_correctOptionId_returnsTrue() {
            MultipleChoiceQuestion q = mcQuestion(10L);
            boolean result = ReflectionTestUtils.invokeMethod(sessionService, "checkAnswer", q, "10");
            assertThat(result).isTrue();
        }

        @Test
        void multipleChoice_wrongOptionId_returnsFalse() {
            MultipleChoiceQuestion q = mcQuestion(10L);
            boolean result = ReflectionTestUtils.invokeMethod(sessionService, "checkAnswer", q, "11");
            assertThat(result).isFalse();
        }

        @Test
        void gapFill_caseAndWhitespaceInsensitiveMatch_returnsTrue() {
            GapFillQuestion q = gapFillQuestion("Paris", "London");
            boolean result = ReflectionTestUtils.invokeMethod(sessionService, "checkAnswer", q, " paris , LONDON ");
            assertThat(result).isTrue();
        }

        @Test
        void gapFill_answerCountMismatch_returnsFalseEvenIfEachProvidedAnswerIsCorrect() {
            GapFillQuestion q = gapFillQuestion("Paris", "London");
            boolean result = ReflectionTestUtils.invokeMethod(sessionService, "checkAnswer", q, "Paris");
            assertThat(result).isFalse();
        }

        @Test
        void calculatePoints_flat1000_whenTimeLimitMissing() {
            SessionInfo session = SessionInfo.builder()
                    .questionStartTime(LocalDateTime.now().minusSeconds(5))
                    .questionTimeLimit(null).build();
            int points = ReflectionTestUtils.invokeMethod(sessionService, "calculatePoints",
                    true, session, LocalDateTime.now());
            assertThat(points).isEqualTo(1000);
        }

        @Test
        void calculatePoints_zero_whenIncorrect() {
            SessionInfo session = SessionInfo.builder()
                    .questionStartTime(LocalDateTime.now())
                    .questionTimeLimit(30).build();
            int points = ReflectionTestUtils.invokeMethod(sessionService, "calculatePoints",
                    false, session, LocalDateTime.now());
            assertThat(points).isEqualTo(0);
        }

        @Test
        void calculatePoints_1000_whenAnsweredInstantly() {
            LocalDateTime start = LocalDateTime.now();
            SessionInfo session = SessionInfo.builder().questionStartTime(start).questionTimeLimit(30).build();
            int points = ReflectionTestUtils.invokeMethod(sessionService, "calculatePoints", true, session, start);
            assertThat(points).isEqualTo(1000);
        }

        @Test
        void calculatePoints_500_whenAnsweredExactlyAtTimeLimit() {
            LocalDateTime start = LocalDateTime.now();
            SessionInfo session = SessionInfo.builder().questionStartTime(start).questionTimeLimit(30).build();
            int points = ReflectionTestUtils.invokeMethod(sessionService, "calculatePoints",
                    true, session, start.plusSeconds(30));
            assertThat(points).isEqualTo(500);
        }

        @Test
        void calculatePoints_clampedAt500_whenAnsweredAfterTimeLimit() {
            LocalDateTime start = LocalDateTime.now();
            SessionInfo session = SessionInfo.builder().questionStartTime(start).questionTimeLimit(30).build();
            int points = ReflectionTestUtils.invokeMethod(sessionService, "calculatePoints",
                    true, session, start.plusSeconds(60));
            assertThat(points).isEqualTo(500);
        }

        @Test
        void getCorrectAnswer_multipleChoice_returnsCorrectOptionText() {
            MultipleChoiceQuestion q = mcQuestion(10L);
            String answer = ReflectionTestUtils.invokeMethod(sessionService, "getCorrectAnswer", q);
            assertThat(answer).isEqualTo("Right");
        }

        @Test
        void getCorrectAnswer_gapFill_joinsAllAnswersWithComma() {
            GapFillQuestion q = gapFillQuestion("Paris", "London");
            String answer = ReflectionTestUtils.invokeMethod(sessionService, "getCorrectAnswer", q);
            assertThat(answer).isEqualTo("Paris, London");
        }
    }

    @Nested
    class GetQuestionById {

        @Test
        void multipleChoice_stripsIsCorrectFlagFromEveryOption() {
            MultipleChoiceQuestion q = MultipleChoiceQuestion.builder()
                    .options(List.of(MultipleChoiceOption.builder().mcoId(1L).isCorrect(true).build()))
                    .build();
            q.setMcId(1L);
            q.setTypeOfContent(TypeOfContent.MULTIPLE_CHOICE);
            when(moduleContentRepository.findById(1L)).thenReturn(Optional.of(q));

            ModuleContent result = sessionService.getQuestionById(1L);

            assertThat(((MultipleChoiceQuestion) result).getOptions().get(0).isCorrect()).isFalse();
        }

        @Test
        void gapFill_nullsAnswersBeforeReturning() {
            GapFillQuestion q = new GapFillQuestion();
            q.setMcId(2L);
            q.setTypeOfContent(TypeOfContent.GAPFILL);
            q.setAnswers(new ArrayList<>(List.of(new GapFillAnswer())));
            when(moduleContentRepository.findById(2L)).thenReturn(Optional.of(q));

            ModuleContent result = sessionService.getQuestionById(2L);

            assertThat(((GapFillQuestion) result).getAnswers()).isNull();
        }

        @Test
        void unrecognizedType_throwsModuleContentNotFound() {
            ModuleContent q = aQuestion(3L, TypeOfContent.READING);
            when(moduleContentRepository.findById(3L)).thenReturn(Optional.of(q));

            assertThrows(com.jpd.web.exception.ModuleContentNotFoundException.class,
                    () -> sessionService.getQuestionById(3L));
        }
    }

    @Nested
    class StartNextQuestion {

        @Test
        void completesQuiz_whenNoMoreQuestions_throwsQuizCompletedExceptionWithSortedLeaderboard() throws Exception {
            SessionInfo session = SessionInfo.builder().sessionCode("ABC123")
                    .currentQuestionIndex(0)
                    .questionIds(List.of(1L)).build();
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get("quiz:session:ABC123")).thenReturn(toJson(session));
            when(stringRedisTemplate.opsForSet()).thenReturn(setOps);
            when(setOps.members("quiz:session:ABC123:participants")).thenReturn(java.util.Set.of("p1", "p2"));
            ParticipantInfo low = ParticipantInfo.builder().participantId("p1").currentScore(100).build();
            ParticipantInfo high = ParticipantInfo.builder().participantId("p2").currentScore(500).build();
            when(valueOps.get("quiz:session:ABC123:participant:p1")).thenReturn(toJson(low));
            when(valueOps.get("quiz:session:ABC123:participant:p2")).thenReturn(toJson(high));

            QuizCompletedException ex = assertThrows(QuizCompletedException.class,
                    () -> sessionService.startNextQuestion("ABC123"));

            List<ParticipantInfo> leaderboard = ex.getFinalLeaderboard();
            assertThat(leaderboard).extracting(ParticipantInfo::getParticipantId).containsExactly("p2", "p1");
        }

        @Test
        void startsNextQuestion_setsThirtySecondTimeLimit_andAcceptingAnswersTrue() throws Exception {
            SessionInfo session = SessionInfo.builder().sessionCode("ABC123")
                    .currentQuestionIndex(-1)
                    .questionIds(List.of(1L))
                    .totalQuestions(1).build();
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get("quiz:session:ABC123")).thenReturn(toJson(session));
            ModuleContent question = aQuestion(1L, TypeOfContent.MULTIPLE_CHOICE);
            ((MultipleChoiceQuestion) question).setOptions(new ArrayList<>());
            when(moduleContentRepository.findById(1L)).thenReturn(Optional.of(question));

            StartQuestionResponse response = sessionService.startNextQuestion("ABC123");

            assertThat(response.getTimeLimit()).isEqualTo(30);
            assertThat(response.getQuestionNumber()).isEqualTo(1);
            verify(valueOps).set(eq("quiz:session:ABC123"), any(String.class), eq(3L), eq(TimeUnit.HOURS));
        }
    }
}
