# E-Learning Platform - CURRENT ARCHITECTURE DOCUMENTATION

**Project**: JPDweb E-Learning Management System  
**Architecture Type**: Monolithic Spring Boot Application  
**Last Updated**: May 18, 2026  
**Status**: ✅ Active & Well-Documented

---

## 📐 High-Level Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    CLIENT LAYER                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   Web App    │  │  Mobile App  │  │   Admin      │          │
│  │  (React/Vue) │  │   (Mobile)   │  │  Dashboard   │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
└─────────┼──────────────────┼──────────────────┼─────────────────┘
          │ HTTPS/WebSocket  │                  │
          └──────────────────┼──────────────────┘
                             │
         ┌───────────────────▼───────────────────┐
         │    NGINX Reverse Proxy                │
         │  (Load Balancing, SSL Termination)   │
         └───────────────────┬───────────────────┘
                             │
         ┌───────────────────▼───────────────────────────┐
         │         API Gateway (Spring Boot)             │
         │  ┌──────────────────────────────────────────┐ │
         │  │  Security Filters & Authentication       │ │
         │  │  (Keycloak JWT Validation)               │ │
         │  └──────────────────────────────────────────┘ │
         └───────────────────┬───────────────────────────┘
                             │
    ┌────────────────────────┼────────────────────────┐
    │                        │                        │
    ▼                        ▼                        ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────────┐
│   REST API   │    │   WebSocket  │    │  Actuator/Health │
│  Controllers │    │   Endpoints  │    │   Monitoring     │
└──────────────┘    └──────────────┘    └──────────────────┘
    │                        │
    │                        ▼
    │                ┌────────────────┐
    │                │  Message Broker│
    │                │  (STOMP/WS)    │
    │                └────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────┐
│             SERVICE LAYER                           │
│  ┌────────────────────────────────────────────────┐ │
│  │ • CourseService       • EnrollmentService      │ │
│  │ • CreatorService      • FeedbackService        │ │
│  │ • CustomerService     • AiEvaluateService      │ │
│  │ • AIService           • FileUploadService      │ │
│  │ • KahootService       • OpenAIService          │ │
│  │ • GeminiAiService     • FireBaseService        │ │
│  │ • KeycloakAdminService• ContentModeration      │ │
│  │ • And 10+ more services...                     │ │
│  └────────────────────────────────────────────────┘ │
└────────────────┬─────────────────────────────────────┘
                 │
    ┌────────────┼────────────────────┐
    │            │                    │
    ▼            ▼                    ▼
┌──────────┐ ┌──────────┐    ┌──────────────────┐
│Repository│ │Transform │    │   Validation &   │
│ Layer    │ │ Layer    │    │  Utility Utils   │
│ (24)     │ │ (DTOs)   │    │  Services        │
└──────────┘ └──────────┘    └──────────────────┘
    │
    ▼
┌──────────────────────────────────────────────────────────────┐
│                    DATA ACCESS LAYER                         │
│  ┌──────────────────────────────────────────────────────┐    │
│  │  Spring Data JPA (Hibernate ORM)                    │    │
│  │  • Entity relationships & mapping                   │    │
│  │  • Query methods & custom queries                   │    │
│  │  • Transaction management                          │    │
│  │  • Lazy/Eager loading strategies                    │    │
│  └──────────────────────────────────────────────────────┘    │
└────────────┬──────────────────────────────────────────────────┘
             │
   ┌─────────┼──────────┬────────────┐
   │         │          │            │
   ▼         ▼          ▼            ▼
┌──────┐ ┌────────┐ ┌──────┐ ┌────────────┐
│MySQL │ │ Redis  │ │Files │ │ External   │
│  DB  │ │ Cache  │ │      │ │  Services  │
└──────┘ └────────┘ └──────┘ └────────────┘
                                    │
                    ┌───────────────┼─────────────┐
                    │               │             │
                    ▼               ▼             ▼
            ┌─────────────┐  ┌────────────┐ ┌──────────┐
            │  Keycloak   │  │  Firebase  │ │ OpenAI   │
            │  (OAuth2)   │  │  Storage   │ │ / Gemini │
            └─────────────┘  └────────────┘ └──────────┘
```

---

## 🏛️ Layered Architecture Pattern

### 1. **Controller Layer** (REST API)

Located in: `src/main/java/com/jpd/web/controller/`

#### Controllers by Functional Area

**Customer Controllers** (`controller/customer/`):
```java
├── AIEvaluateController
│   ├── POST /api/customer/evaluate/speaking
│   ├── POST /api/customer/evaluate/writing
│   └── GET  /api/customer/evaluation/results/{id}
│
├── CommentController
│   ├── POST /api/customer/comments
│   └── GET  /api/customer/courses/{id}/comments
│
├── CourseLearningController
│   ├── GET  /api/customer/courses
│   ├── GET  /api/customer/courses/{id}
│   └── GET  /api/customer/my-courses
│
├── DictionaryController
│   └── GET  /api/customer/dictionary
│
├── EnrollmentController
│   ├── POST /api/customer/courses/{id}/enroll
│   └── GET  /api/customer/enrollments
│
├── FeedbackController
│   ├── POST /api/customer/feedback
│   └── GET  /api/customer/feedback
│
├── WishlistController
│   ├── POST /api/customer/wishlist/{courseId}
│   └── GET  /api/customer/wishlist
│
├── QuizWebSocketController (WebSocket)
│   ├── /ws/kahoot
│   ├── /app/quiz/{sessionCode}/answer
│   └── /user/queue/quiz/updates
│
└── TtsController
    └── GET  /api/customer/tts
