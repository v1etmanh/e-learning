# E-Learning Platform - PROJECT OVERVIEW

**Project Name**: JPDweb SWP - E-Learning Management System  
**Version**: 0.0.1-SNAPSHOT  
**Language**: Java  
**Framework**: Spring Boot 3.5.5  
**Java Version**: 17  
**Status**: Active Development

---

## 📋 Executive Summary

JPDweb is a **comprehensive e-learning platform** designed to connect educators (creators) with learners (customers) through an interactive, multi-language learning environment. The platform provides tools for course creation, AI-powered evaluation, real-time quiz sessions (Kahoot-style), and administrative oversight.

**Primary Use Cases**:
- 🎓 Educators can create and manage multimedia courses
- 📚 Students can learn through interactive content with AI evaluation
- 🎮 Instructors can conduct real-time quiz sessions with students
- 👨‍💼 Admins can monitor platform activity and manage creators
- 🔍 Advanced content search and recommendation system

---

## 🎯 Core Features

### 1. **For Learners (Customers)**
```
├── Course Discovery & Management
│   ├── Browse recommended courses
│   ├── Search by name, language, creator, description
│   ├── View detailed course descriptions
│   ├── Add courses to wishlist
│   └── Enroll in courses (free or paid)
│
├── Interactive Learning
│   ├── Watch teaching videos
│   ├── Study vocabulary (flashcards)
│   ├── Read passages and take comprehension tests
│   ├── Practice gap-fill exercises
│   ├── Listening comprehension exercises
│   └── Speaking practice with AI evaluation
│
├── Real-time Quiz Sessions (Kahoot)
│   ├── Join instructor-hosted quiz sessions
│   ├── Answer multiple-choice questions in real-time
│   ├── View live leaderboard rankings
│   ├── Compete with other students
│   └── Get immediate feedback
│
├── AI-Powered Evaluation
│   ├── Pronunciation analysis (speech-to-text)
│   ├── Writing evaluation with feedback
│   ├── Semantic similarity checking
│   └── Performance scoring
│
├── Personal Learning Tracker
│   ├── Track learning progress
│   ├── View course completion status
│   ├── Access learning history
│   └── Monitor AI evaluation results
│
└── Community Engagement
    ├── Post comments on courses
    ├── Write and read feedback reviews
    ├── Content moderation system
    └── Report inappropriate content
```

### 2. **For Educators (Creators)**
```
├── Course Management
│   ├── Create new courses
│   ├── Set language and teaching language
│   ├── Define learning objectives
│   ├── Set target audience
│   ├── Manage course access (public/private)
│   ├── Create join codes for access
│   └── Upload course thumbnail images
│
├── Content Management
│   ├── Organize content into chapters
│   ├── Create modules within chapters
│   ├── Add multimedia content:
│   │   ├── Teaching videos
│   │   ├── Passages
│   │   ├── PDF documents
│   │   ├── Flashcards
│   │   ├── Multiple-choice questions
│   │   ├── Reading questions with options
│   │   ├── Listening comprehension
│   │   ├── Speaking tasks (picture/passage-based)
│   │   ├── Writing assignments
│   │   └── Gap-fill exercises
│   └── Reorder and organize content
│
├── AI-Powered Content Generation
│   ├── Generate feedback on student answers
│   ├── Request AI assistance for content suggestions
│   ├── AI-powered content generation with quotas
│   └── Track AI usage statistics
│
├── Real-time Quiz Sessions
│   ├── Create Kahoot-style quiz sessions
│   ├── Add questions to quiz pools
│   ├── Configure quiz settings:
│   │   ├── Randomize questions
│   │   ├── Randomize answer options
│   │   └── Display leaderboard settings
│   ├── Start/end quiz sessions
│   ├── Monitor student responses in real-time
│   └── View session analytics
│
├── Student Management
│   ├── View enrolled students
│   ├── Track student progress
│   ├── View student submissions
│   └── Provide feedback
│
├── Financial Management
│   ├── Set course pricing
│   ├── Track revenue
│   ├── Manage withdrawal requests
│   ├── Monitor balance
│   └── View payment history
│
├── Creator Dashboard
│   ├── Overview of key metrics:
│   │   ├── Total revenue
│   │   ├── Total students
│   │   ├── Total courses
│   │   ├── Average rating
│   │   ├── Completion rate
│   │   ├── New enrollments
│   │   └── Total reviews
│   ├── Popular courses list
│   └── Activity logs
│
└── Creator Profile
    ├── Upload profile image
    ├── Add self-description
    ├── Add certificates
    ├── View creator reputation score
    └── Manage contact information
```

