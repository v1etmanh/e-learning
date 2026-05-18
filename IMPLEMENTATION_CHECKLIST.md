# Implementation Checklist & Quick Reference

## Phase 1: AI Model Integration (Weeks 1-2)

### Week 1: Pronunciation Detection — Python FastAPI Server

#### Python Server Setup (`ai-services/pronunciation/`)
- [ ] Clone mispronunciation-detection repository
- [ ] Tạo thư mục `ai-services/pronunciation/`
- [ ] Cài Python dependencies: `pip install fastapi uvicorn transformers librosa torch`
- [ ] Download wav2vec2 model (~350MB)
- [ ] Viết `main.py` — FastAPI app với 2 endpoint:
  ```python
  # POST /analyze
  # GET  /health
  ```
- [ ] Viết `model_loader.py` — load wav2vec2 khi server khởi động
- [ ] Viết `analyzer.py` — xử lý audio bytes, trả về JSON result
- [ ] Viết `requirements.txt`
- [ ] Viết `Dockerfile`
  ```dockerfile
  FROM python:3.11-slim
  WORKDIR /app
  COPY requirements.txt .
  RUN pip install -r requirements.txt
  COPY . .
  EXPOSE 5001
  CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "5001"]
  ```
- [ ] Test local: `curl -X POST localhost:5001/analyze`

#### Spring Boot Integration
- [ ] Thêm config vào `application.properties`:
  ```properties
  ai.pronunciation.service.url=http://pronunciation-service:5001
  ```
- [ ] Tạo `PronunciationClientService.java`:
  ```java
  // Dùng RestTemplate gọi POST /analyze
  // Input: audio bytes (Base64) + expected_text + language
  // Output: PronunciationResult DTO
  ```
- [ ] Xoá hoặc disable: `OpenAIService.speechToText()`
- [ ] Xoá hoặc disable: `OpenAIService.compareSemanticWithEmbedding()`
- [ ] Update `AiEvaluateService.evaluateSpeaking()` → dùng `PronunciationClientService`
- [ ] Thêm `RestTemplate` bean vào config (nếu chưa có)

#### Database Changes
```sql
ALTER TABLE semantic_result ADD COLUMN phoneme_errors JSON;
ALTER TABLE semantic_result ADD COLUMN confidence_score FLOAT;
ALTER TABLE semantic_result ADD COLUMN pronunciation_feedback TEXT;
```

#### Testing
- [ ] Test Python server standalone (Postman/curl)
- [ ] Test Spring Boot → Python server call
- [ ] Test end-to-end: upload audio → result
- [ ] Verify latency < 10 seconds

---

### Week 2: Writing Evaluation — Python FastAPI Server

#### Python Server Setup (`ai-services/writing/`)
- [ ] Tạo thư mục `ai-services/writing/`
- [ ] Cài Python dependencies: `pip install fastapi uvicorn transformers spacy torch`
- [ ] Download IELTS scorer model từ HuggingFace: `KevSun/IELTS_essay_scoring`
- [ ] Viết `main.py` — FastAPI app:
  ```python
  # POST /evaluate
  # GET  /health
  ```
- [ ] Viết `model_loader.py` — load IELTS model khi server khởi động
- [ ] Viết `evaluator.py` — score essay, trả về JSON
- [ ] Viết `requirements.txt`
- [ ] Viết `Dockerfile` (port 5002)
- [ ] Test local: `curl -X POST localhost:5002/evaluate`

#### Spring Boot Integration
- [ ] Thêm config:
  ```properties
  ai.writing.service.url=http://writing-service:5002
  ```
- [ ] Tạo `WritingClientService.java`:
  ```java
  // Dùng RestTemplate gọi POST /evaluate
  // Input: essay (String) + topic (String)
  // Output: WritingScore DTO
  ```
- [ ] Xoá hoặc disable: `GeminiAiService.generateWritingFeedback()`
- [ ] Update `AIService.evaluateWriting()` → dùng `WritingClientService`

#### Database Changes
```sql
ALTER TABLE writing_result ADD COLUMN lexical_score FLOAT;
ALTER TABLE writing_result ADD COLUMN grammar_score FLOAT;
ALTER TABLE writing_result ADD COLUMN coherence_score FLOAT;
ALTER TABLE writing_result ADD COLUMN task_achievement_score FLOAT;
ALTER TABLE writing_result ADD COLUMN ielts_band_score FLOAT;
ALTER TABLE writing_result ADD COLUMN detailed_feedback TEXT;
```

#### Testing
- [ ] Test Python server standalone
- [ ] Test Spring Boot → Python server call
- [ ] Accuracy comparison vs Gemini baseline
- [ ] Verify latency < 5 seconds

---

## Phase 2: Authentication System (Week 3)

### Custom Authentication Implementation

