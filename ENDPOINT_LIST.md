# Endpoint List — Jaen E-Learning API

Every HTTP endpoint in the application: **102 REST endpoints** across 30 controllers, plus 9 STOMP
message destinations listed separately at the end.

Generated from the source. The live, always-accurate version of this is Swagger UI at
`/swagger-ui.html` (raw spec at `/v3/api-docs`) once the app is running.

## Conventions used in this document

- **Auth** — every endpoint requires `Authorization: Bearer <JWT>` **unless the row says otherwise**.
  The user is identified from the token's `sub` claim; it is never taken from the body or query string.
  Endpoints under `/api/admin/**` additionally require the `ADMIN` realm role.
- **`—`** in the request column means the endpoint takes no parameters or body beyond the bearer token.
- **`(no body)`** in the response column means a success returns an empty body; the status code carries
  the result.
- Error responses are omitted from the table. Almost all failures return the standard `ErrorResponse`
  shape — see [Error shape](#error-shape) at the end. Endpoints that deviate are flagged with ⚠️.
- ⚠️ marks behaviour that will surprise you. Each is explained in
  [Behaviour worth knowing](#behaviour-worth-knowing).

---

## Authentication — `/api/auth` *(public, no token required)*

| Method | Endpoint | Request | Response |
|---|---|---|---|
| GET | `/api/auth/login-url` | Query: `redirectUri` (required)<br>`?redirectUri=http://localhost:3000/callback` | `200`<pre>{<br>  "loginUrl": "https://keycloak/realms/jpdweb/protocol/openid-connect/auth?client_id=…&code_challenge_method=S256&state=Zm9vYmFy",<br>  "state": "Zm9vYmFy"<br>}</pre> |
| POST | `/api/auth/callback` | Body<pre>{<br>  "code": "8f1e2a3b-…backended-service",<br>  "state": "Zm9vYmFy",<br>  "redirectUri": "http://localhost:3000/callback"<br>}</pre> | `200` + sets `access_token` / `refresh_token` HttpOnly cookies<pre>{<br>  "success": true,<br>  "expiresIn": 300<br>}</pre> |
| POST | `/api/auth/refresh` | Cookie: `refresh_token` (no body) | `200` + rewrites both cookies<pre>{ "success": true }</pre> |
| POST | `/api/auth/logout` | Cookie: `refresh_token` (optional, no body) | `200` + clears both cookies<pre>{ "success": true }</pre> |

## Course catalogue — `/api/course` *(public, no token required)*

| Method | Endpoint | Request | Response |
|---|---|---|---|
| GET | `/api/course/recommend_courses` | — | `200`<pre>[<br>  {<br>    "id": 5,<br>    "name": "Japanese Conversation Mastery",<br>    "img": "https://…/jp-conversation.jpg",<br>    "numberStudent": 4500,<br>    "rating": 4.8,<br>    "instructor": "Akiko Suzuki",<br>    "price": 699000,<br>    "language": "JAPANESE"<br>  }<br>]</pre> |
| GET | `/api/course/search` | Query: `name` (required), `page` (0), `size` (10)<br>`?name=tiếng nhật&page=0&size=10` | `200`<pre>{<br>  "content": [ /* CourseInfDto */ ],<br>  "currentPage": 0,<br>  "totalPages": 3,<br>  "totalElements": 27,<br>  "size": 10,<br>  "hasNext": true,<br>  "hasPrevious": false<br>}</pre> |
| GET | `/api/course/{id}` | Path: `id` | `200`<pre>{<br>  "courseId": 12,<br>  "name": "Tiếng Nhật sơ cấp N5",<br>  "description": "…",<br>  "language": "JAPANESE",<br>  "teachingLanguage": "VIETNAMESE",<br>  "price": 699000,<br>  "urlImg": "https://…",<br>  "createdAt": "2025-11-03",<br>  "lastUpdate": "2026-06-18",<br>  "public": true,<br>  "ban": false,<br>  "accessMode": "PAID",<br>  "learningObject": "…",<br>  "requirements": "…",<br>  "targetAudience": "…",<br>  "creator": { "creatorId": "7f3c1a90-…", "fullName": "Akiko Suzuki", "totalCourses": 12, "averageRating": 4.8 },<br>  "chapters": [ /* Chapter */ ],<br>  "totalStudents": 4500,<br>  "totalFeedbacks": 312,<br>  "averageRating": 4.8,<br>  "totalModules": 48,<br>  "feedbacks": [ { "feedbackId": 884, "content": "…", "rate": 5, "createDate": "2026-05-14", "customerId": "9c2e5f77-…" } ]<br>}</pre> |

## Account

| Method | Endpoint | Request | Response |
|---|---|---|---|
| GET | `/api/customer/account_infor` ⚠️ | — | `200`<pre>{<br>  "userName": "thuynguyen",<br>  "familyName": "Nguyễn",<br>  "role": "USER,CREATOR",<br>  "givenName": "Thuỷ",<br>  "createDate": null,<br>  "email": "user@example.com",<br>  "creator": true,<br>  "admin": false<br>}</pre> |

## Customer — `/api/customer`

| Method | Endpoint | Request | Response |
|---|---|---|---|
| POST | `/api/customer/upload_profile` ⚠️ | `multipart/form-data`<br>`fullName` (required, ≤100)<br>`phone` (`^[0-9]{10,11}$`)<br>`bio` (≤1000)<br>`profileImage` (file)<br>`agreedToTerms` (required, boolean) | `201`<pre>{<br>  "fullName": "Nguyễn Thị Thuỷ",<br>  "phone": "0912345678",<br>  "bio": "Giáo viên tiếng Nhật 5 năm kinh nghiệm.",<br>  "imgUrl": "https://…/avatar.jpg",<br>  "certificateUrl": null,<br>  "paypalEmail": null,<br>  "status": "PENDING"<br>}</pre> |
| GET | `/api/customer/learning_course_list` | — | `200`<pre>{<br>  "cardDtos": [<br>    { "courseId": 12, "course_name": "Tiếng Nhật N5", "course_img": "https://…", "progress": 65.5 }<br>  ],<br>  "wishlistDtos": [<br>    { "courseId": 41, "course_name": "Kanji 500 chữ", "course_img": "https://…" }<br>  ]<br>}</pre> |

## Wishlist / Enrollment / Feedback / Reports

| Method | Endpoint | Request | Response |
|---|---|---|---|
| POST | `/api/wishlist/{courseId}` | Path: `courseId` | `201` (no body) |
| POST | `/api/enroll/{id}` ⚠️ | Path: `id`<br>Query: `joinKey` (required; ignored for public courses)<br>`?joinKey=N5-2026-SPRING` | `200` (no body)<br>`400` (no body) when the private-course key is wrong |
| POST | `/api/customer/feedback/{courseId}` ⚠️ | Path: `courseId`<br>Query: `rate` (required, int), `detail` (required)<br>`?rate=5&detail=Khóa học rất dễ hiểu` | `204` (no body) |
| POST | `/api/customer/report` ⚠️ | Body<pre>{<br>  "type": "MISLEADING_INFORMATION",<br>  "detail": "Nội dung không đúng mô tả.",<br>  "courseId": 12<br>}</pre> | `200` (no body) |

## Course learning — `/api/customer/learning/{courseId}`

| Method | Endpoint | Request | Response |
|---|---|---|---|
| GET | `/api/customer/learning/{courseId}/courseOverview` | Path: `courseId` | `200`<pre>{<br>  "name": "Tiếng Nhật sơ cấp N5",<br>  "public": true,<br>  "language": "JAPANESE",<br>  "teachingLanguage": "VIETNAMESE",<br>  "chapters": [ /* Chapter, each module carrying its contentTypes */ ]<br>}</pre> |
| GET | `/api/customer/learning/{courseId}/{chapterId}/{moduleId}/moduleContent` | Path: `courseId`, `chapterId`, `moduleId`<br>Query: `typeOfContent` (required)<br>`?typeOfContent=FLASHCARD` | `200`<pre>[ /* ModuleContent */ ]</pre> |
| POST | `/api/customer/learning/{courseId}/{moduleId}/finish_content` ⚠️ | Path: `courseId`, `moduleId`<br>Query: `type` (required)<br>`?type=FLASHCARD` | `204` (no body) |

## Course comments — `/api/courses/{courseId}/comments`

| Method | Endpoint | Request | Response |
|---|---|---|---|
| GET | `/api/courses/{courseId}/comments` ⚠️ | Path: `courseId` | `200`<pre>[<br>  { "comment": "Bài 12 giải thích rất rõ!", "createBy": "Anonymous" }<br>]</pre> |
| POST | `/api/courses/{courseId}/comments` | Path: `courseId`<br>Body<pre>{ "content": "Bài 12 giải thích rất rõ!" }</pre> | `201`<pre>{<br>  "comment": "Bài 12 giải thích rất rõ!",<br>  "createBy": "9c2e5f77-11ab-42d0-8b3e-6ce0d4a9e777"<br>}</pre> |
| PUT | `/api/courses/{courseId}/comments/{commentId}` ⚠️ | Path: `courseId`, `commentId`<br>Body<pre>{ "content": "Nội dung đã sửa" }</pre> | `200`<pre>{ "comment": "Nội dung đã sửa", "createBy": "9c2e5f77-…" }</pre> |
| DELETE | `/api/courses/{courseId}/comments/{commentId}` ⚠️ | Path: `courseId`, `commentId` | `204` (no body) |

## Personal dictionary — `/api/customer/dictionary`

| Method | Endpoint | Request | Response |
|---|---|---|---|
| GET | `/api/customer/dictionary` | — | `200`<pre>[<br>  {<br>    "rwId": 204,<br>    "word": "天気",<br>    "meaning": "thời tiết",<br>    "description": "Đọc là てんき.",<br>    "synonyms": ["気候", "天候"],<br>    "example": ["今日はいい天気ですね"],<br>    "language": "JAPANESE",<br>    "voteCount": 3,<br>    "customerEmail": "user@example.com"<br>  }<br>]</pre> |
| POST | `/api/customer/dictionary` ⚠️ | Body<pre>{<br>  "word": "天気",<br>  "meaning": "thời tiết",<br>  "description": "Đọc là てんき.",<br>  "synonyms": ["気候", "天候"],<br>  "example": ["今日はいい天気ですね"],<br>  "language": "JAPANESE"<br>}</pre> | `201` — the submitted body echoed back verbatim |
| PUT | `/api/customer/dictionary` ⚠️ | Body — full replacement, `rwId` identifies the target<pre>{<br>  "rwId": 204,<br>  "word": "天気",<br>  "meaning": "thời tiết, khí hậu",<br>  "description": "…",<br>  "synonyms": ["気候"],<br>  "example": ["明日の天気は？"],<br>  "language": "JAPANESE"<br>}</pre> | `200` — the submitted body echoed back verbatim |
| DELETE | `/api/customer/dictionary/{rwId}` | Path: `rwId` | `204` (no body) |
| POST | `/api/customer/dictionary/import` ⚠️ | `multipart/form-data`<br>`file` — JSON, non-empty, ≤5 MB | `200`<pre>{<br>  "importedCount": 42,<br>  "skippedCount": 2,<br>  "invalidCount": 1,<br>  "skippedWords": ["天気", "学校"],<br>  "invalidReasons": ["Một entry không có word"]<br>}</pre> |

## AI evaluation — `/api/customer/evaluate`

| Method | Endpoint | Request | Response |
|---|---|---|---|
| POST | `/api/customer/evaluate/evaluate/{moduleId}` ⚠️ | Path: `moduleId`<br>`multipart/form-data`<br>`audio` (file, required)<br>`sentence` (required)<br>`language` (declared optional, in practice required) | `200`<pre>{<br>  "match": true,<br>  "similarity_score": 0.92,<br>  "user_answer": "今日はいい天気ですね",<br>  "expected_answer": "今日はいい天気ですね",<br>  "feedback": "Phát âm rõ ràng.",<br>  "has_error": false,<br>  "error_message": null<br>}</pre> |
| POST | `/api/customer/evaluate/evaluateWriting` ⚠️ | Body<pre>{<br>  "writingText": "Yesterday I went to the library.",<br>  "language": "ENGLISH"<br>}</pre> | `200`<pre>{<br>  "grammar": 7.5,<br>  "vocabulary": 8.0,<br>  "feedback": "Good range of vocabulary."<br>}</pre> |
| POST | `/api/customer/evaluate/magic-diary` ⚠️ | Body — all four fields required<pre>{<br>  "writingText": "Hôm nay tôi học về thời Edo.",<br>  "language": "VIETNAMESE",<br>  "lessonTitle": "Thời kỳ Edo",<br>  "referenceNotes": "Mạc phủ Tokugawa: 1603-1868."<br>}</pre> | `200`<pre>{<br>  "contentAccuracy": 8.5,<br>  "grammar": 7.0,<br>  "vocabulary": 7.5,<br>  "feedback": "Bạn nắm đúng mốc thời gian.",<br>  "nextStep": "Viết thêm một đoạn về sakoku."<br>}</pre> |
| POST | `/api/customer/evaluate/magic-diary/question` ⚠️ | Body — all three fields required<pre>{<br>  "question": "Tại sao có chính sách bế quan tỏa cảng?",<br>  "lessonTitle": "Thời kỳ Edo",<br>  "referenceNotes": "Mạc phủ Tokugawa: 1603-1868."<br>}</pre> | `200`<pre>{<br>  "status": "ANSWERED",<br>  "answer": "Để hạn chế ảnh hưởng phương Tây.",<br>  "correction": "",<br>  "suggestedQuestion": "Sakoku kết thúc thế nào?"<br>}</pre> |
| POST | `/api/customer/evaluate/ielts-brainstorm` | Body<pre>{ "prompt": "Some people believe university education should be free…" }</pre> | `200`<pre>{ "feedback": "Agree side: widens access… Disagree side: high tax burden…" }</pre> |

## Inspiration videos & TTS

| Method | Endpoint | Request | Response |
|---|---|---|---|
| GET | `/api/inspiration-videos/today` ⚠️ | — | `200`<pre>{<br>  "date": "2026-07-22",<br>  "videos": [<br>    {<br>      "id": "inspiration/2026/never-give-up.mp4",<br>      "title": "Never Give Up",<br>      "description": "Inspiration video",<br>      "speaker": "",<br>      "topic": "motivation",<br>      "videoUrl": "https://r2…?X-Amz-Signature=…",<br>      "thumbnailUrl": "https://r2…?X-Amz-Signature=…",<br>      "transcript": null,<br>      "durationSeconds": 412,<br>      "difficultyLevel": "INTERMEDIATE"<br>    }<br>  ]<br>}</pre> |
| GET | `/api/tts` ⚠️ *(public, no token)* | Query: `text` (required), `lang` (`ja-JP`)<br>`?text=今日はいい天気ですね&lang=ja-JP` | `200` `audio/mpeg` — MP3 byte stream, not JSON |

## Live quiz — `/api/quiz`

| Method | Endpoint | Request | Response |
|---|---|---|---|
| POST | `/api/quiz/create` ⚠️ | Body<pre>{ "kahootId": 77, "teacherName": "Cô Thuỷ" }</pre> | `200`<pre>{<br>  "sessionCode": "482913",<br>  "sessionId": "3f1a9c22-77bd-4e0a-9b41-5c2d8e6f0a11",<br>  "qrCodeUrl": "https://…/create-qr-code/?data=…",<br>  "joinUrl": "http://localhost:3000/quiz/join/482913",<br>  "title": "Ôn tập Kanji N5",<br>  "totalQuestions": 15<br>}</pre> |
| POST | `/api/quiz/join` ⚠️ *(public, no token)* | Body<pre>{ "sessionCode": "482913", "participantName": "Thuy" }</pre> | `200`<pre>{<br>  "success": true,<br>  "participant": { "participantId": "b7d1e0c4-…", "name": "Thuy", "sessionCode": "482913", "joinedAt": "2026-07-22T09:15:30.412", "currentScore": 0 },<br>  "session": { /* SessionInfo */ }<br>}</pre> |
| GET | `/api/quiz/{sessionCode}` ⚠️ | Path: `sessionCode` | `200`<pre>{<br>  "sessionId": "3f1a9c22-…",<br>  "sessionCode": "482913",<br>  "kahootId": 77,<br>  "title": "Ôn tập Kanji N5",<br>  "questionIds": [9042, 9043],<br>  "totalQuestions": 15,<br>  "teacherId": "7f3c1a90-…",<br>  "teacherName": "Cô Thuỷ",<br>  "status": "WAITING",<br>  "currentQuestionIndex": null,<br>  "currentQuestionId": null,<br>  "currentQuestionType": null,<br>  "questionStartTime": null,<br>  "questionTimeLimit": 30,<br>  "acceptingAnswers": false,<br>  "questionEndTime": null,<br>  "createdAt": "2026-07-22T09:10:00",<br>  "startedAt": null,<br>  "finishedAt": null,<br>  "totalParticipants": 24,<br>  "currentAnswers": 0,<br>  "showLeaderboardAfterEachQuestion": true,<br>  "randomizeQuestions": false,<br>  "randomizeOptions": false<br>}</pre> |
| GET | `/api/quiz/{sessionCode}/participants` ⚠️ | Path: `sessionCode` | `200`<pre>[<br>  { "participantId": "b7d1e0c4-…", "name": "Thuy", "sessionCode": "482913", "joinedAt": "2026-07-22T09:15:30.412", "currentScore": 1750 }<br>]</pre> |
| DELETE | `/api/quiz/{sessionCode}` ⚠️ | Path: `sessionCode` | `200`<pre>{ "success": true, "message": "Session deleted" }</pre> |
| POST | `/api/quiz/submit-answer` | Body<pre>{<br>  "sessionCode": "482913",<br>  "participantId": "b7d1e0c4-…",<br>  "questionId": 9042,<br>  "answer": "B"<br>}</pre> | `200`<pre>{<br>  "success": true,<br>  "message": "Answer submitted",<br>  "totalAnswered": 18,<br>  "totalParticipants": 24<br>}</pre> |
| POST | `/api/quiz/end-question/{sessionCode}` ⚠️ | Path: `sessionCode` | `200`<pre>{<br>  "questionId": 9042,<br>  "correctAnswer": "B",<br>  "results": [<br>    { "participantId": "b7d1e0c4-…", "participantName": "Thuy", "answer": "B", "correct": true, "points": 850, "totalScore": 1750 }<br>  ]<br>}</pre> |

## Creator profile — `/api/creator`

| Method | Endpoint | Request | Response |
|---|---|---|---|
| GET | `/api/creator/getAccount` | — | `200`<pre>{<br>  "fullName": "Akiko Suzuki",<br>  "phone": "0912345678",<br>  "bio": "…",<br>  "imgUrl": "https://…/avatar.jpg",<br>  "certificateUrl": ["https://…/jlpt-n1.pdf"],<br>  "paypalEmail": "akiko@example.com",<br>  "status": "SUCCESS"<br>}</pre> |
| POST | `/api/creator/upade_certificate` ⚠️ | `multipart/form-data`<br>`certificateFile` — repeat the part for several files | `201` (no body) |
| DELETE | `/api/creator/removeCertificate` | Query: `certificateUrl` (required) | `200` `text/plain`<pre>Certificate removed successfully</pre> |
| GET | `/api/creator/auditlog` | — | `200`<pre>[<br>  {<br>    "auditLogId": 15,<br>    "actionType": "APPROVE_CERT",<br>    "targetCreatorId": "7f3c1a90-…",<br>    "adminEmail": "admin@jaen.vn",<br>    "reason": "Chứng chỉ hợp lệ",<br>    "timestamp": "2026-07-20T10:04:11"<br>  }<br>]</pre> |
| GET | `/api/creator/enrollment/{courseId}` | Path: `courseId` | `200`<pre>[ /* Enrollment, feedback eagerly loaded */ ]</pre> |

## Creator courses — `/api/creator/course`

| Method | Endpoint | Request | Response |
|---|---|---|---|
| POST | `/api/creator/course/create` ⚠️ | `multipart/form-data`<br>`name` (required, ≤255)<br>`description` (required, ≤5000)<br>`targetAudience` (required, ≤1000)<br>`requirements` (≤2000)<br>`learningObject` (≤2000)<br>`language` (required)<br>`teachingLanguage` (required)<br>`price` (≥0)<br>`imgFile` (file)<br>`accessMode` (required) | `201`<pre>{<br>  "id": 12,<br>  "name": "Tiếng Nhật sơ cấp N5",<br>  "createdDate": "2026-07-22",<br>  "studentCount": 0,<br>  "reviewCount": 0.0,<br>  "rating": 0.0,<br>  "image": "https://…/n5-cover.jpg",<br>  "type": "PUBLIC",<br>  "public": false,<br>  "joinKey": "N5-2026-SPRING"<br>}</pre> |
| GET | `/api/creator/course` | — | `200`<pre>[ /* CourseCardDto */ ]</pre> |
| GET | `/api/creator/course/{id}` | Path: `id` | `200`<pre>{<br>  "name": "Tiếng Nhật sơ cấp N5",<br>  "public": false,<br>  "language": "JAPANESE",<br>  "teachingLanguage": "VIETNAMESE",<br>  "chapters": [ /* Chapter */ ]<br>}</pre> |
| GET | `/api/creator/course/{id}/setCourseStatus` ⚠️ | Path: `id` | `200` (no body) |
| GET | `/api/creator/course/retrieve_CommercialCourese` | — | `200`<pre>[<br>  { "courseId": 12, "title": "Tiếng Nhật N5", "students": 4500, "rating": 4.8, "urlImg": "https://…" }<br>]</pre> |

## Chapters & modules — `/api/creator/{courseId}/…`

| Method | Endpoint | Request | Response |
|---|---|---|---|
| POST | `/api/creator/{courseId}/chapter` | Path: `courseId`<br>Query: `chapterName` (required)<br>`?chapterName=Chương 1: Hiragana` | `201`<pre>{ /* Chapter */ }</pre> |
| DELETE | `/api/creator/{courseId}/chapter/{chapterId}` | Path: `courseId`, `chapterId` | `204` (no body) |
| PUT | `/api/creator/{courseId}/chapter/{chapterID}/update` ⚠️ | Path: `courseId` (unused), `chapterID`<br>Query: `name` (required) | `204` (no body) |
| POST | `/api/creator/{courseId}/{chapterId}/module` | Path: `courseId`, `chapterId`<br>Query: `moduleName` (required) | `201`<pre>{ /* Module */ }</pre> |
| DELETE | `/api/creator/{courseId}/{chapterId}/module/{moduleId}` | Path: `courseId`, `chapterId`, `moduleId` | `204` (no body) |
| PUT | `/api/creator/{courseId}/{chapterId}/module/{moduleId}/update` ⚠️ | Path: `courseId` (unused), `chapterId` (unused), `moduleId`<br>Query: `name` (required) | `204` — **unreachable today, always `401`** |

## Module content — `/api/creator/{courseId}/{chapterId}/{moduleId}`

| Method | Endpoint | Request | Response |
|---|---|---|---|
| GET | `/api/creator/{courseId}/{chapterId}/{moduleId}` | Path: `courseId`, `chapterId`, `moduleId`<br>Query: `type` (required)<br>`?type=FLASHCARD` | `200`<pre>[ /* ModuleContent */ ]</pre> |
| POST | `/api/creator/{courseId}/{chapterId}/{moduleId}` ⚠️ | Path variables ignored — target is `moduleId` in the body<br>Body<pre>{<br>  "moduleId": 56,<br>  "moduleContent": [ /* ModuleContent */ ]<br>}</pre> | `200`<pre>[ /* saved ModuleContent, with NEW mcId values */ ]</pre> |
| DELETE | `/api/creator/{courseId}/{chapterId}/{moduleId}/{moduleContentId}` | Path: all four | `200` (no body) |
| DELETE | `/api/creator/{courseId}/{chapterId}/{moduleId}/deleteModuleContentByType` | Path: `courseId`, `chapterId`, `moduleId`<br>Query: `type` (required) | `204` (no body) |

## Kahoot quizzes — `/api/creator/kahoot`

| Method | Endpoint | Request | Response |
|---|---|---|---|
| GET | `/api/creator/kahoot/retrieveAll` | — | `200`<pre>[<br>  { "title": "Ôn tập Kanji N5", "createDate": "2026-07-01T14:22:10", "numberQuestion": 15, "id": 77 }<br>]</pre> |
| POST | `/api/creator/kahoot/create` | Query: `title` (required) | `201`<pre>{ "title": "Ôn tập Kanji N5", "createDate": "2026-07-22T09:15:30.412", "numberQuestion": 0, "id": 77 }</pre> |
| PUT | `/api/creator/kahoot/{kahootId}` | Path: `kahootId`<br>Query: `newTitle` (required) | `204` (no body) |
| DELETE | `/api/creator/kahoot/{kahootId}` | Path: `kahootId` | `204` (no body) |
| GET | `/api/creator/kahoot/{kahootId}/moduleContents` | Path: `kahootId` | `200`<pre>[ /* ModuleContent */ ]</pre> |
| POST | `/api/creator/kahootModuleContent/{kahootId}` ⚠️ | Path: `kahootId`<br>Body<pre>[ /* ModuleContent; mcId null or negative = new */ ]</pre> | `200`<pre>[ /* saved ModuleContent, with NEW mcId values */ ]</pre> |
| DELETE | `/api/creator/kahootModuleContent/{kahootId}/{moduleContentId}` | Path: `kahootId`, `moduleContentId` | `204` (no body) |

## Creator file storage & AI — `/api/creator`

| Method | Endpoint | Request | Response |
|---|---|---|---|
| POST | `/api/creator/uploadFile/savePdf` | `multipart/form-data` — `pdf` (file) | `200` `text/plain`<pre>https://firebasestorage.googleapis.com/…/n5-workbook.pdf?alt=media</pre> |
| POST | `/api/creator/uploadFile/saveImg` | `multipart/form-data` — `img` (file) | `200` `text/plain`<pre>https://firebasestorage.googleapis.com/…/n5-cover.jpg?alt=media</pre> |
| DELETE | `/api/creator/uploadFile/delete_file` | Query: `url` (required) | `204` (no body) |
| POST | `/api/creator/AI/generateFeeback` ⚠️ | Body<pre>{<br>  "question": "Describe what you did yesterday.",<br>  "answer": "I go to school yesterday."<br>}</pre> | `201` `text/plain`<pre>Good attempt. The verb should be past tense: 'I went to school yesterday.'</pre> |

## Admin — courses — `/api/admin/courses` *(ADMIN role)*

| Method | Endpoint | Request | Response |
|---|---|---|---|
| GET | `/api/admin/courses` | Query: `page` (0, ≥0), `size` (20, 1–100), `search` (optional) | `200`<pre>{<br>  "success": true,<br>  "data": {<br>    "content": [<br>      { "courseId": 12, "name": "Tiếng Nhật N5", "creatorName": "Akiko Suzuki", "creatorId": 0, "isBan": false, "language": "JAPANESE", "numberReports": 3, "numberStudent": 4500, "avtRating": 4.8 }<br>    ],<br>    "totalElements": 137,<br>    "totalPages": 7,<br>    "number": 0,<br>    "size": 20<br>  },<br>  "error": null,<br>  "timestamp": "2026-07-22T09:15:30.412"<br>}</pre> |
| GET | `/api/admin/courses/{courseId}` | Path: `courseId` | `200`<pre>{<br>  "success": true,<br>  "data": { /* CourseDescriptionDto fields */, "reports": [ /* Report */ ] },<br>  "error": null,<br>  "timestamp": "2026-07-22T09:15:30.412"<br>}</pre> |
| POST | `/api/admin/courses/{courseId}/ban` | Path: `courseId` | `200`<pre>{ "success": true, "data": "Course banned successfully", "error": null, "timestamp": "…" }</pre> |
| POST | `/api/admin/courses/{courseId}/unban` | Path: `courseId` | `200`<pre>{ "success": true, "data": "Course unbanned successfully", "error": null, "timestamp": "…" }</pre> |

## Admin — creators — `/api/admin/creators` *(ADMIN role)*

| Method | Endpoint | Request | Response |
|---|---|---|---|
| GET | `/api/admin/creators` | Query: `status` (optional), `search` (optional), `page` (0), `size` (20) | `200`<pre>{<br>  "content": [<br>    { "creatorId": "7f3c1a90-…", "fullName": "Akiko Suzuki", "imageUrl": "https://…", "status": "SUCCESS", "balance": 15750000.0, "totalCourses": 12, "totalStudents": 2847, "avgRating": 4.8, "warningCount": 1, "createDate": "2025-11-03" }<br>  ],<br>  "totalElements": 58,<br>  "totalPages": 3,<br>  "number": 0,<br>  "size": 20<br>}</pre> |
| GET | `/api/admin/creators/{creatorId}` ⚠️ | Path: `creatorId` | `200`<pre>{<br>  "creatorId": "7f3c1a90-…",<br>  "fullName": "Akiko Suzuki",<br>  "phone": "0912345678",<br>  "titleSelf": "…",<br>  "imageUrl": "https://…",<br>  "status": "SUCCESS",<br>  "certificateUrls": ["https://…/jlpt-n1.pdf"],<br>  "paymentEmail": "akiko@example.com",<br>  "balance": 15750000.0,<br>  "totalRevenue": 82400000.0,<br>  "totalCourses": 12,<br>  "totalStudents": 2847,<br>  "avgRating": 4.8,<br>  "warningCount": 1,<br>  "reputationScore": 82,<br>  "isBanned": false,<br>  "bannedUntil": null,<br>  "recentReports": [ { "reportId": 551, "reportType": "MISLEADING_INFORMATION", "detail": "…", "status": "PENDING", "createdAt": "2026-07-18", "reviewedAt": null } ],<br>  "recentCourses": [ /* CourseCardDto */ ],<br>  "createDate": "2025-11-03",<br>  "lastActivity": "2026-07-21"<br>}</pre> |
| GET | `/api/admin/creators/pending-certificates` | — | `200`<pre>[<br>  { "creatorId": "7f3c1a90-…", "fullName": "Akiko Suzuki", "certificateUrls": ["https://…/jlpt-n1.pdf"], "submittedAt": "2026-07-20", "status": "PENDING", "adminNote": null }<br>]</pre> |
| POST | `/api/admin/creators/{creatorId}/approve-certificate` | Path: `creatorId`<br>Query: `adminNote` (required) | `200`<pre>{ "message": "Certificate approved successfully" }</pre> |
| POST | `/api/admin/creators/{creatorId}/reject-certificate` | Path: `creatorId`<br>Body<pre>{ "reason": "Chứng chỉ không đọc được" }</pre> | `200`<pre>{ "message": "Certificate rejected" }</pre> |
| POST | `/api/admin/creators/{creatorId}/warn` ⚠️ | Path: `creatorId`<br>Query: `reason` (required) | `200`<pre>{ "message": "Warning issued successfully" }</pre> |
| POST | `/api/admin/creators/{creatorId}/ban` ⚠️ | Path: `creatorId`<br>Body — both values are strings<pre>{ "reason": "Vi phạm nhiều lần", "durationDays": "30" }</pre><br>omit `durationDays` for a permanent ban | `200`<pre>{ "message": "Creator banned successfully" }</pre> |
| POST | `/api/admin/creators/{creatorId}/unban` ⚠️ | Path: `creatorId`<br>Body (optional)<pre>{ "reason": "Khiếu nại được chấp nhận" }</pre> | `200`<pre>{ "message": "Creator unbanned successfully" }</pre> |
| GET | `/api/admin/creators/{creatorId}/violations` | Path: `creatorId` | `200`<pre>[ /* Report */ ]</pre> |
| GET | `/api/admin/creators/{creatorId}/audit-logs` | Path: `creatorId` | `200`<pre>[ /* AuditLog */ ]</pre> |

## Admin — dashboard — `/api/admin/dashboard` *(ADMIN role)*

All responses use the `controller.admin.ApiResponse` envelope:
`{ "success": true, "message": "…", "data": { … }, "timestamp": "2026-07-22 09:15:30" }` — the shape of
`data` is named in each row.

| Method | Endpoint | Request | Response |
|---|---|---|---|
| GET | `/api/admin/dashboard/overview` | Query: `startDate`, `endDate` (`yyyy-MM-dd`, both optional) | `200` — `data` = `AdminDashboardOverviewResponse` (all sections below combined) |
| GET | `/api/admin/dashboard/stats/users` | — | `200` — `data` = `UserStats` |
| GET | `/api/admin/dashboard/stats/courses` | — | `200` — `data` = `CourseStats` |
| GET | `/api/admin/dashboard/stats/enrollments` | Query: `days` (30) | `200` — `data` = `EnrollmentStats` |
| GET | `/api/admin/dashboard/stats/moderation` | — | `200` — `data` = `ModerationStats` |
| GET | `/api/admin/dashboard/stats/engagement` | Query: `days` (30) | `200` — `data` = `EngagementStats` |
| GET | `/api/admin/dashboard/stats/system-health` | — | `200` — `data` = `SystemHealthStats` |
| GET | `/api/admin/dashboard/top-courses` | Query: `limit` (10), `sortBy` (`enrollments`) | `200` — `data` = `TopCoursesResponse` |
| GET | `/api/admin/dashboard/recent-activities` | Query: `limit` (20) | `200` — `data` = `RecentActivitiesResponse` |
| GET | `/api/admin/dashboard/top-creators` | Query: `limit` (10) | `200` — `data` = `TopCreatorsResponse` |
| GET | `/api/admin/dashboard/export` | Query: `format` (`csv` or `excel`), `startDate`, `endDate` | `200` — file download, `text/csv` or `.xlsx`; **not** the envelope |

## Voice practice — `/api/voice`

| Method | Endpoint | Request | Response |
|---|---|---|---|
| GET | `/api/voice/characters` | — | `200`<pre>[<br>  {<br>    "id": "tanaka-sensei",<br>    "displayName": "Tanaka Sensei",<br>    "description": "Giáo viên kiên nhẫn, nói chậm.",<br>    "category": "teacher",<br>    "liveVoice": "Kore"<br>  }<br>]</pre> |
| GET | `/api/voice/characters/{characterId}` ⚠️ | Path: `characterId` | `200`<pre>{ "id": "tanaka-sensei", "displayName": "Tanaka Sensei", "description": "…", "category": "teacher", "liveVoice": "Kore" }</pre> |
| POST | `/api/voice/live/token` ⚠️ | Body<pre>{ "characterId": "tanaka-sensei" }</pre> | `200`<pre>{<br>  "accessToken": "auth_tokens/abc123def456",<br>  "model": "gemini-2.0-flash-live-001",<br>  "characterId": "tanaka-sensei",<br>  "expiresAt": "2026-07-22T09:45:30Z"<br>}</pre> |
| POST | `/api/voice/progress/sessions` ⚠️ | Body<pre>{ "characterId": "tanaka-sensei" }</pre> | `201`<pre>{<br>  "id": 418,<br>  "characterId": "tanaka-sensei",<br>  "startedAt": "2026-07-22T09:15:30.412",<br>  "endedAt": null,<br>  "durationSeconds": 0,<br>  "completed": false<br>}</pre> |
| POST | `/api/voice/progress/sessions/{sessionId}/complete` ⚠️ | Path: `sessionId` | `200`<pre>{<br>  "id": 418,<br>  "characterId": "tanaka-sensei",<br>  "startedAt": "2026-07-22T09:15:30.412",<br>  "endedAt": "2026-07-22T09:21:12.902",<br>  "durationSeconds": 342,<br>  "completed": true<br>}</pre> |
| GET | `/api/voice/progress/daily` | — | `200`<pre>{<br>  "date": "2026-07-22",<br>  "conversationCount": 3,<br>  "uniqueCharactersCount": 2,<br>  "totalSeconds": 742,<br>  "totalMinutes": 12,<br>  "charactersTarget": 3,<br>  "minutesTarget": 15,<br>  "charactersProgress": 0.67,<br>  "minutesProgress": 0.8,<br>  "completed": false<br>}</pre> |

---

## WebSocket (STOMP) — not HTTP

Handshake at **`/ws-quiz`** (SockJS, all origins allowed, `permitAll`). Clients **send** to `/app/…` and
**subscribe** to `/topic/…`. These carry no HTTP status codes and never appear in the OpenAPI spec.

| Direction | Destination | Payload | Broadcast to |
|---|---|---|---|
| send | `/app/quiz/join/{sessionCode}` | `{ "sessionCode": "482913", "participantName": "Thuy" }` | `/topic/quiz/{sessionCode}/participants` — `{ "type": "PARTICIPANT_JOINED", "participant": {…}, "totalParticipants": 24 }` |
| send | `/app/quiz/{sessionCode}/get-participants` | *(none)* | `/topic/quiz/{sessionCode}/participants` — `{ "type": "PARTICIPANTS_LIST", "participants": [...], "totalParticipants": 24 }` |
| send | `/app/quiz/{sessionCode}/start` | *(none)* | `/topic/quiz/{sessionCode}` |
| send | `/app/quiz/{sessionCode}/next-question` | *(none)* | `/topic/quiz/{sessionCode}` |
| send | `/app/quiz/{sessionCode}/submit-answer` | `{ "sessionCode": "482913", "participantId": "b7d1e0c4-…", "questionId": 9042, "answer": "B" }` | `/topic/quiz/{sessionCode}` |
| send | `/app/quiz/{sessionCode}/end-question` | *(none)* | `/topic/quiz/{sessionCode}` |
| send | `/app/quiz/{sessionCode}/show-leaderboard` | *(none)* | `/topic/quiz/{sessionCode}` |
| send | `/app/quiz/{sessionCode}/end-quiz` | *(none)* | `/topic/quiz/{sessionCode}` |
| send | `/app/quiz/ping` | *(none)* | `/topic/quiz/test` — `{ "message": "PONG" }` |

Every handler swallows its exceptions, so a failed message produces **no reply at all** rather than an
error frame. The client sees silence.

---

## Error shape

Most failures return `ErrorResponse`:

```json
{
  "code": "COURSE_NOT_FOUND",
  "message": "Course not found with id: 12",
  "userMessage": "Tài nguyên không được tìm thấy",
  "path": "/api/course/12",
  "timestamp": "2026-07-22T09:15:30.412",
  "details": null,
  "traceId": "0b6f2c1e-9a44-4f0d-9d6e-2f1c8b7a1234",
  "success": false,
  "status": 404,
  "responseType": "NOT_FOUND"
}
```

On a validation failure `code` is `VALIDATION_ERROR` and `details` is a field→message map:

```json
{ "details": { "fullName": "Họ tên không được để trống" } }
```

| Status | When |
|---|---|
| `400` | Bean Validation on a `@RequestBody`, type mismatch, and the business exceptions `ExceedLimitRequestException`, `FileUploadException`, `ApiException`, `AIHandlerException`, `FeedBackIligalException`, `ModerateException`, `CreatorIdNotFoundInRequestException` |
| `401` | Missing/invalid bearer token (empty body, `WWW-Authenticate` header), or `UnauthorizedException` |
| `403` | Valid token without the `ADMIN` role, on `/api/admin/**` |
| `404` | `CourseNotFoundException`, `ChapterNotFoundException`, `ModuleNotFoundException`, `ModuleContentNotFoundException`, `CreatorNotFoundException` |
| `409` | `EnrollmentExistException`, `CreatorAlreadyExistsException`, `WishlistExistException` |
| `500` | Everything else, including several cases that *should* be 4xx — see below |

---

## Behaviour worth knowing

Each ⚠️ above, grouped. These are all documented in the Swagger annotations too.

### Returns a status you would not expect

| Endpoint | Behaviour |
|---|---|
| `POST /api/customer/upload_profile`<br>`POST /api/creator/course/create` | `@Valid` on `@ModelAttribute` raises `BindException`, which `GlobalExceptionHandler` does not map, so field errors are **`500`, not `400`** |
| `GET /api/course/{id}` | An unknown id throws a plain `RuntimeException` → **`500`, not `404`** |
| `GET /api/customer/account_infor` | Path is `permitAll` but the handler dereferences the JWT, so an anonymous call is **`500`, not `401`** |
| all 6 `/api/voice/**` | `ResponseStatusException` is caught by the advice's catch-all `Exception` handler before Spring's status resolver, so intended `401`/`404` become **`500`** |
| `POST /api/customer/dictionary/import` | Empty file, >5 MB, unreadable JSON and no-entries all raise `IllegalArgumentException` → **`500`, not `400`** |
| `POST /api/customer/report` | Handler returns `null` → **`200` with an empty body, not `201`** |
| `GET /api/admin/creators/{creatorId}` and the four action endpoints | Creator lookups use `orElse(null)`, so an unknown `creatorId` is an NPE → **`500`, not `404`** |
| `POST /api/quiz/create`<br>`POST /api/customer/evaluate/evaluate/{moduleId}` | Every failure is caught and flattened to **`400` with an empty body** — the cause is not reported |
| `POST /api/quiz/join`, `DELETE /api/quiz/{sessionCode}` | Errors come back as `{ "success": false, "message": "…" }`, **not** the standard `ErrorResponse` |

### Missing or broken authorization

| Endpoint | Behaviour |
|---|---|
| `PUT /api/courses/{courseId}/comments/{commentId}` | **No authorization at all** — any authenticated user can edit any comment by id |
| `DELETE /api/courses/{courseId}/comments/{commentId}` | Only the *course creator* may delete; a learner cannot delete their own comment |
| `GET`/`DELETE /api/quiz/{sessionCode}`, `/participants`, `end-question` | **No host check** — any authenticated user who knows a session code can read, delete, or close a question on someone else's quiz |
| `PUT /api/creator/{courseId}/{chapterId}/module/{moduleId}/update` | Compares creator ids with `!=` instead of `equals`, so it **returns `401` for every caller, including the owner** |
| `POST /api/customer/learning/{courseId}/{moduleId}/finish_content` | Unusable by the course creator — the ownership helper returns `null` for them and the service then dereferences it → `500` |

### Data semantics that will bite

| Endpoint | Behaviour |
|---|---|
| `POST`/`PUT /api/customer/dictionary` | Echoes the request body back, so the returned `rwId` is **whatever you sent**, not the generated id. Re-read the list for real ids. `POST` does not reject duplicates |
| `POST /api/creator/{courseId}/{chapterId}/{moduleId}`<br>`POST /api/creator/kahootModuleContent/{kahootId}` | Items sent with an existing `mcId` are **deleted and re-inserted**, so they come back with **new ids**. Path variables are ignored on the first — the target is `moduleId` in the body |
| `GET /api/customer/learning_course_list` | `progress` is `completed / total * 100`; a course with zero content items yields **`NaN`**, which serialises as bare `NaN` — not valid JSON |
| `GET /api/courses/{courseId}/comments` | `createBy` is always the literal `"Anonymous"` — the `Comment` entity stores no author reference |
| `POST /api/enroll/{id}` | `joinKey` is **required on every request** even for public courses, where its value is then ignored. Pass an empty string |
| `POST /api/customer/feedback/{courseId}` | `rate` is **not range-checked** — any integer is accepted and averaged into the course rating |
| `GET /api/inspiration-videos/today` | Not filtered by date. `date` is just today's server date; the list is the whole R2 prefix. Presigned URLs expire |
| `GET /api/tts` | Streams, so `200` and headers are committed before the upstream read. An upstream failure gives **`200` with a truncated or empty body** — check you got bytes |
| `POST /api/customer/evaluate/evaluateWriting`, `magic-diary`, `magic-diary/question` | An AI failure is **never** an HTTP error — you get `200` with a neutral fallback (scores `5.0`, or `status: "ERROR"`) |
| `POST /api/admin/creators/{creatorId}/warn` | Three warnings within 90 days **auto-suspends** the creator. The response does not say whether that happened |
| `POST /api/admin/creators/{creatorId}/ban` | Bans **every course the creator owns** too |
| `POST /api/admin/creators/{creatorId}/unban` | Restores status to `SUCCESS` **regardless of what it was before the ban** — a `PENDING` creator comes back approved. Unbans all their courses |
| `GET /api/creator/course/{id}/setCourseStatus` | A state **toggle** mapped as `GET` — neither safe nor idempotent. There is no way to set a specific state |
| `PUT .../chapter/{chapterID}/update` | `courseId` in the path is unused. A missing chapter reports `MODULE_NOT_FOUND`, not `CHAPTER_NOT_FOUND` |
| `POST /api/creator/AI/generateFeeback` | A 5/day limiter exists in the class but is **never called** — the endpoint is unlimited |
| `POST /api/creator/upade_certificate` | Path is misspelled (`upade`). Appends rather than replaces; empty files are silently skipped |

### Naming oddities worth not tripping over

- `POST /api/customer/evaluate/evaluate/{moduleId}` — `evaluate` really is doubled (class base + method path).
- `GET /api/creator/course/retrieve_CommercialCourese` — `Courese` is misspelled in the route.
- Comments live under `/api/courses/...` (plural) while the public catalogue is `/api/course/...` (singular).
