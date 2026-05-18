# Technical Quick Start Guide - New Architecture

## Table of Contents
1. System Overview
2. Local Development Setup
3. Service Integration
4. API Endpoints
5. Configuration
6. Troubleshooting

---

## System Overview

### Architecture Stack
```
┌─────────────────────────┐
│   Frontend (React)      │
└───────────────┬─────────┘
                │
┌───────────────▼─────────────────────────────────┐
│         Spring Boot Backend (9090)              │
│  ┌────────────────────────────────────────────┐ │
│  │  Authentication (JWT - Local)             │ │
│  │  ├─ AuthenticationService                 │ │
│  │  ├─ TokenService                          │ │
│  │  └─ AuthorizationService                  │ │
│  ├────────────────────────────────────────────┤ │
│  │  AI Client Services (HTTP → Python)       │ │
│  │  ├─ PronunciationClientService ──────────────────► :5001
│  │  ├─ WritingClientService ─────────────────────────► :5002
│  │  ├─ ContentGenerationAgent                │ │
│  │  └─ ContentModerationService ─────────────────────► :5000 (hiện có)
│  ├────────────────────────────────────────────┤ │
│  │  Business Services                        │ │
│  │  ├─ CourseService                         │ │
│  │  ├─ EnrollmentService                     │ │
│  │  └─ UserService                           │ │
│  └────────────────────────────────────────────┘ │
└───────────────┬─────────────────────────────────┘
                │
    ┌───────────┼──────────┬─────────────┬────────────────┐
    │           │          │             │                │
┌───▼────┐  ┌──▼──┐  ┌────▼──────┐  ┌───▼──────────┐  ┌─▼──────────┐
│ MySQL  │  │Redis│  │Python:5001│  │ Python:5002  │  │Python:5000 │
│        │  │     │  │Pronunciat.│  │ Writing IELTS│  │Content Mod │
│ Local  │  │ TTL │  │wav2vec2   │  │ KevSun model │  │(existing)  │
│ Auth   │  │Cache│  │+ LLM      │  │              │  │            │
└────────┘  └─────┘  └───────────┘  └──────────────┘  └────────────┘
```

---

## Local Development Setup

### Prerequisites
- Java 17+
- MySQL 8.0+
- Redis 6.0+
- Python 3.9+ (for model inference wrapper)
- Maven 3.8+

### Installation Steps

#### 1. Clone and Setup Project
```bash
# Clone repository
git clone <repo-url>
cd e-learning

# Build project
mvn clean install

# Setup databases
mysql -u root -p < setup/database.sql
```

#### 2. Configure Environment Variables
Create `.env` file or set system variables:

```bash
# Authentication
JWT_SECRET=your-256-bit-secret-key-base64-encoded
JWT_ACCESS_TOKEN_EXPIRATION=1800000
JWT_REFRESH_TOKEN_EXPIRATION=2592000000

# Database
DB_HOST=localhost
DB_PORT=3306
DB_NAME=elearning_db
DB_USER=root
DB_PASSWORD=your_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Model Paths
PRONUNCIATION_MODEL_PATH=/models/wav2vec2
IELTS_MODEL_PATH=/models/ielts_scorer
CONTENT_GEN_MODEL_PATH=/models/content_generator

# Model Cache TTL (seconds)
MODEL_CACHE_TTL=3600
CONTENT_CACHE_TTL=86400
```

#### 3. Setup Python AI Servers
```bash
# Pronunciation server
cd ai-services/pronunciation
pip install -r requirements.txt
python scripts/download_wav2vec2.py
uvicorn main:app --host 0.0.0.0 --port 5001

# Writing server  
cd ai-services/writing
pip install -r requirements.txt
python scripts/download_ielts_model.py
uvicorn main:app --host 0.0.0.0 --port 5002

# Verify cả 2 server
curl http://localhost:5001/health   # {"status": "ok", "model": "wav2vec2"}
curl http://localhost:5002/health   # {"status": "ok", "model": "ielts_scorer"}
```

#### 4. Start Services

```bash
# Terminal 1: MySQL
mysql.server start  # macOS
# or: systemctl start mysql  # Linux

# Terminal 2: Redis
redis-server

# Terminal 3: Backend
mvn spring-boot:run

# Terminal 4: Frontend
cd frontend && npm start
```

### Docker Setup (Recommended)