### 3. **For Administrators**
```
├── Dashboard & Monitoring
│   ├── System overview (uptime, performance)
│   ├── Key metrics:
│   │   ├── Total users, creators, courses
│   │   ├── Enrollment trends
│   │   ├── Revenue statistics
│   │   ├── Report statistics
│   │   └── Suspension statistics
│   ├── Recent activity feed
│   ├── Top creators ranking
│   ├── HTTP metrics & performance
│   └── Database connection pool status
│
├── User Management
│   ├── View all users (creators)
│   ├── Search creators by name/email
│   ├── Suspend/unsuspend creators
│   ├── View creator details
│   └── Reset user credentials
│
├── Course Management
│   ├── View all courses
│   ├── Ban/unban courses
│   ├── Search courses
│   ├── View course details
│   └── Monitor course compliance
│
├── Content Moderation
│   ├── Review reported courses
│   ├── Review reported comments
│   ├── Review pending media uploads
│   ├── Approve/reject uploads
│   ├── Set content moderation policies
│   └── View moderation logs
│
├── Creator Management
│   ├── Issue warnings to creators
│   ├── Track warning history
│   ├── Suspend creators for violations
│   ├── Ban permanent violators
│   ├── View reputation scores
│   └── Monitor creator media capacity
│
├── Financial Management
│   ├── View platform revenue
│   ├── Track payment transactions
│   ├── Process withdrawal requests
│   └── Generate financial reports
│
├── Audit & Logging
│   ├── View comprehensive audit logs
│   ├── Filter by action/user/date
│   ├── Track system changes
│   └── Export audit trails
│
└── System Configuration
    ├── Configure moderation rules
    ├── Set content policies
    ├── Manage suspension rules
    └── System performance tuning
```

---

## 🏗️ Technology Stack

### Backend Framework
```
Spring Boot 3.5.5
├── Spring Data JPA (ORM)
├── Spring Web (REST APIs)
├── Spring Security (Authentication & Authorization)
├── Spring WebSocket (Real-time communication)
├── Spring Actuator (Monitoring & Health)
└── Spring Mail (Email notifications)
```

### Database & Caching
```
Database
├── MySQL 8.0+ (Primary database)
└── 28 entities with relationships

Caching
├── Redis 6.0+
│   ├── User roles cache
│   ├── Token cache
│   └── Course recommendations cache
└── In-memory caching strategies
```

### External Services
```
Authentication & Authorization
├── Keycloak 25.0.1 (OAuth2 Identity Provider)
├── JWT Token-based security
└── Role-based access control (RBAC)

File Storage
├── Firebase Cloud Storage
├── Google Cloud Storage SDK
└── Media capacity management

AI & Evaluation Services
├── OpenAI API (Speech-to-Text, Semantic analysis)
├── Gemini API (Content generation, Feedback)
└── Content moderation service (Python service, localhost:5000)

Email Services
├── Gmail SMTP
└── Email notifications

Real-time Communication
├── WebSocket (Spring WebSocket)
└── STOMP messaging protocol
```

### Testing & Build
```
Testing
├── JUnit 5
├── Mockito
├── Selenium (UI testing capability)
└── Allure (Test reporting)

Build & Deployment
├── Maven 3.8+
├── Docker & Docker Compose
├── Jib (Docker image builder)
└── Spring Boot Actuator
```

---

## 📊 Data Models (Key Entities)