#### Database Schema
- [ ] Create user table
- [ ] Create role table
- [ ] Create user_role mapping table
- [ ] Create refresh_token table
- [ ] Add necessary indexes

```sql
CREATE TABLE users (
  id CHAR(36) PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  username VARCHAR(100) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(255),
  status ENUM('ACTIVE', 'INACTIVE', 'BANNED') DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE roles (
  id INT PRIMARY KEY,
  name ENUM('CUSTOMER', 'CREATOR', 'ADMIN', 'INSTRUCTOR'),
  permissions JSON
);

CREATE TABLE user_roles (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id CHAR(36),
  role_id INT,
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (role_id) REFERENCES roles(id),
  UNIQUE KEY unique_user_role (user_id, role_id)
);

CREATE TABLE refresh_tokens (
  token_id CHAR(36) PRIMARY KEY,
  user_id CHAR(36),
  token_hash VARCHAR(255),
  expires_at TIMESTAMP,
  created_at TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_expires (expires_at)
);
```

#### Services Implementation
- [ ] `AuthenticationService.java`
  - [ ] register(UserRegistrationDTO)
  - [ ] login(email, password)
  - [ ] validateCredentials()
  - [ ] passwordStrengthCheck()

- [ ] `JwtTokenService.java`
  - [ ] generateAccessToken(user)
  - [ ] generateRefreshToken(user)
  - [ ] validateToken(token)
  - [ ] extractClaims(token)

- [ ] `AuthorizationService.java`
  - [ ] getUserRoles(userId)
  - [ ] hasPermission(userId, resource, action)
  - [ ] updateUserRole(userId, roleId)

#### Filters & Configuration
- [ ] Update `JwtAuthenticationFilter.java` (use local JWT instead of Keycloak)
- [ ] Update `SecurityConfig.java` (remove Keycloak configuration)
- [ ] Remove Keycloak dependency from pom.xml
- [ ] Add JJWT dependencies

#### Migration
- [ ] Export users from Keycloak
- [ ] Hash passwords with Bcrypt
- [ ] Import into new users table
- [ ] Verify data integrity
- [ ] Test login for sample users

#### Testing
- [ ] Registration with valid email
- [ ] Registration with duplicate email (should fail)
- [ ] Login with correct credentials
- [ ] Login with wrong password (should fail)
- [ ] Token refresh flow
- [ ] Token expiration handling
- [ ] Role-based access control

---

## Phase 3: Content Generation Agent (Week 4)

### AI Agent Implementation

#### Service Creation
- [ ] Create `ContentGenerationAgent.java`
  ```java
  public class ContentGenerationAgent {
      public ContentPackage generateTopicContent(String topic, StudentLevel level) { }
      public boolean validateContent(Content content) { }
      public void cacheContent(Content content, long ttlSeconds) { }
      public List<String> suggestTopics(StudentLevel level) { }
  }
  ```

#### Database Schema
```sql
CREATE TABLE generated_content (
  id CHAR(36) PRIMARY KEY,
  topic VARCHAR(255),
  level ENUM('BEGINNER', 'INTERMEDIATE', 'ADVANCED'),
  content_type ENUM('LESSON', 'EXERCISE', 'QUIZ'),
  content JSON,
  created_at TIMESTAMP,
  cached_until TIMESTAMP,
  accuracy_score FLOAT,
  human_reviewed BOOLEAN DEFAULT FALSE,
  INDEX idx_topic_level (topic, level)
);
```

#### Integration
- [ ] Update `ModuleService` to use content generation
- [ ] Update `CourseService` to request new content as needed
- [ ] Add caching layer (Redis)
- [ ] Implement cache invalidation strategy

#### Testing
- [ ] Generate content for different topics
- [ ] Validate generated content structure
- [ ] Test cache hit/miss scenarios
- [ ] Performance under load (100 concurrent requests)

---

## Phase 4: Integration & Testing (Week 4-5)

### Integration Testing

#### API Endpoint Testing
- [ ] Test pronunciation evaluation endpoint
- [ ] Test writing evaluation endpoint
- [ ] Test new authentication endpoints
- [ ] Test content generation endpoints
- [ ] Cross-endpoint workflows

#### Regression Testing
- [ ] Existing course functionality
- [ ] Existing user management
- [ ] WebSocket communication
- [ ] File upload/storage

#### Performance Testing
- [ ] Load testing: 1000 concurrent users
- [ ] Model inference performance
- [ ] Database query optimization
- [ ] Cache effectiveness

#### Security Testing
- [ ] SQL injection tests
- [ ] XSS prevention validation
- [ ] CSRF token validation
- [ ] Password security verification
- [ ] Token expiration enforcement

---

## Phase 5: Deployment (Week 5-6)

### Pre-Deployment Checklist