```bash
# Build with docker-compose
docker-compose up

# Services will be available at:
# Frontend: http://localhost:3000
# Backend: http://localhost:9090
# MySQL: localhost:3306
# Redis: localhost:6379
```

---

## Service Integration Guide

### 1. Authentication Service

#### User Registration
```java
@PostMapping("/auth/register")
public ResponseEntity<?> register(@RequestBody UserRegistrationDTO dto) {
    User user = authenticationService.register(
        dto.getEmail(),
        dto.getPassword(),
        dto.getFullName()
    );
    return ResponseEntity.ok(user);
}
```

Request Body:
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "fullName": "John Doe"
}
```

#### User Login
```java
@PostMapping("/auth/login")
public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    AuthResponse response = authenticationService.login(
        request.getEmail(),
        request.getPassword()
    );
    return ResponseEntity.ok(response);
}
```

Request Body:
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

Response:
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "expiresIn": 1800,
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "roles": ["CUSTOMER"]
  }
}
```

#### Token Refresh
```java
@PostMapping("/auth/refresh")
public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequest request) {
    String newAccessToken = tokenService.refreshAccessToken(
        request.getRefreshToken()
    );
    return ResponseEntity.ok(new TokenResponse(newAccessToken));
}
```

### 2. Pronunciation Analysis Service

#### Analyze Pronunciation — Spring Boot gọi Python server
```java
// PronunciationClientService.java
@Service
public class PronunciationClientService {

    @Value("${ai.pronunciation.service.url}")
    private String serviceUrl;

    private final RestTemplate restTemplate;

    public PronunciationClientService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public PronunciationResult analyze(byte[] audioData, String expectedText, String language) {
        Map<String, Object> request = Map.of(
            "audio_base64", Base64.getEncoder().encodeToString(audioData),
            "expected_text", expectedText,
            "language", language
        );
        return restTemplate.postForObject(
            serviceUrl + "/analyze",
            request,
            PronunciationResult.class
        );
    }
}
```

Python server nhận request (ai-services/pronunciation/main.py):
```python
@app.post("/analyze")
async def analyze(request: AnalyzeRequest):
    audio_bytes = base64.b64decode(request.audio_base64)
    result = analyzer.analyze(audio_bytes, request.expected_text, request.language)
    return result

@app.get("/health")
async def health():
    return {"status": "ok", "model": "wav2vec2"}
```

Response:
```json
{
  "confidence": 0.95,
  "phonemeErrors": [
    {
      "word": "pronunciation",
      "phoneme": "/ə/",
      "expectedSound": "schwa",
      "actualSound": "short-a",
      "severity": "medium",
      "suggestion": "Try pronouncing with a softer vowel sound"
    }
  ],
  "overallScore": 8.5,
  "feedback": "Good pronunciation overall. Focus on the stressed syllables.",
  "timeSpent": 5.2
}
```

### 3. Writing Evaluation Service

#### Evaluate Essay — Spring Boot gọi Python server
```java
// WritingClientService.java
@Service
public class WritingClientService {

    @Value("${ai.writing.service.url}")
    private String serviceUrl;

    private final RestTemplate restTemplate;

    public WritingScore evaluate(String essay, String topic) {
        Map<String, String> request = Map.of(
            "essay", essay,
            "topic", topic
        );
        return restTemplate.postForObject(
            serviceUrl + "/evaluate",
            request,
            WritingScore.class
        );
    }
}
```

Python server nhận request (ai-services/writing/main.py):
```python
@app.post("/evaluate")
async def evaluate(request: EvaluateRequest):
    result = evaluator.score(request.essay, request.topic)
    return result

@app.get("/health")
async def health():
    return {"status": "ok", "model": "ielts_scorer"}
```

Response:
```json
{
  "lexicalScore": 7.5,
  "grammarScore": 8.0,
  "coherenceScore": 7.0,
  "taskAchievementScore": 8.5,
  "overallBandScore": 7.8,
  "detailedFeedback": {
    "strengths": [
      "Wide range of vocabulary",
      "Complex sentence structures",
      "Clear organization"
    ],
    "areasForImprovement": [
      "Some spelling errors",
      "Pronoun reference issues in paragraph 2",
      "Could use more transition phrases"
    ]
  },
  "estimatedIeltsBand": "7.5-8.0"
}
```

### 4. Content Generation Agent

