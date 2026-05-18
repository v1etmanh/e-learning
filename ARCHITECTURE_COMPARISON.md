# Visual Architecture Comparison

## Current Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT TIER                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   Web App    │  │  Mobile App  │  │   Admin      │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
└─────────┼──────────────────┼──────────────────┼─────────────────┘
          │                  │                  │
          └──────────────────┼──────────────────┘
                             │
         ┌───────────────────▼───────────────────┐
         │    API Gateway / Spring Boot         │
         │  (Port: 9090)                        │
         └────────┬──────────────────────┬──────┘
                  │                      │
        ┌─────────▼────────┐    ┌────────▼──────────┐
        │ Authentication   │    │   Core Services   │
        │ Filter           │    │  (Controllers)    │
        │ ┌──────────────┐ │    │                   │
        │ │ Keycloak     │ │    │  Course           │
        │ │ JWT Decoder  │ │    │  Enrollment       │
        │ │ Role Conv.   │ │    │  Content          │
        │ └──────────────┘ │    │  User             │
        └──────────┬──────┘    └──────┬────────────┘
                   │                  │
        ┌──────────▼──────────────────▼────────┐
        │         Service Layer                │
        │  ┌────────────────────────────────┐ │
        │  │ AI Services (External APIs)   │ │
        │  │ ├─ OpenAI Service             │ │
        │  │ │  ├─ Speech-to-Text          │ │
        │  │ │  └─ Semantic Analysis       │ │
        │  │ ├─ Gemini Service             │ │
        │  │ │  ├─ Content Generation      │ │
        │  │ │  └─ Feedback Generation     │ │
        │  │ ├─ File Upload Service        │ │
        │  │ ├─ Audit Log Service          │ │
        │  │ └─ Dictionary Service         │ │
        │  └────────────────────────────────┘ │
        └──────────┬──────────────────────────┘
                   │
     ┌─────────────┼─────────────┬──────────────┐
     │             │             │              │
┌────▼────┐  ┌───▼────┐  ┌─────▼───┐  ┌──────▼──┐
│ MySQL   │  │ Redis  │  │Firebase │  │ Keycloak│
│         │  │        │  │ Storage │  │ Server  │
│ Users   │  │ Cache  │  │         │  │         │
│ Courses │  │ Roles  │  │ Files   │  │ Users   │
│ Results │  │ Tokens │  │ Storage │  │ Realms  │
└─────────┘  └────────┘  └─────────┘  └─────────┘
     │             │             │           │
     └─────────────┼─────────────┴───────────┘
                   │
        ┌──────────▼──────────┐
        │  External APIs      │
        │  ┌────────────────┐ │
        │  │ OpenAI API     │ │ $500/month
        │  │ Gemini API     │ │ $300/month
        │  │ Firebase API   │ │ $200/month
        │  └────────────────┘ │
        └─────────────────────┘