```

**Creator Controllers** (`controller/creator/`):
```java
├── CourseController
│   ├── POST   /api/creator/courses (Create)
│   ├── GET    /api/creator/courses (List)
│   ├── PUT    /api/creator/courses/{id}
│   ├── DELETE /api/creator/courses/{id}
│   └── GET    /api/creator/courses/{id}
│
├── ChapterController
│   ├── POST   /api/creator/chapters
│   ├── PUT    /api/creator/chapters/{id}
│   └── DELETE /api/creator/chapters/{id}
│
├── ModuleController
│   ├── POST   /api/creator/modules
│   ├── PUT    /api/creator/modules/{id}
│   └── DELETE /api/creator/modules/{id}
│
├── ModuleContentController
│   ├── POST   /api/creator/content
│   ├── PUT    /api/creator/content/{id}
│   ├── DELETE /api/creator/content/{id}
│   └── POST   /api/creator/content/reorder
│
├── KahootController
│   ├── POST   /api/creator/kahoot
│   ├── PUT    /api/creator/kahoot/{id}
│   ├── POST   /api/creator/kahoot/{id}/start
│   └── POST   /api/creator/kahoot/{id}/end
│
├── FileUploadController
│   ├── POST   /api/creator/upload/image
│   ├── POST   /api/creator/upload/video
│   ├── POST   /api/creator/upload/document
│   └── GET    /api/creator/media-capacity
│
├── AiGenerateController
│   ├── POST   /api/creator/AI/generate-feedback
│   └── GET    /api/creator/AI/quota
│
├── CreatorController
│   ├── GET    /api/creator/dashboard
│   ├── GET    /api/creator/profile
│   └── PUT    /api/creator/profile
│
└── EnrollmentCreatorController
    └── GET    /api/creator/courses/{id}/enrollments
```

**Admin Controllers** (`controller/admin/`):
```java
├── AdminDashboardOverviewController
│   ├── GET /api/admin/dashboard/overview
│   ├── GET /api/admin/dashboard/metrics
│   ├── GET /api/admin/dashboard/activities
│   └── GET /api/admin/dashboard/top-creators
│
├── AdminCourseController
│   ├── GET /api/admin/courses
│   ├── GET /api/admin/courses/{id}
│   ├── PUT /api/admin/courses/{id}/ban
│   └── PUT /api/admin/courses/{id}/unban
│
└── AdminCreatorController
    ├── GET    /api/admin/creators
    ├── GET    /api/admin/creators/{id}
    ├── PUT    /api/admin/creators/{id}/suspend
    ├── PUT    /api/admin/creators/{id}/warn
    └── GET    /api/admin/creators/{id}/warnings
```

### 2. **Service Layer** (Business Logic)

Located in: `src/main/java/com/jpd/web/service/`

#### Core Services Architecture

**Domain Services**:
```java
CourseService
├── Create course with validation
├── Update course metadata
├── Delete course (with cascade)
├── Search & filter courses
├── Get course statistics
└── Manage course access modes

CreatorService
├── Create creator profile
├── Update creator information
├── Manage creator media capacity
├── Track creator reputation
└── Handle creator suspension/warning

CustomerService
├── Manage customer enrollment
├── Track learning progress
├── Update customer profile
└── Manage wishlist items

EnrollmentService
├── Handle course enrollment
├── Track enrollment status
├── Generate enrollment reports
└── Manage completion tracking

ModuleService
├── Create/manage modules
├── Organize module content
├── Track module completion
└── Manage module ordering

ModuleContentService
├── Handle content lifecycle
├── Support 12+ content types
├── Track content access
└── Manage content moderation
```

**AI & Evaluation Services**:
```java
AIService
├── Generate feedback on student work
├── Orchestrate AI model calls
├── Store evaluation results
└── Manage AI quota per creator

AiEvaluateService
├── Evaluate speaking (pronunciation)
├── Analyze semantic similarity
├── Compare student answer with expected
└── Generate pronunciation feedback

OpenAIService
├── Speech-to-Text conversion
├── Text embedding generation
├── Semantic similarity calculation
└── Handle OpenAI API integration

GeminiAiService
├── Content generation via Gemini
├── Feedback generation
├── Text enhancement
└── Handle Gemini API integration
```

**Real-time & Communication Services**:
```java
QuizSessionController (WebSocket)
├── Handle quiz session creation
├── Manage real-time participant updates
├── Deliver questions to participants
├── Collect and score answers
├── Calculate leaderboard rankings
└── Broadcast session events

KahootService
├── Create Kahoot quiz sessions
├── Manage quiz content
├── Configure quiz settings
├── Track quiz analytics
└── Handle session lifecycle
```

**File & Media Services**:
```java
FileUploadService
├── Validate file types & sizes
├── Check creator media capacity
├── Coordinate with Firebase
├── Generate file URLs
├── Clean up pending uploads
└── Handle moderation queue

FireBaseService
├── Upload to Firebase Cloud Storage
├── Generate download URLs
├── Manage file lifecycle
├── Handle authentication
└── Coordinate with GCS SDK
```

**Moderation & Content Services**:
```java
ContentModerationService
├── Filter comments for toxicity
├── Check for profanity
├── Validate user-generated content
├── Call Python moderation service
└── Cache blacklist words

CommentFilterService
├── Load blacklist words
├── Calculate toxicity scores
├── Filter profanity
└── Support multiple languages

FeedbackService
├── Collect student feedback
├── Store feedback in DB
├── Enforce rate limiting
├── Moderation integration
└── Generate feedback reports
```

**Admin & System Services**:
```java
AdminDashboardService
├── Aggregate system metrics
├── Generate dashboard data
├── Track platform statistics
├── Compile activity feed
└── Calculate KPIs