#### Generate Content
```java
@PostMapping("/content/generate")
public ResponseEntity<?> generateContent(
    @RequestBody ContentGenerationRequest request,
    @AuthenticationPrincipal Jwt jwt
) {
    String userId = jwt.getSubject();
    ContentPackage content = contentAgent.generateTopicContent(
        request.getTopic(),
        request.getLevel()
    );
    return ResponseEntity.ok(content);
}
```

Response:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "topic": "IELTS Band 7 Vocabulary",
  "level": "ADVANCED",
  "lessons": [
    {
      "lessonId": 1,
      "title": "Academic Writing Vocabulary",
      "vocabulary": [
        {
          "word": "ubiquitous",
          "pronunciation": "/juːˈbɪk.wɪ.təs/",
          "partOfSpeech": "adjective",
          "definition": "present, appearing, or found everywhere",
          "example": "Smartphones have become ubiquitous in modern society.",
          "difficulty": 7
        }
      ]
    }
  ],
  "exercises": [
    {
      "exerciseId": 1,
      "type": "fill_blank",
      "question": "The _____ use of technology has changed education.",
      "options": ["ubiquitous", "unique", "useless", "urban"],
      "correctAnswer": 0,
      "explanation": "Ubiquitous means everywhere/widespread"
    }
  ],
  "createdAt": "2026-05-18T10:30:00Z",
  "cachedUntil": "2026-05-25T10:30:00Z"
}
```

---

## API Endpoints Reference

### Authentication Endpoints
```
POST   /api/auth/register          - Register new user
POST   /api/auth/login             - Login user
POST   /api/auth/refresh           - Refresh access token
POST   /api/auth/logout            - Logout user
GET    /api/auth/profile           - Get current user profile
```

### Evaluation Endpoints
```
POST   /api/evaluation/pronunciation    - Analyze pronunciation
POST   /api/evaluation/writing          - Evaluate essay
GET    /api/evaluation/results/{id}     - Get evaluation result
```

### Content Endpoints
```
POST   /api/content/generate       - Generate content for topic
GET    /api/content/{id}           - Get generated content
GET    /api/content/topics         - Get suggested topics
```

### Course Management
```
GET    /api/courses                - List all courses
GET    /api/courses/{id}           - Get course details
POST   /api/courses/{id}/enroll    - Enroll in course
```

---

## Configuration Files

### application.properties

```properties
# ==================== Spring ====================
spring.application.name=elearning-project
server.port=9090
server.servlet.context-path=/api

# ==================== Database ====================
spring.datasource.url=jdbc:mysql://localhost:3306/elearning_db
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect

# ==================== JWT ====================
jwt.secret=${JWT_SECRET}
jwt.access-token-expiration=${JWT_ACCESS_TOKEN_EXPIRATION:1800000}
jwt.refresh-token-expiration=${JWT_REFRESH_TOKEN_EXPIRATION:2592000000}

# ==================== Redis ====================
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.redis.timeout=2000ms