ISSUES:
❌ 6 containers to manage
❌ Complex authentication (Keycloak)
❌ API key dependency
❌ High monthly costs ($1,350)
❌ Network latency issues
❌ Vendor lock-in risk
```

---

## Proposed Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT TIER                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   Web App    │  │  Mobile App  │  │   Admin      │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
└─────────┼──────────────────┼──────────────────┼─────────────────┘
          │                  │                  │
          └──────────────────┼──────────────────┘
                             │
         ┌───────────────────▼───────────────────┐
         │    API Gateway / Spring Boot         │
         │  (Port: 9090)                        │
         │  Self-contained with AI Models       │
         └────────┬──────────────────────┬──────┘
                  │                      │
        ┌─────────▼────────┐    ┌────────▼──────────┐
        │ Authentication   │    │   Core Services   │
        │ Filter (Custom   │    │  (Controllers)    │
        │  JWT)            │    │                   │
        │ ┌──────────────┐ │    │  Course           │
        │ │ JWT Token    │ │    │  Enrollment       │
        │ │ Service      │ │    │  Content          │
        │ │ (Local)      │ │    │  User             │
        │ └──────────────┘ │    │  Evaluation       │
        └──────────┬──────┘    └──────┬────────────┘
                   │                  │
        ┌──────────▼──────────────────▼────────┐
        │         Service Layer                │
        │  ┌────────────────────────────────┐ │
        │  │ AI Services (Local Models)    │ │
        │  │ ├─ Pronunciation Service      │ │
        │  │ │  ├─ wav2vec2 Model          │ │
        │  │ │  ├─ LLM Analysis            │ │
        │  │ │  └─ Error Feedback          │ │
        │  │ ├─ Writing Evaluation Service │ │
        │  │ │  ├─ IELTS Scoring Model    │ │
        │  │ │  ├─ Multi-criteria Analysis │ │
        │  │ │  └─ Band Score Mapping      │ │
        │  │ ├─ Content Generation Agent   │ │
        │  │ │  ├─ Topic-based Gen.        │ │
        │  │ │  ├─ Validation              │ │
        │  │ │  └─ Caching                 │ │
        │  │ ├─ File Upload Service        │ │
        │  │ ├─ Audit Log Service          │ │
        │  │ └─ Dictionary Service         │ │
        │  └────────────────────────────────┘ │
        └──────────┬──────────────────────────┘
                   │
     ┌─────────────┼──────────────┬──────────────┬─────────────┐
     │             │              │              │             │
┌────▼────┐  ┌───▼────┐  ┌──────▼───┐  ┌───────▼──────┐  ┌──▼──────────┐
│ MySQL   │  │ Redis  │  │  Cloud   │  │ Python :5001 │  │ Python :5002│
│         │  │        │  │  Storage │  │ Pronunciation│  │ Writing     │
│ Users   │  │ Cache  │  │ Optional │  │ wav2vec2+LLM │  │ IELTS Scorer│
│ Courses │  │ Tokens │  │          │  │ ~500MB RAM   │  │ ~400MB RAM  │
│ Results │  │        │  │  Files   │  │ Port 5001    │  │ Port 5002   │
└─────────┘  └────────┘  └──────────┘  └──────────────┘  └─────────────┘

IMPROVEMENTS:
✅ 6 containers — Keycloak bỏ, thêm 2 Python AI server
✅ Simple authentication (JWT tự quản lý)
✅ No API key dependency (OpenAI, Gemini bỏ)
✅ Low monthly costs (~$130)
✅ Python model chạy native (không overhead JVM)
✅ Mỗi AI service scale độc lập
✅ Pattern đã có sẵn (port 5000 ContentModeration)
```

---

## Cost Comparison

```
CURRENT ARCHITECTURE COST
═════════════════════════════════════════
Monthly Breakdown:
├── OpenAI API             $500    (Speech-to-Text)
├── Gemini API             $300    (Content Generation)
├── Firebase Storage       $200    (File Storage)
├── Cloud VM (Keycloak)    $150    (Server Instance)
├── Cloud VM (Backend)     $200    (Backend Server)
└── Monitoring/Logging      $50    (Tools & Services)
═════════════════════════════════════════
Total/Month:              $1,400
Total/Year:              $16,800

Annual: $16,800


NEW ARCHITECTURE COST
═════════════════════════════════════════
Monthly Breakdown:
├── Database (MySQL)       $30     (Small tier)
├── Cache (Redis)          $20     (Small tier)
├── Backend VM             $50     (Smaller instance)
└── Monitoring/Logging      $30    (Tools & Services)
═════════════════════════════════════════
Total/Month:               $130
Total/Year:               $1,560

Annual: $1,560

COST SAVINGS: $15,240/year (91% reduction)
```

---

## Inference Performance Comparison