AdminCreatorService
├── Manage creator suspensions
├── Issue warnings
├── Track creator violations
├── Handle banning logic
└── Update reputation scores

KeycloakAdminService
├── Communicate with Keycloak
├── Assign roles to users
├── Create users
├── Manage realm configuration
└── Handle OAuth2 integration
```

**Utility & Helper Services**:
```java
ValidationResources
├── Validate user roles
├── Verify ownership
├── Check course access
├── Enforce business rules
└── Raise appropriate exceptions

CourseMetricsHelper
├── Track course views
├── Count enrollments
├── Calculate ratings
├── Track reports
└── Update statistics

LanguageConverter
├── Convert language codes
├── Validate supported languages
├── Handle language enums
└── Support multi-language UI
```

### 3. **Repository Layer** (Data Access)

Located in: `src/main/java/com/jpd/web/repository/`

#### Repository Pattern Implementation

**Course-Related Repositories**:
```java
CourseRepository extends JpaRepository<Course, Long>
├── findByCreatorId(String creatorId)
├── findByLanguage(Language language)
├── findByCourseIdAndCreatorId(long courseId, String creatorId)
├── findByNameContainingIgnoreCase(String name)
└── Custom query methods for search

ChapterRepository extends JpaRepository<Chapter, Long>
├── findByCourseId(long courseId)
└── findByOrderInChapter(int order)

ModuleRepository extends JpaRepository<Module, Long>
├── findByChapterId(long chapterId)
└── findByOrderInChapter(int order)

ModuleContentRepository extends JpaRepository<ModuleContent, Long>
├── findByModuleId(long moduleId)
├── findByTypeOfContent(TypeOfContent type)
└── Custom polymorphic queries
```

**User & Enrollment Repositories**:
```java
EnrollmentRepository extends JpaRepository<Enrollment, Long>
├── findByCourseId(long courseId)
├── findByCustomerId(String customerId)
├── findByCourseIdAndCustomerId(long courseId, String customerId)
└── findByIsFinish(boolean isFinish)

CreatorRepository extends JpaRepository<Creator, String>
├── findByStatus(Status status)
├── findBanIsTrue()
└── findByWarningCountGreaterThan(int count)

WishlistRepository extends JpaRepository<Wishlist, Long>
├── findByCustomerIdAndCourseId(String customerId, long courseId)
└── findByCustomerId(String customerId)
```

**Evaluation & Learning Repositories**:
```java
SemanticDataRepository extends JpaRepository<SemanticData, Long>
├── findByCustomerId(String customerId)
├── countTodayByCustomerId(String customerId, LocalDateTime start, LocalDateTime end)
└── findRecentResults(String customerId, int limit)

WritingResultRepository extends JpaRepository<WritingResult, Long>
├── findByCustomerId(String customerId)
└── findTopByOrderByCreatedAtDesc()

UserFeedbackUsageRepository
├── Track feedback generation usage
└── Enforce usage quotas

RememberWordRepository
├── Track vocabulary learning
└── Manage word retention
```

**Moderation & Reporting Repositories**:
```java
ReportRepository extends JpaRepository<Report, Long>
├── findByCourseId(long courseId)
├── findByStatus(String status)
└── findUnresolvedReports()

CommentRepository extends JpaRepository<Comment, Long>
├── findByCourseId(long courseId)
├── findByCustomerId(String customerId)
└── findPendingModeration()

PendingImgRepository extends JpaRepository<PendingImage, Long>
├── findByStatus(Status status)
└── findOldestPending()
```

**Audit & Metrics Repositories**:
```java
AuditLogRepository extends JpaRepository<AuditLog, Long>
├── findByUserId(String userId)
├── findByDateRange(LocalDateTime start, LocalDateTime end)
└── findByActionType(String actionType)

CourseMetricsRepository extends JpaRepository<CourseMetrics, Long>
├── findByCourseId(long courseId)
└── Update metrics incrementally

CreatorWarningRepository
├── Track creator warnings
└── findByCreatorId(String creatorId)
```

### 4. **Data Model Layer** (Entities)

Located in: `src/main/java/com/jpd/web/model/`

#### Entity Relationships Diagram

```
User Entities:
├── Creator (Educator)
│   ├── 1:N → Course
│   ├── 1:N → KahootListFunction
│   ├── 1:N → CreatorWarning
│   └── 1:N → CreatorMediaCapacity
│
└── Customer (Learner)
    ├── 1:N → Enrollment
    ├── 1:N → SemanticData (Speech evaluation)
    ├── 1:N → WritingResult
    ├── 1:N → Feedback
    ├── 1:N → Comment
    ├── 1:N → Report
    ├── 1:N → Wishlist
    └── 1:N → RememberWord

Course Content:
├── Course (1:N) → Chapter
│   ├── Chapter (1:N) → Module
│   │   └── Module (1:N) → ModuleContent (polymorphic)
│   │       ├── TeachingVideo
│   │       ├── Passage
│   │       ├── PdfDocument
│   │       ├── FlashCard
│   │       ├── MultipleChoiceQuestion
│   │       ├── ReadingQuestion
│   │       ├── ListeningChoiceQuestion
│   │       ├── SpeakingQuestion (Picture/Passage)
│   │       ├── WritingQuestion
│   │       ├── GapFillQuestion
│   │       └── RememberWord
│   ├── Course (1:N) → Enrollment
│   ├── Course (1:N) → Report
│   ├── Course (1:N) → Comment
│   ├── Course (1:N) → Feedback
│   ├── Course (1:N) → Wishlist
│   └── Course (1:1) → CourseMetrics

