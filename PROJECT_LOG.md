# PROJECT LOG

## [2026-05-18 | Phiên phân tích AI Agent] - Đọc 2 file tổng quan kiến trúc - Không sửa code, không đọc file khác

### Hành động
- Đọc PROJECT_OVERVIEW.md (971 dòng)
- Đọc CURRENT_ARCHITECTURE.md (1621 dòng, đọc 1000 dòng đầu)

### Tác động
- Không có thay đổi gì với codebase
- Chỉ phân tích để đánh giá khả năng chuyển đổi sang AI Agent tự gen nội dung

### Ghi chú cho tương lai
- Dự án đã tích hợp Gemini API và OpenAI — nền tảng tốt để xây AI Agent
- AiGenerateController và GeminiAiService là điểm mở rộng chính
- 12 loại content type đã được định nghĩa rõ — Agent cần biết gen đúng schema từng loại
- Cần xem xét bổ sung: orchestration layer, topic → content pipeline, agent tool definitions


## [2026-05-18 | Phiên đọc toàn bộ MD] - Đọc 5 file MD còn lại - Không sửa code

### Hành động
- Đọc ARCHITECTURE_PLAN.md (829 dòng)
- Đọc ARCHITECTURE_COMPARISON.md (633 dòng)
- Đọc IMPLEMENTATION_CHECKLIST.md (458 dòng)
- Đọc README_ARCHITECTURE.md (524 dòng)
- Đọc TECHNICAL_QUICKSTART.md (675 dòng)

### Tác động
- Không thay đổi gì với codebase
- Hiểu đầy đủ kế hoạch transformation 3 giai đoạn của chủ dự án

### Ghi chú cho tương lai
- Kế hoạch lớn hơn ban đầu: không chỉ AI agent mà còn thay Keycloak + thay OpenAI/Gemini bằng local model
- Phase 1: wav2vec2 + IELTS local model thay OpenAI/Gemini
- Phase 2: Custom JWT thay Keycloak
- Phase 3: ContentGenerationAgent (AI tự gen nội dung theo topic)
- Cần cẩn thận với Phase 1: DJL + PyTorch trong Spring Boot là thách thức lớn
- Phase 2 (Keycloak → Custom JWT): rủi ro migration dữ liệu user
- Timeline 6 tuần có thể căng nếu làm solo


## [2026-05-18 | Cập nhật tài liệu — Python AI Servers] - Sửa 4 file MD

### Hành động
Cập nhật kiến trúc từ "DJL nhúng trong JVM" → "2 Python FastAPI server riêng"

### Files đã sửa
1. ARCHITECTURE_PLAN.md
   - Phase 1.1 & 1.2: mô tả Python server thay vì DJL
   - Dependencies: bỏ DJL, thêm ghi chú RestTemplate
   - Deployment: cập nhật 6 containers (bỏ Keycloak + Postgres, thêm 2 Python servers)

2. ARCHITECTURE_COMPARISON.md
   - Sơ đồ proposed architecture: thêm 2 Python server boxes (:5001, :5002)
   - NEW SERVICE CALLS: dùng HTTP call thay model inference trực tiếp
   - Migration Timeline: cập nhật task theo hướng Python server

3. IMPLEMENTATION_CHECKLIST.md
   - Week 1 & 2: task list chi tiết cho Python FastAPI server (main.py, Dockerfile, etc.)
   - Dependencies: bỏ DJL/HuggingFace Java, thêm docker-compose snippet

4. TECHNICAL_QUICKSTART.md
   - System diagram: thêm Python :5001, :5002, :5000
   - Service integration: PronunciationClientService và WritingClientService dùng RestTemplate
   - Config: bỏ MODEL_PATH, thêm service URL config

### Tác động
- Không thay đổi code, chỉ cập nhật tài liệu
- Kiến trúc rõ ràng hơn: Spring Boot chỉ gọi HTTP → Python servers

### Ghi chú cho tương lai
- Khi bắt đầu code: tạo thư mục ai-services/pronunciation/ và ai-services/writing/ trước
- Pattern giống ContentModerationService (port 5000) đang có sẵn — có thể dùng làm reference