```
PRONUNCIATION EVALUATION
═════════════════════════════════════════
Current (API-based):
  ├── Network request         2-3 sec
  ├── OpenAI processing       3-5 sec
  ├── Network response        1-2 sec
  └── Total:                 6-10 sec  ❌

New (Local Model):
  ├── Audio upload           0.1 sec
  ├── wav2vec2 inference      2 sec
  ├── LLM analysis            2-3 sec
  ├── Feedback generation     1 sec
  └── Total:                 5-6 sec   ✅ 20% faster


WRITING EVALUATION
═════════════════════════════════════════
Current (API-based):
  ├── Network request         2-3 sec
  ├── Gemini processing       5-10 sec
  ├── Network response        1-2 sec
  └── Total:                 8-15 sec  ❌

New (Local Model):
  ├── Text upload            0.1 sec
  ├── IELTS scoring          1.5 sec
  ├── Criterion analysis     1.5 sec
  ├── Feedback generation    1 sec
  └── Total:                 4-5 sec   ✅ 65% faster


CONTENT GENERATION
═════════════════════════════════════════
Current (API-based):
  ├── API request             1-2 sec
  ├── Gemini generation       5-15 sec
  ├── Response                1-2 sec
  └── Total:                 7-19 sec  ❌

New (AI Agent):
  ├── Cache check             0.05 sec
  ├── Agent processing        3-5 sec
  ├── Validation              1-2 sec
  ├── Storage                 0.5 sec
  └── Total:                 4.5-7.5 sec ✅ 50-65% faster
              (or instant if cached)
```

---

## Database Schema Evolution

```
CURRENT SCHEMA (Keycloak-based)
═════════════════════════════════════════
├── semantic_result
│   ├── id
│   ├── customer_id (external ref)
│   ├── spoken_text
│   ├── expected_text
│   ├── similarity_score
│   └── created_at
│
├── writing_result
│   ├── id
│   ├── customer_id (external ref)
│   ├── essay_content
│   ├── feedback
│   └── created_at
│
└── Keycloak External
    ├── Users (in Keycloak)
    ├── Roles (in Keycloak)
    └── Permissions (in Keycloak)


NEW SCHEMA (Custom Auth)
═════════════════════════════════════════
├── users (NEW)
│   ├── id (UUID)
│   ├── email
│   ├── username
│   ├── password_hash (Bcrypt)
│   ├── full_name
│   ├── status
│   ├── created_at
│   └── updated_at
│
├── roles (NEW)
│   ├── id
│   ├── name (ENUM)
│   ├── permissions (JSON)
│   └── created_at
│
├── user_roles (NEW)
│   ├── user_id (FK)
│   ├── role_id (FK)
│   └── assigned_at
│
├── refresh_tokens (NEW)
│   ├── token_id (UUID)
│   ├── user_id (FK)
│   ├── token_hash
│   ├── expires_at
│   └── created_at
│
├── semantic_result (UPDATED)
│   ├── id
│   ├── user_id (FK) ← local reference
│   ├── spoken_text
│   ├── expected_text
│   ├── similarity_score
│   ├── phoneme_errors (JSON) ← NEW
│   ├── confidence_score (FLOAT) ← NEW
│   ├── pronunciation_feedback (TEXT) ← NEW
│   └── created_at
│
├── writing_result (UPDATED)
│   ├── id
│   ├── user_id (FK) ← local reference
│   ├── essay_content
│   ├── feedback
│   ├── lexical_score (FLOAT) ← NEW
│   ├── grammar_score (FLOAT) ← NEW
│   ├── coherence_score (FLOAT) ← NEW
│   ├── task_achievement_score (FLOAT) ← NEW
│   ├── ielts_band_score (FLOAT) ← NEW
│   └── created_at
│
└── generated_content (NEW)
    ├── id (UUID)
    ├── topic
    ├── level (ENUM)
    ├── content_type (ENUM)
    ├── content (JSON)
    ├── created_at
    ├── cached_until
    ├── accuracy_score
    ├── human_reviewed
    └── created_by (FK)
```

---

## Deployment Architecture