Assessment & Evaluation:
├── Enrollment
│   ├── 1:N → CustomerModuleContent (Progress)
│   └── 1:N → Feedback & Evaluation results
│
├── SemanticData (Speech evaluation results)
│   └── Linked to ModuleContent (speaking task)
│
└── WritingResult (Essay evaluation)
    └── Linked to ModuleContent (writing task)

Quiz System:
├── Kahoot Session
│   ├── 1:N → Questions
│   └── 1:N → Participants
│
└── SessionInfo (Real-time session state)
    ├── Current question tracking
    ├── Participant leaderboard
    └── Session progress

Moderation & Reporting:
├── Report
│   ├── N:1 → Course (reported course)
│   └── N:1 → Customer (reporter)
│
├── Comment
│   ├── N:1 → Course
│   ├── N:1 → Customer
│   └── Content moderation status
│
├── PendingImage
│   └── Awaiting moderation
│
├── BlacklistWord
│   └── For content filtering
│
└── AuditLog
    └── Track all actions
```

#### Key Entity Features

**Temporal Tracking**:
```java
@CreationTimestamp
private LocalDate createdAt;

@UpdateTimestamp
private LocalDate lastUpdate;

// Used across: Course, Chapter, Module, 
// Comment, Enrollment, Feedback, Report
```

**Polymorphic Content**:
```java
@Enumerated(EnumType.STRING)
@Column(name = "type_of_content")
private TypeOfContent typeOfContent;
// Discriminator column for single table inheritance

// 12 different concrete types supported
// Single ModuleContent table with type column
```

**Relationships Configuration**:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "creator_id")
private Creator creator;
// Lazy loading to improve performance

@OneToMany(cascade = CascadeType.ALL, mappedBy = "course")
@JsonManagedReference
private List<Chapter> chapters;
// Cascade delete, circular reference handling
```

### 5. **Transform/DTO Layer** (Data Transfer)

Located in: `src/main/java/com/jpd/web/transform/` and `src/main/java/com/jpd/web/dto/`

#### Purpose & Implementation

**DTOs (Data Transfer Objects)**:
```java
CourseLearningCardDto
├── Used for: Displaying course cards in learning list
├── Fields: courseId, name, progress, lastAccessed, etc.
└── Transformation: Course entity → DTO

CourseCardDto
├── Used for: Course listing/search results
├── Fields: courseId, name, creator, rating, price
└── Transformation: Course entity → DTO

CourseDescriptionDto
├── Used for: Full course detail page
├── Fields: All course info + chapters + feedback
└── Transformation: Course entity with related data → DTO

CreatorDashboardDTO
├── Used for: Creator dashboard display
├── Fields: metrics, revenue, students, courses
└── Transformation: Creator entity + aggregated data → DTO

AdminDashboardOverviewResponse
├── Used for: Admin dashboard
├── Fields: metrics, activities, top creators
└── Transformation: Multiple entities → aggregated DTO

WritingScores
├── Used for: Writing evaluation results
├── Fields: grammar, vocabulary, feedback scores
└── Transformation: WritingResult entity → DTO

AnswerResult
├── Used for: Quiz answer feedback
├── Fields: correct, pointsEarned, totalScore
└── Transformation: Quiz result → DTO
```

**Transform/Mapper Classes**:
```java
CourseTransForm
├── toCourseCardDto(Course)
├── toCourseDescriptionDto(Course)
├── toCourseInfDto(Course)
└── toEntity(CourseFormDto)

CreatorTransform
├── transToCreatorDto(Creator)
├── transformFromCreatorDto(CreatorProfileDto)
└── CreatorDashboardDTO(Creator + metrics)

ModuleContentTransform
├── extractRawContent(ModuleContent)
├── Handle 12+ content types
└── toDto(ModuleContent)

ReportTransform
├── transToReport(ReportForm, Course, customerId)
└── toDto(Report)

RememberTransform
├── Transform Remember entities
└── Handle token-based remember-me
```

---

## 🗄️ Database Schema Overview

### Database Structure

```
Database: elearning_db
Tables: 28+
Total Records: ~100,000+ (production)

Core Table Groups:

1. User Management Tables
   ├── creator (educators)
   ├── creator_media_capacity
   ├── creator_warning
   ├── creator_codes_change_p
   ├── creator_certificates (collection table)
   └── customer (inferred from imports)

2. Course Content Tables
   ├── course
   ├── chapter
   ├── module
   ├── module_content (polymorphic - 12 types)
   ├── teaching_video
   ├── passage
   ├── pdf_document
   ├── flashcard
   ├── multiple_choice_question
   ├── multiple_choice_option
   ├── reading_question
   ├── reading_question_options
   ├── listening_choice_question
   ├── listening_choice_option
   ├── speaking_picture_question
   ├── speaking_picture_list_questions
   ├── speaking_passage_question
   ├── writing_question
   ├── gapfill_question
   ├── gapfill_answer
   └── remember_word

3. Learning & Enrollment Tables
   ├── enrollment
   ├── customer_module_content (progress tracking)
   ├── semantic_data (speech evaluation results)
   ├── semantic_result (comparison results)
   ├── writing_result (essay evaluation)
   └── user_feedback_usage (quota tracking)

4. Quiz & Interactive Tables
   ├── kahoot_list_function
   └── session_info (in-memory or Redis)

5. Community & Moderation Tables
   ├── comment
   ├── feedback
   ├── report
   ├── wishlist
   ├── pending_image
   ├── blacklist_word
   └── content_moderation_result (inferred)

6. Admin & Audit Tables
   ├── audit_log
   ├── access_mode (enum)
   ├── course_metrics
   └── creator_request_number

Key Indexing Strategy:
├── Index on creator_id (frequent filtering)
├── Index on course_id (course lookups)
├── Index on chapter_id (content hierarchy)
├── Index on enrollment dates
├── Composite indexes on frequently joined columns
└── Full-text indexes on course name/description
```