#### Code Quality
- [ ] SonarQube analysis (no critical issues)
- [ ] Code coverage > 80%
- [ ] No deprecated dependency warnings
- [ ] All tests passing

#### Documentation
- [ ] API documentation updated
- [ ] Configuration guide prepared
- [ ] Troubleshooting guide created
- [ ] Runbook for common issues

#### Infrastructure
- [ ] Staging environment ready
- [ ] Database backups configured
- [ ] Monitoring alerts set up
- [ ] Logging aggregation configured

### Deployment Steps

#### Blue-Green Deployment
```
1. Deploy new version to staging
2. Run smoke tests on staging
3. Route 10% traffic to new system (canary)
4. Monitor metrics for 24 hours
5. Increase to 25% traffic
6. Monitor for 24 hours
7. Increase to 50% traffic
8. Monitor for 24 hours
9. Full migration (100%)
```

#### Rollback Plan
- [ ] Keep old system running for 30 days
- [ ] Monitor error rates
- [ ] If issues detected, rollback within 5 minutes
- [ ] Post-incident analysis

---

## Dependency Updates Required

### Remove from pom.xml
```xml
<!-- Keycloak -->
<dependency>
    <groupId>org.keycloak</groupId>
    <artifactId>keycloak-admin-client</artifactId>
    <version>25.0.1</version>
</dependency>
<!-- Keycloak Spring Boot Adapter -->
<dependency>
    <groupId>org.keycloak</groupId>
    <artifactId>keycloak-spring-boot-starter</artifactId>
</dependency>
<!-- OpenAI SDK (nếu có dùng thư viện riêng) -->
<!-- Gemini SDK (nếu có dùng thư viện riêng) -->
<!-- DJL — KHÔNG CẦN (model chạy trong Python server riêng) -->
```

### Add to pom.xml
```xml
<!-- JWT Token Management (thay Keycloak) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>

<!-- Password Encryption -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>

<!-- RestTemplate đã có sẵn trong spring-boot-starter-web -->
<!-- KHÔNG cần thêm DJL hay HuggingFace Java library -->
```

### Thêm vào docker-compose.yml
```yaml
pronunciation-service:
  build: ./ai-services/pronunciation
  ports:
    - "5001:5001"
  volumes:
    - ./models/wav2vec2:/app/models
  networks:
    - backend-network

writing-service:
  build: ./ai-services/writing
  ports:
    - "5002:5002"
  volumes:
    - ./models/ielts:/app/models
  networks:
    - backend-network
```

---

## Configuration Changes

### application.properties Updates

#### Remove Keycloak Config
```properties
# REMOVE THESE LINES
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8080/realms/jpdweb/protocol/openid-connect/certs
keycloak.auth-server-url=...
keycloak.realm=...
keycloak.admin.client-id=...
keycloak.admin.client-secret=...
```

#### Add New Config
```properties
# JWT Configuration
jwt.secret=${JWT_SECRET}
jwt.access-token-expiration=1800000
jwt.refresh-token-expiration=2592000000

# IELTS Model
ielts.model.path=${IELTS_MODEL_PATH}
ielts.model.cache-ttl=3600

# Pronunciation Model
pronunciation.model.path=${PRONUNCIATION_MODEL_PATH}
pronunciation.model.cache-ttl=3600

# Content Generation
content.generation.cache-ttl=86400
content.validation.enabled=true
```

---

## Success Criteria

### Technical
- [ ] All services deployed without errors
- [ ] Model inference time < 10 seconds per request
- [ ] System uptime > 99.9%
- [ ] API response time < 500ms (excluding model inference)
- [ ] Authentication latency < 100ms

### Business
- [ ] Zero critical security issues
- [ ] Model accuracy within 5% of commercial APIs
- [ ] Cost reduction verified (87% savings)
- [ ] User satisfaction maintained or improved
- [ ] Deployment time < 10 minutes

### Quality
- [ ] Test coverage > 80%
- [ ] Zero medium/critical SonarQube issues
- [ ] All documentation updated
- [ ] Team trained on new systems

---

## Contingency Plans

### If Model Inference is Too Slow
- [ ] Implement request queuing
- [ ] Add model quantization
- [ ] Use ensemble of smaller models
- [ ] Implement caching for common inputs

### If Accuracy is Below Target
- [ ] Fine-tune models on domain data
- [ ] Create ensemble models
- [ ] Implement human feedback loop
- [ ] Use hybrid approach (AI + manual review)

### If Authentication Issues Arise
- [ ] Keep Keycloak as backup
- [ ] Implement dual authentication
- [ ] Gradual migration
- [ ] Ready rollback plan

---

## Key Contacts & Escalation

- Technical Lead: [Name]
- DevOps: [Name]
- ML Engineer: [Name]
- Security Team: [Name]

---

**Document Version**: 1.0  
**Last Updated**: May 18, 2026  
**Status**: Ready for Implementation