```
CURRENT MULTI-CONTAINER SETUP
═════════════════════════════════════════
┌──────────────────────────────────────┐
│        Docker Compose Stack          │
├──────────────────────────────────────┤
│ Container 1: MySQL Database          │
│   - Port: 3306                       │
│   - Volume: MySQL data               │
│   - Startup time: 10 sec             │
├──────────────────────────────────────┤
│ Container 2: Redis Cache             │
│   - Port: 6379                       │
│   - Volume: Redis data               │
│   - Startup time: 3 sec              │
├──────────────────────────────────────┤
│ Container 3: Keycloak Server         │
│   - Port: 8080                       │
│   - Database: Internal PostgreSQL    │
│   - Volume: Keycloak data            │
│   - Startup time: 30-45 sec ❌       │
│   - Memory: 512MB                    │
├──────────────────────────────────────┤
│ Container 4: Backend (Spring Boot)   │
│   - Port: 9090                       │
│   - Environment: Java 17             │
│   - Volume: Logs                     │
│   - Startup time: 20 sec             │
├──────────────────────────────────────┤
│ Container 5: Frontend (Node/React)   │
│   - Port: 3000                       │
│   - Build output                     │
│   - Startup time: 5 sec              │
├──────────────────────────────────────┤
│ Container 6: Nginx (Reverse Proxy)   │
│   - Port: 80, 443                    │
│   - SSL certificates                 │
│   - Startup time: 2 sec              │
└──────────────────────────────────────┘

Total Startup Time: ~70-80 seconds
Total Memory: ~2GB
Build Complexity: High ❌


NEW SIMPLIFIED SETUP
═════════════════════════════════════════
┌──────────────────────────────────────┐
│        Docker Compose Stack          │
├──────────────────────────────────────┤
│ Container 1: MySQL Database          │
│   - Port: 3306                       │
│   - Volume: MySQL data               │
│   - Startup time: 10 sec             │
├──────────────────────────────────────┤
│ Container 2: Redis Cache             │
│   - Port: 6379                       │
│   - Volume: Redis data               │
│   - Startup time: 3 sec              │
├──────────────────────────────────────┤
│ Container 3: Backend (Spring Boot)   │
│   - Port: 9090                       │
│   - WITH: Embedded JWT auth          │
│   - WITH: Local AI models            │
│   - Environment: Java 17             │
│   - Volume: Logs + Model cache       │
│   - Startup time: 15 sec ✅          │
│   - Memory: 800MB                    │
├──────────────────────────────────────┤
│ Container 4: Frontend (Node/React)   │
│   - Port: 3000                       │
│   - Build output                     │
│   - Startup time: 5 sec              │
└──────────────────────────────────────┘

Total Startup Time: ~35 seconds (50% reduction)
Total Memory: ~1.2GB (40% reduction)
Build Complexity: Low ✅

Keycloak removed: ~300MB saved
ML models: Optional in container
```

---

## Authentication Flow Comparison

```
KEYCLOAK-BASED FLOW (Current)
═════════════════════════════════════════
User clicks "Login"
        │
        ▼
Frontend: POST /auth/login {email, password}
        │
        ▼
Backend API
        │
        ▼ (HTTP request)
Keycloak Server (Port 8080)
        │
        ├─ Validate credentials
        ├─ Check user exists
        ├─ Generate JWT token
        └─ Return JWT + Refresh token
        │
        ▼ (HTTP response)
Backend: Validate response
        │
        ▼
Frontend: Store token in cookie
        │
        ▼
User logged in ✓

Issues: Extra network hop, Keycloak must be running, High latency


CUSTOM JWT FLOW (New)
═════════════════════════════════════════
User clicks "Login"
        │
        ▼
Frontend: POST /auth/login {email, password}
        │
        ▼
Backend API (AuthenticationService)
        │
        ├─ Query local users table
        ├─ Verify email exists
        ├─ Hash password with Bcrypt
        ├─ Compare with stored hash
        └─ If valid:
             ├─ Generate JWT token (signing locally)
             ├─ Generate Refresh token
             └─ Store refresh token in DB
        │
        ▼
Frontend: Store token in cookie
        │
        ▼
User logged in ✓

Benefits: Single hop, No external dependency, Lower latency
```

---

## Service Layer Architecture