### Database Connection Management

```java
Configuration (application.properties):
spring.datasource.url=jdbc:mysql://localhost:3306/jpdweb
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.properties.hibernate.connection.characterEncoding=utf8mb4

Connection Pooling:
- HikariCP (default in Spring Boot)
- Connection timeout: configured
- Idle timeout & max lifetime
- Min/max pool size optimization
```

---

## 🔐 Security Architecture

### Authentication Flow

```
┌────────────────────────┐
│   User Login Request   │
└────────────┬───────────┘
             │ email + password
             ▼
┌────────────────────────────────────────┐
│  Keycloak Authentication Server        │
│  ├── Validate credentials              │
│  ├── Generate JWT token                │
│  └── Return access + refresh tokens    │
└────────────┬───────────────────────────┘
             │
             ▼
┌────────────────────────────────────────┐
│   Client stores JWT in cookie/storage  │
└────────────┬───────────────────────────┘
             │
             ▼
┌────────────────────────────────────────┐
│   Subsequent API requests              │
│   Include: Authorization: Bearer JWT   │
└────────────┬───────────────────────────┘
             │
             ▼
┌────────────────────────────────────────┐
│  CookieAuthenticationFilter            │
│  ├── Extract JWT from request          │
│  ├── Validate JWT signature            │
│  ├── Check token expiration            │
│  ├── Extract user claims               │
│  └── Build security context            │
└────────────┬───────────────────────────┘
             │
             ▼
┌────────────────────────────────────────┐
│  JwtRoleConverted                      │
│  ├── Extract roles from JWT            │
│  ├── Convert to GrantedAuthority       │
│  └── Filter Keycloak system roles      │
└────────────┬───────────────────────────┘
             │
             ▼
┌────────────────────────────────────────┐
│  SecurityContextHolder                 │
│  └── Store authentication for request  │
└────────────┬───────────────────────────┘
             │ @AuthenticationPrincipal Jwt jwt
             ▼
┌────────────────────────────────────────┐
│  Controller / Service                  │
│  ├── Access jwt.getSubject()           │
│  ├── Check jwt.getClaimAsStringList()  │
│  └── Enforce role-based access        │
└────────────────────────────────────────┘
```

### Authorization Strategy

```java
Role-Based Access Control (RBAC)

Roles:
├── CUSTOMER
│   ├── Can view courses
│   ├── Can enroll in courses
│   ├── Can submit assignments
│   └── Can participate in quizzes
│
├── CREATOR
│   ├── Can create courses
│   ├── Can manage content
│   ├── Can see student enrollments
│   └── Can use AI features
│
└── ADMIN
    ├── Full platform access
    ├── Can view all data
    ├── Can moderate content
    └── Can suspend users

Enforcement:
- @AuthenticationPrincipal Jwt jwt in controllers
- Extract roles from JWT claims
- Check roles against required permissions
- Raise UnauthorizedException if denied
```

### Data Validation

```java
Input Validation Points:
├── DTO validation (javax.validation annotations)
│   ├── @NotNull, @NotBlank
│   ├── @Size, @Min, @Max
│   ├── @Email, @Pattern
│   └── Custom validators
│
├── Business Rule Validation (Service layer)
│   ├── Ownership verification
│   ├── Status checks
│   ├── Capacity validation
│   └── Duplicate checking
│
└── SQL Injection Prevention
    ├── Parameterized queries (JPA)
    ├── No dynamic SQL concatenation
    └── Input sanitization
```

---

## 🔄 Data Flow Patterns

### 1. **Course Enrollment Flow**

```
1. Customer searches for courses
   └─→ CourseLearningController.searchCourses()
       └─→ CourseInfService.searchByKey()
           └─→ CourseRepository.findByNameContainingIgnoreCase()
               ├─→ MySQL query with pagination
               └─→ Transform to CourseInfDto
                   └─→ Return to client

2. Customer views course details
   └─→ CourseLearningController.getCourseDescription()
       └─→ CourseInfService.getCourseDescription()
           ├─→ Get course from CourseRepository
           ├─→ Get chapters (with modules)
           ├─→ Get enrollments & feedbacks
           ├─→ Calculate metrics
           └─→ Transform to CourseDescriptionDto

3. Customer enrolls in course
   └─→ EnrollmentController.enrollCourse()
       ├─→ Validate user has access
       ├─→ Validate course exists
       ├─→ EnrollmentService.handleEnrollCourse()
       │   ├─→ Create Enrollment entity
       │   ├─→ Update CourseMetrics
       │   ├─→ Save to EnrollmentRepository
       │   └─→ Trigger event/notification
       └─→ Return success response

4. Customer accesses learning content
   └─→ CourseLearningController.getYourCourse()
       └─→ CustomerService.retrieveLearningList()
           ├─→ Query enrollments by customerId
           ├─→ Get course content hierarchy
           ├─→ Fetch user progress
           └─→ Transform & return CourseLearningCardDto
```

### 2. **AI Evaluation Flow**