### User Management
```
Creator (Educator)
├── creatorId: String (Primary Key)
├── fullName, imageUrl, mobiPhone
├── balance (Financial tracking)
├── paymentEmail
├── titleSelf (Self-description)
├── certificateUrl (List of certificates)
├── status (ACTIVE, INACTIVE, BANNED)
├── reputationScore (0-100)
├── warningCount & bannedUntil
└── Relationships: Courses, Kahoot sessions, Warnings

Customer (Learner)
├── Inherits from User base
├── Tracks enrollment history
├── Learning progress tracking
└── AI evaluation history
```

### Course Management
```
Course
├── courseId: Long
├── name, description
├── language (VIETNAMESE, ENGLISH, etc.)
├── teachingLanguage
├── accessMode (PAID, FREE, PREMIUM)
├── joinKey (Access control)
├── urlImg (Course thumbnail)
├── isPublic, isBan
├── createdAt, lastUpdate
├── learningObjective, targetAudience
└── Relationships: Creator, Chapters, Enrollments, Reports

Chapter
├── chapterId
├── name, description
├── orderInChapter
└── Modules

Module
├── moduleId
├── titleOfModule
├── orderInChapter
├── contentTypes (Types of content in module)
└── Module Content Items

ModuleContent (Abstract - Polymorphic)
├── contentId
├── typeOfContent (Discriminator)
├── createdDate
└── Concrete types:
    ├── TeachingVideo
    ├── Passage
    ├── PdfDocument
    ├── FlashCard
    ├── MultipleChoiceQuestion
    ├── ReadingQuestion
    ├── ListeningChoiceQuestion
    ├── SpeakingPictureQuestion
    ├── SpeakingPassageQuestion
    ├── WritingQuestion
    ├── GapFillQuestion
    └── RememberWord
```

### Learning & Evaluation
```
Enrollment
├── enrollmentId
├── customerId, courseId
├── enrollmentDate
├── isFinish (Completion status)
└── Progress tracking

SemanticResult (Speech/Text comparison)
├── resultId
├── spokenText, expectedAnswer
├── similarityScore
├── feedback
└── Timestamp

WritingResult
├── resultId
├── essayContent
├── grammar, vocabulary scores
├── feedback
└── Timestamp

SemanticData (Data storage for evaluation)
├── Stores pronunciation analysis results
└── Links to evaluations

CustomerModuleContent
├── Tracks student progress per content item
├── Completion status
└── Attempt history
```

### Interactive Features
```
SessionInfo (Kahoot Quiz Session)
├── sessionCode (Unique session ID)
├── startedAt, finishedAt
├── currentQuestionIndex
├── currentAnswers (Count)
├── totalParticipants
├── Settings:
│   ├── randomizeQuestions
│   ├── randomizeOptions
│   └── showLeaderboardAfterEachQuestion
└── Status (WAITING, IN_PROGRESS, FINISHED)

Comment
├── commentId
├── content (Comment text)
├── rating (1-5 stars)
├── courseId
└── Moderation tracking

Feedback & Review
├── feedbackId
├── enrollmentId
├── rating, comment
└── Timestamp

Report
├── reportId
├── reportType (PLAGIARISM, INAPPROPRIATE, SPAM, etc.)
├── detail
├── courseId
└── Resolution status

Wishlist
├── wishlistId
├── customerId, courseId
└── Tracking saved courses
```

### Content Moderation
```
PendingImage
├── imageId
├── imageUrl
├── status (PENDING, APPROVED, REJECTED)
├── fileCategory
└── Review timestamp

BlacklistWord
├── List of inappropriate words
├── Toxicity scores
└── Language-specific variants

ContentModerationResult
├── Stores moderation check results
├── Confidence scores
└── Action taken

AuditLog
├── Tracks all admin actions
├── User, action, timestamp
└── Object affected
```

---

## 🔌 API Endpoints Organization