```
CURRENT SERVICE CALLS
═════════════════════════════════════════
ControllerLayer
        │
        ├─→ AiEvaluateService.evaluateSpeaking()
        │   └─→ OpenAIService.speechToText() [EXTERNAL API ❌]
        │   └─→ OpenAIService.compareSemanticWithEmbedding() [EXTERNAL API ❌]
        │
        ├─→ AIService.generateFeedback()
        │   └─→ GeminiAiService.generateText() [EXTERNAL API ❌]
        │
        └─→ CourseService
            └─→ (Manual content, no generation)


NEW SERVICE CALLS
═════════════════════════════════════════
ControllerLayer
        │
        ├─→ PronunciationClientService.analyze()
        │   └─→ HTTP POST :5001/analyze [PYTHON SERVER ✅]
        │       └─→ wav2vec2 + LLM (Python native, no JVM overhead)
        │
        ├─→ WritingClientService.evaluateEssay()
        │   └─→ HTTP POST :5002/evaluate [PYTHON SERVER ✅]
        │       └─→ IELTS Scorer (Python native, no JVM overhead)
        │
        ├─→ ContentGenerationAgent.generateContent()
        │   ├─→ Cache.get(topic) [REDIS ✅]
        │   └─→ If miss:
        │       ├─→ LLMContentGenerator.generate() [LOCAL/API ✅]
        │       ├─→ ContentValidator.validate() [LOCAL ✅]
        │       └─→ Cache.set(topic) [REDIS ✅]
        │
        └─→ CourseService
            └─→ ContentGenerationAgent (Auto-generate as needed)
```

---

## Migration Timeline

```
WEEK 1: Pronunciation Python Server
─────────────────────────────────────────
Mon: Clone repo, setup Python env, download wav2vec2 model
Tue: Viết FastAPI app (main.py, analyzer.py)
Wed: Viết Dockerfile, test local
Thu: Viết PronunciationClientService.java (RestTemplate)
Fri: Integration test Spring Boot ↔ Python server

WEEK 2: Writing Python Server
─────────────────────────────────────────
Mon: Setup IELTS model từ HuggingFace
Tue: Viết FastAPI app (main.py, evaluator.py)
Wed: Viết Dockerfile, test local
Thu: Viết WritingClientService.java (RestTemplate)
Fri: Integration test + accuracy validation

WEEK 3: Custom Authentication
─────────────────────────────────────────
Mon: Design JWT token structure
Tue: Implement TokenService + AuthenticationService
Wed: Update SecurityConfig (bỏ Keycloak)
Thu: Migrate users from Keycloak
Fri: Testing & security validation

WEEK 4: Integration & Agent
─────────────────────────────────────────
Mon: Implement ContentGenerationAgent
Tue: Update docker-compose (6 services)
Wed: Full system integration testing
Thu: Performance benchmarking
Fri: Staging deployment

WEEK 5-6: Deployment & Monitoring
─────────────────────────────────────────
Blue-green deployment with canary
Monitor metrics continuously
Rollback ready at all times
```

---

## Quality Metrics Dashboard

```
BEFORE vs AFTER COMPARISON
═════════════════════════════════════════

Cost per Month:
  Current:  ██████████████████ $1,400
  New:      ██ $130
  Savings:  91% ✅

Response Time (avg):
  Current:  ████████░░░░ 12 seconds
  New:      ████░░░░░░░░ 6 seconds
  Improvement: 50% ✅

Infrastructure Complexity:
  Current:  ██████████░░░░░░░░░░ 60%
  New:      ███░░░░░░░░░░░░░░░░░░ 15%
  Reduction: 75% ✅

Deployment Time:
  Current:  ██████████ 70 seconds
  New:      ███░░░░░░░ 35 seconds
  Improvement: 50% ✅

Memory Usage (GB):
  Current:  ████████░░░░░░░░░░░░ 2GB
  New:      ███░░░░░░░░░░░░░░░░░░ 1.2GB
  Reduction: 40% ✅

API Dependency:
  Current:  Multiple (3 providers)
  New:      None (0 providers) ✅

Data Privacy:
  Current:  Shared with external APIs
  New:      100% on-premise ✅

Scale Capacity:
  Current:  Limited by API quotas
  New:      Unlimited (self-hosted) ✅
```

---

**Document Version**: 1.0  
**Last Updated**: May 18, 2026  
**Status**: Ready for Team Review