```
Student Submission (Speaking):
│
└─→ AIEvaluateController.evaluateSpeaking()
    ├─→ Extract audio file & parameters
    ├─→ Validate inputs
    │
    ├─→ AiEvaluateService.evaluateSpeaking()
    │   ├─→ Check rate limiting (10/day)
    │   ├─→ OpenAIService.speechToText(audio)
    │   │   ├─→ Call OpenAI Whisper API
    │   │   └─→ Return transcribed text
    │   │
    │   ├─→ OpenAIService.compareSemanticWithEmbedding()
    │   │   ├─→ Get embedding of transcribed text
    │   │   ├─→ Get embedding of expected answer
    │   │   ├─→ Calculate cosine similarity
    │   │   └─→ Generate feedback based on similarity
    │   │
    │   ├─→ Create SemanticData entity
    │   ├─→ Save to SemanticDataRepository
    │   └─→ Return SemanticResult DTO
    │
    └─→ Return evaluation to client

Student Submission (Writing):
│
└─→ AIEvaluateController.evaluateWriting() or
    AiGenerateController.generateFeedback()
    │
    ├─→ AIService.generateFeedback(question, answer)
    │   ├─→ Build LLM prompt
    │   ├─→ Call GeminiAiService.generateText()
    │   │   ├─→ Call Gemini API
    │   │   ├─→ Parse JSON response
    │   │   └─→ Extract scores & feedback
    │   │
    │   ├─→ Save to WritingResultRepository
    │   └─→ Return WritingScores DTO
    │
    └─→ Return evaluation to client
```

### 3. **Real-time Quiz Session Flow**

```
Kahoot Quiz Session:

1. Instructor Starts Session
   └─→ KahootController.startQuiz()
       ├─→ Create SessionInfo (in Redis)
       ├─→ Generate sessionCode
       └─→ Broadcast start event via WebSocket

2. Student Joins Session
   └─→ QuizWebSocketController.joinSession()
       ├─→ Validate sessionCode
       ├─→ Add to session participants
       ├─→ Send first question
       └─→ Update leaderboard

3. Questions Delivered in Real-time
   └─→ /app/quiz/{sessionCode}/next-question
       ├─→ Get next question from KahootService
       ├─→ Broadcast to all connected participants
       ├─→ Start timer
       └─→ Collect answers as they arrive

4. Students Submit Answers
   └─→ /app/quiz/{sessionCode}/answer
       ├─→ Validate answer
       ├─→ Calculate points
       ├─→ Update session state
       └─→ Broadcast updated leaderboard

5. Session Ends
   └─→ KahootController.endQuiz()
       ├─→ Finalize scores
       ├─→ Save results
       ├─→ Broadcast final leaderboard
       └─→ Clean up session from Redis

WebSocket Messages:
├── /topic/quiz/{sessionCode}/updates (broadcast)
├── /user/queue/quiz/personal (personal messages)
└── Message format: JSON with question, options, leaderboard
```

### 4. **Content Moderation Flow**

```
Comment Submission:
│
└─→ CommentController.postComment()
    ├─→ Extract comment content
    │
    ├─→ ContentModerationService.moderateComment()
    │   ├─→ CommentFilterService.filterToxicity()
    │   │   ├─→ Load blacklist from cache
    │   │   ├─→ Search for toxic words
    │   │   ├─→ Calculate toxicity score
    │   │   └─→ Return score & filtered text
    │   │
    │   ├─→ Call Python moderation service
    │   │   (http://localhost:5000/moderate)
    │   │
    │   └─→ Return moderation result
    │
    ├─→ If APPROVED:
    │   ├─→ Save Comment entity
    │   └─→ Update CourseMetrics
    │
    └─→ If REJECTED:
        ├─→ Notify user
        └─→ Request human review

Image Upload Moderation:
│
└─→ FileUploadService.uploadImage()
    ├─→ Validate file (type, size)
    ├─→ Check creator media capacity
    ├─→ Upload to Firebase
    │
    ├─→ Create PendingImage entity
    ├─→ Queue for moderation
    │
    └─→ Admin reviews via AdminDashboard
        ├─→ Approve → Use in platform
        └─→ Reject → Delete from Firebase
```

---

## 🔌 External Service Integration

### OpenAI Integration

```
Configuration:
├── API Key: stored in environment variables
├── Endpoint: https://api.openai.com/v1/
└── Models: whisper-1, text-embedding-3-small, gpt-4

Usage Points:

1. Speech-to-Text (Whisper)
   ├── Input: Audio file (MP3, WAV, M4A)
   ├── Process: OpenAIService.speechToText()
   ├── Response: Transcribed text
   └── Use: Pronunciation evaluation

2. Text Embeddings
   ├── Input: Text string
   ├── Process: OpenAIService.getTextEmbedding()
   ├── Response: 1536-dimensional vector
   └── Use: Semantic similarity calculation

3. Semantic Similarity
   ├── Input: Two texts
   ├── Process: Compare embeddings (cosine similarity)
   ├── Response: Similarity score (0-1)
   └── Use: Speech evaluation feedback

Error Handling:
├── Rate limiting (429): Retry with backoff
├── Invalid API key: Log error, alert admin
├── Network timeout: Retry mechanism
└── Fallback: Graceful degradation
```

### Gemini Integration