### Customer APIs (`/api/customer/*`)
```
Course Management
  GET    /courses                        - Get all courses
  GET    /courses/{id}                   - Get course details
  POST   /courses/{id}/enroll            - Enroll in course
  GET    /my-courses                     - Get enrolled courses
  GET    /courses/search?keyword=...     - Search courses
  
Wishlist
  POST   /wishlist/{courseId}            - Add to wishlist
  DELETE /wishlist/{courseId}            - Remove from wishlist
  GET    /wishlist                       - Get wishlist

Comments & Feedback
  POST   /courses/{courseId}/comments    - Post comment
  GET    /courses/{courseId}/comments    - Get comments
  POST   /feedback                       - Submit feedback
  
Dictionary & Learning
  GET    /dictionary                     - Get vocabulary
  GET    /learning-list                  - Get learning items

AI Evaluation
  POST   /evaluate/speaking              - Evaluate pronunciation
  POST   /evaluate/writing               - Evaluate essay
  GET    /evaluation/results/{id}        - Get evaluation result
  
Quiz
  GET    /quiz/session/{sessionCode}     - Join quiz session
  POST   /quiz/submit-answer             - Submit quiz answer
  GET    /quiz/leaderboard               - Get quiz leaderboard

Reports & Moderation
  POST   /report/course/{courseId}       - Report course
```

### Creator APIs (`/api/creator/*`)
```
Course Management
  POST   /courses                        - Create course
  GET    /courses                        - List my courses
  PUT    /courses/{id}                   - Update course
  DELETE /courses/{id}                   - Delete course
  GET    /courses/{id}/enrollments       - View enrolled students

Content Management
  POST   /chapters                       - Create chapter
  PUT    /chapters/{id}                  - Update chapter
  POST   /modules                        - Create module
  PUT    /modules/{id}                   - Update module
  POST   /content                        - Add module content
  PUT    /content/{id}                   - Update content
  DELETE /content/{id}                   - Delete content

File Upload
  POST   /upload/image                   - Upload media
  POST   /upload/video                   - Upload video
  POST   /upload/document                - Upload document
  GET    /media-capacity                 - Check capacity

AI Features
  POST   /AI/generate-feedback           - Generate AI feedback
  POST   /AI/suggest-content             - Suggest content
  GET    /AI/quota                       - Check AI usage quota

Kahoot Quiz
  POST   /kahoot                         - Create kahoot session
  GET    /kahoot/{id}                    - Get kahoot details
  PUT    /kahoot/{id}                    - Update kahoot
  POST   /kahoot/{id}/start              - Start session
  POST   /kahoot/{id}/end                - End session

Dashboard
  GET    /dashboard                      - Creator dashboard
  GET    /profile                        - Creator profile
  PUT    /profile                        - Update profile
  GET    /analytics                      - Course analytics

Financial
  GET    /earnings                       - View earnings
  POST   /withdraw                       - Request withdrawal
```

### Admin APIs (`/api/admin/*`)
```
Dashboard
  GET    /dashboard/overview             - System overview
  GET    /dashboard/metrics              - Key metrics
  GET    /dashboard/activities           - Recent activities
  GET    /dashboard/top-creators         - Top creators ranking

User Management
  GET    /creators                       - List all creators
  GET    /creators/{id}                  - Creator details
  PUT    /creators/{id}/suspend          - Suspend creator
  PUT    /creators/{id}/unsuspend        - Unsuspend creator
  DELETE /creators/{id}                  - Delete creator
  POST   /creators/{id}/warn             - Issue warning

Course Management
  GET    /courses                        - List all courses
  GET    /courses/{id}                   - Course details
  PUT    /courses/{id}/ban               - Ban course
  PUT    /courses/{id}/unban             - Unban course
  DELETE /courses/{id}                   - Delete course

Content Moderation
  GET    /pending-images                 - Pending media for review
  POST   /pending-images/{id}/approve    - Approve media
  POST   /pending-images/{id}/reject     - Reject media
  GET    /reports                        - List course reports
  PUT    /reports/{id}/resolve           - Resolve report

Audit & Logging
  GET    /audit-logs                     - Get audit logs
  GET    /audit-logs/filter              - Filter audit logs
```

### Common APIs
```
WebSocket (`/ws/kahoot`)
  - Real-time quiz session updates
  - Live question delivery
  - Instant feedback
  - Leaderboard updates

Health & Monitoring
  GET    /actuator/health                - Application health
  GET    /actuator/metrics               - Performance metrics
  GET    /actuator/env                   - Environment info
```