# ==================== AI Python Services ====================
ai.pronunciation.service.url=${PRONUNCIATION_SERVICE_URL:http://localhost:5001}
ai.writing.service.url=${WRITING_SERVICE_URL:http://writing-service:5002}
ai.content.cache-ttl=${CONTENT_CACHE_TTL:86400}

# ==================== File Upload ====================
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB

# ==================== Security ====================
spring.security.filter.order=5
server.servlet.session.timeout=20m
```

---

## Database Schema (Key Tables)

### Users Table
```sql
CREATE TABLE users (
  id CHAR(36) PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  username VARCHAR(100) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(255),
  status ENUM('ACTIVE', 'INACTIVE', 'BANNED') DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_email (email)
);
```

### Roles Table
```sql
CREATE TABLE roles (
  id INT PRIMARY KEY,
  name VARCHAR(50) NOT NULL UNIQUE,
  permissions JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default roles
INSERT INTO roles VALUES
(1, 'CUSTOMER', '{"read:course","submit:evaluation"}', NOW()),
(2, 'CREATOR', '{"read:course","create:content","manage:content"}', NOW()),
(3, 'ADMIN', '{"*":"*"}', NOW());
```

### Refresh Tokens Table
```sql
CREATE TABLE refresh_tokens (
  token_id CHAR(36) PRIMARY KEY,
  user_id CHAR(36) NOT NULL,
  token_hash VARCHAR(255) NOT NULL,
  expires_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_user (user_id),
  INDEX idx_expires (expires_at)
);
```

### Pronunciation Results Table
```sql
CREATE TABLE pronunciation_results (
  id CHAR(36) PRIMARY KEY,
  user_id CHAR(36) NOT NULL,
  spoken_text TEXT,
  expected_text TEXT,
  language VARCHAR(10),
  phoneme_errors JSON,
  confidence_score FLOAT,
  overall_score FLOAT,
  feedback TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_user_date (user_id, created_at)
);
```

### Writing Results Table
```sql
CREATE TABLE writing_results (
  id CHAR(36) PRIMARY KEY,
  user_id CHAR(36) NOT NULL,
  essay_content LONGTEXT,
  topic_code VARCHAR(50),
  lexical_score FLOAT,
  grammar_score FLOAT,
  coherence_score FLOAT,
  task_achievement_score FLOAT,
  overall_band_score FLOAT,
  detailed_feedback JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_user_date (user_id, created_at)
);
```

---

## Common Tasks

### Add a New Role
```sql
INSERT INTO roles (id, name, permissions) VALUES
(4, 'INSTRUCTOR', JSON_OBJECT(
  'read', JSON_ARRAY('course', 'student'),
  'write', JSON_ARRAY('feedback', 'grades')
));
```

### Create a User Programmatically
```java
User user = new User();
user.setId(UUID.randomUUID().toString());
user.setEmail("newuser@example.com");
user.setUsername("newuser");
user.setPassword(passwordEncoder.encode("PlainPassword123!"));
user.setFullName("New User");
user.setStatus(UserStatus.ACTIVE);
userRepository.save(user);

// Assign role
UserRole userRole = new UserRole(user.getId(), 1); // CUSTOMER role
userRoleRepository.save(userRole);
```

### Check Token Expiration
```java
@GetMapping("/token/info")
public ResponseEntity<?> getTokenInfo(@RequestHeader("Authorization") String token) {
    String jwt = token.replace("Bearer ", "");
    Claims claims = tokenService.extractClaims(jwt);
    return ResponseEntity.ok(new TokenInfoDTO(
        claims.getSubject(),
        claims.getExpiration(),
        claims.get("roles")
    ));
}
```

---

## Troubleshooting

### Issue: "Invalid JWT Token"
**Solution:**
1. Check JWT_SECRET is properly set
2. Verify token hasn't expired
3. Ensure token format is "Bearer <token>"

### Issue: "Model not found"
**Solution:**
```bash
# Verify model paths
ls -la /models/wav2vec2/
ls -la /models/ielts_scorer/

# Re-download if missing
python scripts/download_models.py
```

### Issue: Redis connection timeout
**Solution:**
```bash
# Check Redis is running
redis-cli ping  # Should return PONG

# Restart Redis
redis-server --daemonize yes
```

### Issue: Database connection failed
**Solution:**
```bash
# Check MySQL is running
mysql -u root -p -e "SELECT 1"

# Verify credentials in .env
echo $DB_USER
echo $DB_PASSWORD
```

### Issue: High inference latency
**Solution:**
1. Check model cache is enabled (MODEL_CACHE_TTL)
2. Monitor system memory (models need ~2GB during inference)
3. Check CPU usage isn't maxed out

---

## Performance Monitoring

### Check System Health
```bash
# Monitor CPU and memory
top

# Check database connections
mysql -u root -p -e "SHOW PROCESSLIST;"

# Monitor Redis
redis-cli INFO stats
```

### View Logs
```bash
# Backend logs
tail -f logs/application.log

# Filter by level
tail -f logs/application.log | grep "ERROR"

# Search for specific service
grep "PronunciationAnalysisService" logs/application.log
```

---

## Security Best Practices

1. **Never commit secrets**: Use environment variables or .env files (add to .gitignore)
2. **Password requirements**: Minimum 12 characters, mix of upper/lower/numbers/special
3. **Token expiration**: Keep access token TTL short (15-30 min)
4. **HTTPS only**: Always use HTTPS in production
5. **Rate limiting**: Implement per-user API rate limits
6. **Input validation**: Validate all user inputs before processing

---

## Resources

- [DJL Documentation](https://docs.djl.ai)
- [JJWT Documentation](https://github.com/jwtk/jjwt)
- [HuggingFace Hub](https://huggingface.co)
- [Spring Security](https://spring.io/projects/spring-security)
- [MySQL Documentation](https://dev.mysql.com/doc/)

---

**Document Version**: 1.0  
**Last Updated**: May 18, 2026  
**Status**: Ready for Development