```
Configuration:
├── API Key: stored in environment variables
├── Endpoint: https://generativelanguage.googleapis.com/v1beta/
└── Model: gemini-2.5-flash

Usage Points:

1. Content Generation
   ├── Input: Topic, level, parameters
   ├── Process: Build prompt → Call API
   ├── Response: Generated content
   └── Use: Creator content suggestions

2. Feedback Generation
   ├── Input: Student answer, question, language
   ├── Process: Build evaluation prompt → Call API
   ├── Response: Structured feedback JSON
   └── Use: Writing evaluation feedback

3. Question Suggestions
   ├── Input: Topic, content area
   ├── Process: Generate questions
   ├── Response: Multiple questions
   └── Use: Quiz content generation

Error Handling:
├── Timeout handling: Configurable timeouts
├── JSON parse errors: Validation & fallback
├── Usage quota: Track per creator
└── API changes: Version management
```

### Firebase Integration

```
Configuration:
├── Service Account: firebase-service-account.json
├── Storage Bucket: jpdweb-9d3d3.firebasestorage.app
└── Region: configured in FireBaseConfig

Usage Points:

1. Upload Files
   ├── Process: FileUploadService.uploadImage()
   ├── Destination: gs://bucket/path/file.ext
   ├── Response: Download URL
   └── Use: Course media storage

2. Generate URLs
   ├── Process: FireBaseService.getDownloadUrl()
   ├── Response: Long-lived HTTPS URL
   └── Use: Content delivery

3. File Cleanup
   ├── Process: Delete rejected/old files
   ├── Target: gs://bucket/pending/...
   └── Trigger: Moderation rejection

Configuration:
- Init: FireBaseConfig.init() @PostConstruct
- Bean: StorageClient created via FirebaseApp
- Storage: gs:// URLs for all uploads
```

### Keycloak Integration

```
Configuration:
├── Server URL: http://localhost:8080
├── Realm: jpdweb
├── Admin Client: backend-service
└── Client Secret: from environment

Usage Points:

1. User Authentication
   ├── OIDC protocol
   ├── JWT token validation
   └── Role extraction from claims

2. Role Assignment
   ├── Process: KeycloakAdminService.assignClientRoleToUser()
   ├── Target: User in Keycloak
   ├── Role: CREATOR, CUSTOMER, ADMIN
   └── Trigger: User signup/promotion

3. User Provisioning
   ├── Create user account
   ├── Set initial password
   ├── Assign roles
   └── Enable user

Security:
├── Client credentials OAuth2 flow
├── JWT RSA signature validation
├── Token expiration enforcement
└── Role-based access control
```

### Content Moderation Service

```
External Python Service Integration:
├── Host: localhost
├── Port: 5000
├── Endpoint: POST /moderate
├── Protocol: HTTP REST

Request Format:
{
  "text": "comment text",
  "language": "en",
  "type": "comment"
}

Response Format:
{
  "status": "approved|rejected|flagged",
  "score": 0.85,
  "reason": "spam|hate|inappropriate|ok",
  "confidence": 0.92
}

Usage:
├── Called from ContentModerationService
├── Cached results for performance
├── Fallback to simple filtering
└── Rate limiting implemented
```

---

## 🎯 Request/Response Patterns

### Standard API Response Format

```java
// Success Response
{
  "data": {
    "courseId": 123,
    "name": "English Course",
    ...
  },
  "message": "Course retrieved successfully",
  "status": 200,
  "timestamp": "2026-05-18T10:30:00Z"
}

// Error Response
{
  "error": {
    "code": "COURSE_NOT_FOUND",
    "message": "The requested course does not exist",
    "details": "Course ID: 999"
  },
  "status": 404,
  "timestamp": "2026-05-18T10:30:00Z"
}
```

### WebSocket Message Format (Kahoot)

```java
// Question Broadcast
{
  "type": "QUESTION",
  "sessionCode": "ABC123",
  "questionIndex": 1,
  "questionId": 456,
  "questionText": "What is...?",
  "options": ["A", "B", "C", "D"],
  "timeLimit": 30,
  "timestamp": "2026-05-18T10:30:00Z"
}

// Answer Submission
{
  "type": "ANSWER",
  "sessionCode": "ABC123",
  "studentId": "student1",
  "questionIndex": 1,
  "selectedOption": 2,
  "timeSpent": 15
}

// Leaderboard Update
{
  "type": "LEADERBOARD",
  "sessionCode": "ABC123",
  "rankings": [
    {"position": 1, "studentName": "Alice", "score": 100},
    {"position": 2, "studentName": "Bob", "score": 80}
  ],
  "timestamp": "2026-05-18T10:30:15Z"
}
```

---

## 📊 Database Query Patterns

### Common Query Patterns

```sql
-- Get course with all related data
SELECT c.*, 
       COUNT(DISTINCT e.enrollment_id) as enrollment_count,
       AVG(f.rating) as avg_rating
FROM course c
LEFT JOIN enrollment e ON c.course_id = e.course_id
LEFT JOIN feedback f ON c.course_id = f.course_id
WHERE c.course_id = ?
GROUP BY c.course_id;

-- Find courses by search criteria
SELECT DISTINCT c.*
FROM course c
WHERE (c.name LIKE ? 
   OR c.description LIKE ?)
AND c.language = ?
AND c.is_public = true
AND c.is_ban = false
ORDER BY c.created_at DESC
LIMIT ?, ?;

-- Track student progress
SELECT 
    e.enrollment_id,
    COUNT(cmc.content_id) as total_content,
    COUNT(CASE WHEN cmc.is_completed THEN 1 END) as completed_content,
    (COUNT(CASE WHEN cmc.is_completed THEN 1 END) / 
     COUNT(cmc.content_id) * 100) as progress_percentage
FROM enrollment e
LEFT JOIN customer_module_content cmc 
    ON e.enrollment_id = cmc.enrollment_id
WHERE e.enrollment_id = ?
GROUP BY e.enrollment_id;

-- Get top creators by revenue
SELECT c.creator_id, c.full_name, 
       SUM(e.payment_amount) as total_revenue,
       COUNT(DISTINCT e.enrollment_id) as total_students
FROM creator c
JOIN course co ON c.creator_id = co.creator_id
JOIN enrollment e ON co.course_id = e.course_id
WHERE e.payment_status = 'COMPLETED'
AND e.enrollment_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY c.creator_id
ORDER BY total_revenue DESC
LIMIT 10;
```