---

## 🔄 Key Workflows

### 1. **Course Creation & Publication**
```
Creator Action → Create Course → Add Chapters → Add Modules → 
Add Content (Videos, Exercises, etc.) → Set Pricing → Publish → 
Course available for enrollment
```

### 2. **Student Learning Journey**
```
Student Discovery → Search/Browse → View Details → Enroll → 
Access Content → Complete Lessons → AI Evaluation → Feedback → 
Progress Tracking → Certificate/Completion
```

### 3. **Real-time Quiz Session**
```
Instructor Creates Kahoot → Adds Questions → Starts Session → 
Issues Session Code → Students Join via Code → Questions Delivered → 
Students Answer in Real-time → Live Leaderboard → Session Ends → 
Results Recorded
```

### 4. **AI Evaluation Process**
```
Student Submits Work (Speaking/Writing) → AI Processing → 
Feature Extraction → Scoring Model → Generate Feedback → 
Store Results → Return to Student → Track Progress
```

### 5. **Content Moderation**
```
User Reports Content → Admin Reviews → 
Approve (keep content) or Reject (remove content) → 
Creator Notified → Reputation Adjusted → Action Logged
```

---

## 📱 Technology Integration Points

### Real-time Features
```
WebSocket Communication
├── Kahoot quiz sessions
├── Live leaderboards
├── Real-time notifications
└── Instant message delivery

Message Protocol
├── STOMP over WebSocket
├── /app/quiz/... destinations
└── /user/queue/... subscriptions
```

### AI/ML Integration
```
OpenAI API
├── Speech-to-Text (Wav format to text)
├── Semantic embeddings (Text to vectors)
└── Similarity scoring

Gemini API
├── Content generation
├── Feedback creation
└── Question suggestions

Custom Moderation Service
├── Comment toxicity detection
├── Profanity filtering
├── Content classification
```

### Storage & Media
```
Firebase Cloud Storage
├── Video uploads
├── Document storage
├── Media streaming
└── Backup & recovery

MySQL Database
├── All structured data
├── Relationships & indexing
├── Transaction management
└── ACID compliance

Redis Cache
├── Session management
├── Token storage
├── Recommendation caching
└── Temporary data
```

---

## 👥 User Roles & Permissions

### Customer Role
```
Permissions:
  ✓ View courses
  ✓ Search courses
  ✓ Enroll in courses
  ✓ Access learning content
  ✓ Participate in quizzes
  ✓ Submit assignments
  ✓ Write feedback/reviews
  ✓ Report inappropriate content
  
Restrictions:
  ✗ Cannot create courses
  ✗ Cannot modify courses
  ✗ Cannot access admin panel
  ✗ Cannot modify other users' data
```

### Creator Role
```
Permissions:
  ✓ Create courses
  ✓ Manage courses
  ✓ Upload content
  ✓ View student enrollments
  ✓ Access creator dashboard
  ✓ Use AI content generation
  ✓ Create Kahoot sessions
  ✓ Withdraw earnings
  
Restrictions:
  ✗ Cannot access admin panel
  ✗ Cannot modify other creators' courses
  ✗ Cannot ban users/courses
  ✗ Cannot view system logs
```

### Admin Role
```
Permissions:
  ✓ Full system access
  ✓ User management
  ✓ Course management
  ✓ Content moderation
  ✓ Financial management
  ✓ View audit logs
  ✓ System configuration
  ✓ Creator management
  
Restrictions:
  None - Full system control
```

---

## 🔐 Security Features

### Authentication
```
JWT-based authentication
├── Keycloak identity provider
├── OAuth2 resource server
├── JWT token validation
└── Token expiration & refresh
```

### Authorization
```
Role-Based Access Control (RBAC)
├── Three main roles (CUSTOMER, CREATOR, ADMIN)
├── API endpoint protection
├── Resource-level authorization
└── Dynamic permission checking
```