---

## 🚀 Performance Considerations

### Caching Strategy

```java
Redis Cache Usage:
├── User roles cache
│   ├── Key: "user:{userId}:roles"
│   ├── TTL: 1 hour
│   └── Invalidated on role change
│
├── Course recommendations
│   ├── Key: "recommendations:{userId}"
│   ├── TTL: 24 hours
│   └── Invalidated on enrollment
│
├── Kahoot session state
│   ├── Key: "kahoot:{sessionCode}:state"
│   ├── TTL: Session duration
│   └── Auto-cleanup on end
│
└── JWT token blacklist
    ├── Key: "token:blacklist:{tokenId}"
    ├── TTL: Token expiration time
    └── For logout functionality

In-Memory Caching:
├── Blacklist words
│   ├── Loaded on startup
│   └── Refreshed periodically
│
└── Language configuration
    ├── Supported languages
    └── Language-specific resources
```

### N+1 Query Prevention

```java
Example: Course with all data
PROBLEM:
- Load course (1 query)
- Load chapters (N queries for N courses)
- Load modules (M queries for M chapters)
- Load content (P queries for P modules)
Total: 1 + N + M + P queries ❌

SOLUTION: Eager loading with JOIN FETCH
@Query("""
    SELECT DISTINCT c FROM Course c
    LEFT JOIN FETCH c.chapters ch
    LEFT JOIN FETCH ch.modules m
    WHERE c.courseId = :courseId
""")
Course findByIdWithAllContent(@Param("courseId") long courseId);

Result: Single optimized query with JOINs ✓
```

### Query Optimization

```java
// Pagination for large result sets
Page<Course> findPublicCourses(
    Pageable pageable  // size=20, page=0
);

// Select only needed fields
@Query("""
    SELECT new map(
        c.courseId as id,
        c.name as name,
        c.urlImg as image,
        COUNT(e) as enrollments
    ) FROM Course c
    LEFT JOIN c.enrollments e
    WHERE c.language = :language
    GROUP BY c.courseId
""")
List<Map<String, Object>> getPublicCoursesSummary(
    @Param("language") Language language,
    Pageable pageable
);
```

---

## 🔧 Configuration & Setup

### Spring Boot Configuration

```properties
# Spring Profiles: dev, staging, production
spring.profiles.active=dev

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/elearning_db
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.cache.type=redis

# Mail
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Security & Authentication
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8080/realms/jpdweb/protocol/openid-connect/certs

# File Upload
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB

# Server
server.port=9090
server.servlet.context-path=/api
server.session.timeout=20m
```

### Application Startup

```java
@SpringBootApplication
@EnableScheduling  // Enable scheduled tasks
public class JpDwebSwpApplication {
    public static void main(String[] args) {
        SpringApplication.run(JpDwebSwpApplication.class, args);
    }
}

Startup sequence:
1. Spring context initialization
2. Database schema validation/update
3. Firebase initialization
4. Redis connection established
5. Keycloak client initialization
6. Cache population (blacklist words, etc.)
7. Scheduled tasks registered
8. Application ready on port 9090
```

---

## 📈 Scalability Considerations

### Horizontal Scaling

```
Current Single Instance:
└─→ Spring Boot (port 9090)

Scaled Architecture:
├─→ Load Balancer (Nginx)
│   ├─→ Spring Boot Instance 1 (port 9091)
│   ├─→ Spring Boot Instance 2 (port 9092)
│   └─→ Spring Boot Instance 3 (port 9093)
└─→ Shared Resources:
    ├─→ MySQL Database (replica set)
    ├─→ Redis Cluster
    └─→ Firebase (external)

Sticky Sessions for WebSocket:
- Kahoot sessions routed to same instance
- SessionInfo stored in Redis (shared)
- Fallback to DB if Redis unavailable
```

### Database Scaling

```
Read Optimization:
├── Master-Slave replication
├── Read replicas for SELECT queries
├── Caching layer (Redis)
└── Connection pooling

Write Optimization:
├── Single master for all writes
├── Transaction batching
├── Bulk operations for bulk loads
└── Prepared statements for performance
```

---

## 📞 Monitoring & Debugging

### Actuator Endpoints

```
Available Monitoring Endpoints:
├── /actuator/health              - Application health
├── /actuator/metrics             - Performance metrics
├── /actuator/env                 - Environment variables
├── /actuator/beans               - Spring beans
├── /actuator/threaddump          - Thread dump
├── /actuator/heapdump            - Heap dump
└── /actuator/prometheus          - Prometheus metrics
```

### Logging

```java
// Log files location
logs/application.log

// Configured loggers
logging.level.root=INFO
logging.level.com.jpd.web=DEBUG
logging.level.org.springframework=INFO
logging.level.org.hibernate.SQL=DEBUG

// Typical log usage
@Slf4j
public class SomeService {
    public void someMethod() {
        log.info("Processing user: {}", userId);
        log.debug("Detailed debug information");
        log.error("Error occurred", exception);
    }
}
```

---

**Document Version**: 1.0  
**Last Updated**: May 18, 2026  
**Status**: ✅ Complete & Ready for Reference