### Data Protection
```
Validation & Sanitization
├── Input validation on all endpoints
├── SQL injection prevention
├── XSS protection
└── CSRF token validation

Content Moderation
├── Comment filtering
├── Toxicity detection
├── Blacklist word filtering
└── User reporting system
```

### Audit & Compliance
```
Logging & Auditing
├── All admin actions logged
├── User activity tracking
├── Financial transaction logs
└── Content moderation history
```

---

## 📈 Performance Optimization

### Database Optimization
```
Indexing Strategy
├── Index on creator_id (frequent joins)
├── Index on course_id (course queries)
├── Index on enrollment dates
└── Composite indexes for common filters

Query Optimization
├── Lazy loading for relationships
├── Pagination for large result sets
├── Database connection pooling
└── Prepared statements
```

### Caching Strategy
```
Multi-level Caching
├── Redis distributed cache
├── In-memory Spring cache
├── Course recommendations cache
├── User roles cache
└── TTL-based expiration
```

### Load Balancing
```
Scalability Considerations
├── Stateless service design
├── WebSocket horizontal scaling
├── Database read replicas (optional)
├── CDN for static assets
└── Message queuing for async tasks
```

---

## 📊 Current Metrics & Statistics

### Project Size
```
Total Entities:           28
Total Repositories:       24
Total Controllers:        18
Total Services:           25+
Total DTOs:              50+
Lines of Code:           15,000+
Database Tables:         28+
API Endpoints:           100+
```

### Technology Versions
```
Spring Boot:             3.5.5
Java:                    17
MySQL:                   8.0+
Redis:                   6.0+
Keycloak:                25.0.1
Firebase Admin SDK:      9.4.1
OpenAI API:              Latest
Gemini API:              Latest
```

---

## 🚀 Deployment & DevOps

### Docker Deployment
```
Multi-container Architecture
├── MySQL container (Database)
├── Redis container (Cache)
├── Backend (Spring Boot app)
├── Frontend (React/Node)
└── Nginx (Reverse proxy)

Docker Compose
└── Orchestrates all services
```

### Build Pipeline
```
Maven Build
├── Compile & test
├── Package JAR
├── Jib Docker image build
└── Push to registry

CI/CD Ready
├── GitHub Actions capable
├── Automated testing hooks
├── Deployment automation
└── Rollback capability
```

---

## 📝 Development Notes

### File Upload Management
```
Media Storage Features
├── Multiple file types (video, PDF, image)
├── Creator media capacity limits
├── Pending image approval workflow
├── Firebase Cloud Storage integration
└── URL-based asset delivery
```

### AI Integration
```
Current AI Services
├── OpenAI for speech & semantic analysis
├── Gemini for content generation
└── Custom moderation service

Future Improvements
├── Fine-tune models on domain data
├── Implement local models
├── Add more evaluation criteria
└── Expand supported languages
```

### Content Types
```
Supported Learning Content
├── 12+ different content types
├── Rich media support
├── Interactive exercises
├── Assessment tools
└── Progress tracking
```

---

## 🎯 Success Metrics

### User Engagement
```
Target Metrics:
├── Daily Active Users (DAU)
├── Course Completion Rate
├── Student Satisfaction Rating
└── Creator Retention Rate
```

### Platform Performance
```
Target Metrics:
├── API Response Time < 500ms
├── Quiz Session Latency < 2s
├── 99.9% System Uptime
└── Database Query < 100ms
```

### Business Metrics
```
Target Metrics:
├── Course Quality Score
├── User Growth Rate
├── Revenue per Creator
└── Platform Engagement Score
```

---

## 📚 Documentation References

- **ARCHITECTURE_PLAN.md** - Proposed system transformation
- **CURRENT_ARCHITECTURE.md** - Detailed current architecture
- **IMPLEMENTATION_CHECKLIST.md** - Implementation tasks
- **ARCHITECTURE_COMPARISON.md** - Before/after comparison
- **TECHNICAL_QUICKSTART.md** - Developer guide

---

**Document Version**: 1.0  
**Last Updated**: May 18, 2026  
**Status**: ✅ Complete & Current
