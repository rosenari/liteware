# Liteware - Product Requirements Document (PRD)

## 1. 제품 개요

### 1.1 프로젝트 정보
- **프로젝트명**: Liteware
- **버전**: 1.0.0
- **작성일**: 2025-01-05
- **목적**: 중소기업을 위한 경량화된 통합 그룹웨어 시스템
- **대상 사용자**: 50-500명 규모 기업의 임직원

### 1.2 핵심 가치
- **효율성**: 업무 프로세스 자동화 및 간소화
- **통합성**: 조직관리, 전자결재, 커뮤니케이션 통합
- **접근성**: 웹 기반 어디서나 접속 가능
- **확장성**: 기업 성장에 따른 기능 확장 가능

## 2. 기술 스택

### 2.1 Backend
| 구분 | 기술 | 버전 | 용도 |
|------|------|------|------|
| Framework | Spring Boot | 3.x | 웹 애플리케이션 프레임워크 |
| Language | Java | 17+ | 개발 언어 |
| Build Tool | Gradle | 8.x | 빌드 및 의존성 관리 |
| Database | PostgreSQL | 15.x | 메인 데이터베이스 |
| ORM | JPA/Hibernate | - | 객체-관계 매핑 |
| Security | Spring Security | - | 인증/인가 |
| Token | JWT | - | 토큰 기반 인증 |
| Cache | Redis | 7.x | 캐싱 및 세션 관리 |
| Message Queue | RabbitMQ | 3.x | 비동기 메시지 처리 |

### 2.2 Frontend
| 구분 | 기술 | 버전 | 용도 |
|------|------|------|------|
| Template Engine | Thymeleaf | 3.x | 서버 사이드 렌더링 |
| CSS Framework | Bootstrap | 5.x | UI 프레임워크 |
| Admin Template | AdminLTE | 3.x | 관리자 템플릿 |
| JavaScript | Alpine.js | 3.x | 경량 반응형 프레임워크 |
| Chart | Chart.js | 4.x | 데이터 시각화 |
| Editor | TinyMCE | 6.x | WYSIWYG 에디터 |

### 2.3 DevOps & Integration
| 구분 | 기술 | 용도 |
|------|------|------|
| VCS | Git | 버전 관리 |
| CI/CD | Jenkins/GitHub Actions | 자동 빌드/배포 |
| Container | Docker | 컨테이너화 |
| Monitoring | Prometheus + Grafana | 모니터링 |
| API Doc | Swagger/OpenAPI | API 문서화 |
| External | Slack API | 알림 연동 |
| Storage | AWS S3 / Local | 파일 저장소 |

## 3. 시스템 아키텍처

### 3.1 MVC 아키텍처
```
┌─────────────────────────────────────────────────────────┐
│                     Presentation Layer                   │
│                  (Controller + View)                     │
│  - Spring MVC Controllers                               │
│  - Thymeleaf Templates                                  │
│  - REST API Controllers                                 │
├─────────────────────────────────────────────────────────┤
│                     Business Layer                       │
│                      (Service)                          │
│  - Business Logic                                       │
│  - Transaction Management                               │
│  - Validation                                           │
├─────────────────────────────────────────────────────────┤
│                    Persistence Layer                     │
│                 (Repository + Model)                     │
│  - JPA Entities                                         │
│  - Repository Interfaces                                │
│  - Database Access                                      │
└─────────────────────────────────────────────────────────┘
```

### 3.2 패키지 구조
```
com.liteware/
├── controller/           # Controller Layer (MVC - C)
│   ├── auth/            # 인증 관련
│   ├── user/            # 사용자/조직 관리
│   ├── approval/        # 전자결재
│   ├── board/           # 게시판
│   └── api/v1/          # REST API
├── model/               # Model Layer (MVC - M)
│   ├── entity/          # JPA 엔티티
│   ├── dto/             # 데이터 전송 객체
│   ├── vo/              # 값 객체
│   └── enums/           # 열거형
├── service/             # Service Layer
│   ├── auth/            # 인증 서비스
│   ├── user/            # 사용자 서비스
│   ├── approval/        # 결재 서비스
│   └── board/           # 게시판 서비스
├── repository/          # Repository Layer
│   ├── user/            # 사용자 리포지토리
│   ├── approval/        # 결재 리포지토리
│   └── board/           # 게시판 리포지토리
├── config/              # 설정 클래스
├── common/              # 공통 유틸리티
│   ├── exception/       # 예외 처리
│   ├── utils/           # 유틸리티
│   └── interceptor/     # 인터셉터
└── security/            # 보안 관련
```

## 4. 도메인 모델

### 4.1 사용자 및 조직 관리

#### User (사용자)
| 필드명 | 타입 | 설명 | 제약사항 |
|--------|------|------|----------|
| userId | Long | PK | Auto Increment |
| loginId | String | 로그인 ID | Unique, Not Null |
| password | String | 비밀번호 | Encrypted |
| name | String | 이름 | Not Null |
| email | String | 이메일 | Unique |
| phone | String | 전화번호 | |
| profileImage | String | 프로필 이미지 URL | |
| status | Enum | 상태 | ACTIVE/INACTIVE/SUSPENDED |
| departmentId | Long | 부서 ID (FK) | |
| positionId | Long | 직급 ID (FK) | |
| hireDate | Date | 입사일 | |
| createdAt | Timestamp | 생성일시 | |
| updatedAt | Timestamp | 수정일시 | |

#### Department (부서)
| 필드명 | 타입 | 설명 | 제약사항 |
|--------|------|------|----------|
| deptId | Long | PK | Auto Increment |
| deptName | String | 부서명 | Not Null |
| parentDeptId | Long | 상위부서 ID | Self Reference |
| deptLevel | Integer | 부서 레벨 | |
| deptCode | String | 부서 코드 | Unique |
| managerId | Long | 부서장 ID (FK) | |
| sortOrder | Integer | 정렬 순서 | |
| isActive | Boolean | 활성 여부 | Default: true |

#### Position (직급)
| 필드명 | 타입 | 설명 | 제약사항 |
|--------|------|------|----------|
| positionId | Long | PK | Auto Increment |
| positionName | String | 직급명 | Not Null |
| positionLevel | Integer | 직급 레벨 | |
| positionCode | String | 직급 코드 | Unique |
| sortOrder | Integer | 정렬 순서 | |

#### Role (권한)
| 필드명 | 타입 | 설명 | 제약사항 |
|--------|------|------|----------|
| roleId | Long | PK | Auto Increment |
| roleName | String | 권한명 | Unique |
| roleCode | String | 권한 코드 | Unique |
| description | String | 설명 | |
| permissions | JSON | 권한 상세 | |

### 4.2 전자결재

#### ApprovalDocument (결재 문서)
| 필드명 | 타입 | 설명 | 제약사항 |
|--------|------|------|----------|
| docId | Long | PK | Auto Increment |
| docNumber | String | 문서 번호 | Unique |
| docType | Enum | 문서 타입 | Not Null |
| title | String | 제목 | Not Null |
| content | Text | 내용 | |
| formData | JSON | 양식별 데이터 | |
| status | Enum | 상태 | DRAFT/PENDING/APPROVED/REJECTED |
| drafterId | Long | 기안자 ID (FK) | Not Null |
| currentApproverId | Long | 현재 결재자 ID | |
| draftedAt | Timestamp | 기안일시 | |
| completedAt | Timestamp | 완료일시 | |
| urgency | Enum | 긴급도 | NORMAL/URGENT |

#### ApprovalLine (결재선)
| 필드명 | 타입 | 설명 | 제약사항 |
|--------|------|------|----------|
| lineId | Long | PK | Auto Increment |
| docId | Long | 문서 ID (FK) | Not Null |
| approverId | Long | 결재자 ID (FK) | Not Null |
| approvalType | Enum | 결재 타입 | APPROVAL/AGREEMENT/REFERENCE |
| orderSeq | Integer | 순서 | Not Null |
| status | Enum | 상태 | PENDING/APPROVED/REJECTED |
| comment | Text | 의견 | |
| approvedAt | Timestamp | 결재일시 | |
| isOptional | Boolean | 선택적 결재 | Default: false |

#### LeaveRequest (휴가 신청)
| 필드명 | 타입 | 설명 | 제약사항 |
|--------|------|------|----------|
| requestId | Long | PK | Auto Increment |
| docId | Long | 문서 ID (FK) | Not Null |
| leaveType | Enum | 휴가 유형 | 연차/반차/병가/경조사 |
| startDate | Date | 시작일 | Not Null |
| startTime | Time | 시작시간 | |
| endDate | Date | 종료일 | Not Null |
| endTime | Time | 종료시간 | |
| totalDays | Double | 총 일수 | Calculated |
| totalHours | Double | 총 시간 | Calculated |
| reason | Text | 사유 | Not Null |
| emergencyContact | String | 비상연락처 | |
| handoverPerson | Long | 업무인수자 ID | |

#### OvertimeRequest (연장/휴일 근무)
| 필드명 | 타입 | 설명 | 제약사항 |
|--------|------|------|----------|
| requestId | Long | PK | Auto Increment |
| docId | Long | 문서 ID (FK) | Not Null |
| overtimeType | Enum | 근무 유형 | 연장/휴일/야간 |
| workDate | Date | 근무일 | Not Null |
| startTime | Time | 시작시간 | Not Null |
| endTime | Time | 종료시간 | Not Null |
| totalHours | Double | 총 시간 | Calculated |
| workContent | Text | 업무 내용 | Not Null |
| mealProvided | Boolean | 식사 제공 | Default: false |
| transportProvided | Boolean | 교통비 지원 | Default: false |

#### ExpenseRequest (비용 청구)
| 필드명 | 타입 | 설명 | 제약사항 |
|--------|------|------|----------|
| requestId | Long | PK | Auto Increment |
| docId | Long | 문서 ID (FK) | Not Null |
| expenseType | Enum | 비용 유형 | Not Null |
| totalAmount | Decimal | 총 금액 | Not Null |
| expenseDate | Date | 지출일 | Not Null |
| paymentMethod | Enum | 지불 방법 | 법인카드/개인카드/현금 |
| vendorName | String | 거래처명 | |
| businessPurpose | Text | 사용 목적 | Not Null |
| receipts | JSON | 영수증 정보 | |

### 4.3 게시판

#### Board (게시판)
| 필드명 | 타입 | 설명 | 제약사항 |
|--------|------|------|----------|
| boardId | Long | PK | Auto Increment |
| boardName | String | 게시판명 | Not Null |
| boardType | Enum | 게시판 유형 | NOTICE/GENERAL/DEPT |
| description | String | 설명 | |
| sortOrder | Integer | 정렬 순서 | |
| permissions | JSON | 권한 설정 | |
| isActive | Boolean | 활성 여부 | Default: true |

#### Post (게시글)
| 필드명 | 타입 | 설명 | 제약사항 |
|--------|------|------|----------|
| postId | Long | PK | Auto Increment |
| boardId | Long | 게시판 ID (FK) | Not Null |
| title | String | 제목 | Not Null |
| content | Text | 내용 | Not Null |
| writerId | Long | 작성자 ID (FK) | Not Null |
| viewCount | Integer | 조회수 | Default: 0 |
| isPinned | Boolean | 상단 고정 | Default: false |
| isNotice | Boolean | 공지 여부 | Default: false |
| startDate | Date | 게시 시작일 | |
| endDate | Date | 게시 종료일 | |
| createdAt | Timestamp | 작성일시 | |
| updatedAt | Timestamp | 수정일시 | |

#### Comment (댓글)
| 필드명 | 타입 | 설명 | 제약사항 |
|--------|------|------|----------|
| commentId | Long | PK | Auto Increment |
| postId | Long | 게시글 ID (FK) | Not Null |
| content | Text | 내용 | Not Null |
| writerId | Long | 작성자 ID (FK) | Not Null |
| parentCommentId | Long | 부모 댓글 ID | Self Reference |
| createdAt | Timestamp | 작성일시 | |
| updatedAt | Timestamp | 수정일시 | |

#### Attachment (첨부파일)
| 필드명 | 타입 | 설명 | 제약사항 |
|--------|------|------|----------|
| attachmentId | Long | PK | Auto Increment |
| referenceType | Enum | 참조 타입 | POST/APPROVAL/COMMENT |
| referenceId | Long | 참조 ID | Not Null |
| fileName | String | 파일명 | Not Null |
| originalFileName | String | 원본 파일명 | Not Null |
| filePath | String | 파일 경로 | Not Null |
| fileSize | Long | 파일 크기 | |
| mimeType | String | MIME 타입 | |
| uploadedBy | Long | 업로더 ID (FK) | Not Null |
| uploadedAt | Timestamp | 업로드일시 | |

## 5. 주요 기능 명세

### 5.1 인증 및 권한 관리

#### 5.1.1 로그인/로그아웃
- **로그인**
  - ID/Password 인증
  - JWT 토큰 발급
  - Remember Me 기능
  - 로그인 실패 횟수 제한 (5회)
  - 비밀번호 찾기 (이메일 인증)

- **세션 관리**
  - JWT 토큰 기반 인증
  - Refresh Token 구현
  - 세션 타임아웃 (30분)
  - 동시 로그인 제한 옵션

#### 5.1.2 권한 관리
- **Role 기반 접근 제어 (RBAC)**
  - ADMIN: 시스템 관리자
  - MANAGER: 부서 관리자
  - USER: 일반 사용자
  - VIEWER: 읽기 전용

- **권한 체크 포인트**
  - URL 레벨 권한 체크
  - 메소드 레벨 권한 체크 (@PreAuthorize)
  - 데이터 레벨 권한 체크

### 5.2 조직도 및 사용자 관리

#### 5.2.1 조직도
- **조직도 조회**
  - 트리 구조 표시
  - 부서별 인원 조회
  - 검색 기능 (이름, 부서, 직급)
  - 프로필 상세 보기

- **부서 관리** (관리자)
  - 부서 CRUD
  - 부서 이동/병합
  - 부서장 지정
  - 부서 코드 관리

#### 5.2.2 사용자 관리
- **사용자 정보 관리**
  - 프로필 수정
  - 비밀번호 변경
  - 프로필 사진 업로드

- **관리자 기능**
  - 사용자 등록/수정/삭제
  - 부서/직급 변경
  - 권한 부여/회수
  - 비밀번호 초기화
  - 계정 활성화/비활성화

### 5.3 전자결재

#### 5.3.1 기안 작성
- **문서 작성**
  - 템플릿 선택
  - 양식별 필드 입력
  - 첨부파일 업로드 (최대 50MB)
  - 임시저장 기능

- **결재선 설정**
  - 조직도 기반 결재자 선택
  - 결재선 템플릿 저장/불러오기
  - 병렬/순차 결재 설정
  - 참조자 지정

#### 5.3.2 결재 처리
- **결재 액션**
  - 승인: 다음 결재자에게 전달
  - 반려: 기안자에게 반송
  - 보류: 추가 검토 필요
  - 위임: 다른 사람에게 결재 위임

- **결재함**
  - 미결함: 결재 대기 문서
  - 진행함: 결재 진행 중 문서
  - 완료함: 결재 완료 문서
  - 임시저장함: 임시 저장 문서
  - 참조함: 참조/열람 문서

#### 5.3.3 결재 규칙
- **자동 결재선 규칙**
  - 문서 타입별 기본 결재선
  - 금액 기준 자동 결재선
    - 10만원 미만: 팀장 전결
    - 100만원 미만: 팀장 → 부서장
    - 500만원 미만: 팀장 → 부서장 → 본부장
    - 500만원 이상: 팀장 → 부서장 → 본부장 → 대표
  
- **휴가 결재 규칙**
  - 반차: 팀장 전결
  - 1-2일: 팀장 → 부서장
  - 3-5일: 팀장 → 부서장 → 인사팀
  - 5일 초과: 팀장 → 부서장 → 인사팀 → 대표

#### 5.3.4 결재 양식
1. **근태 관리**
   - 휴가 신청 (연차/반차/병가/경조사)
   - 외출/조퇴 신청
   - 출장 신청

2. **근무 관리**
   - 연장근무 신청
   - 휴일근무 신청
   - 근무시간 변경 신청
   - 재택근무 신청

3. **비용 관리**
   - 경비 청구
   - 법인카드 사용 보고
   - 출장비 정산

4. **구매 관리**
   - 물품 구매 요청
   - 서비스 구매 요청
   - 자산 구매 요청

### 5.4 게시판

#### 5.4.1 공지사항
- **작성/편집**
  - WYSIWYG 에디터
  - 다중 파일 첨부
  - 게시 기간 설정
  - 상단 고정 설정
  - 부서별 공지 설정

- **조회**
  - 목록 조회 (페이징)
  - 상세 조회
  - 조회수 증가
  - 읽음 표시

#### 5.4.2 일반 게시판
- **게시글 관리**
  - 작성/수정/삭제
  - 댓글 작성/수정/삭제
  - 좋아요 기능
  - 북마크 기능

- **검색/필터**
  - 제목/내용/작성자 검색
  - 기간별 필터
  - 카테고리별 필터
  - 정렬 (최신순/조회순/댓글순)

### 5.5 알림 시스템

#### 5.5.1 알림 유형
- **결재 알림**
  - 결재 요청 수신
  - 결재 승인/반려
  - 문서 완료

- **게시판 알림**
  - 새 공지사항
  - 댓글 알림
  - 답글 알림

- **시스템 알림**
  - 비밀번호 변경 필요
  - 시스템 점검 안내

#### 5.5.2 알림 채널
- **웹 알림**
  - 실시간 푸시 (SSE/WebSocket)
  - 알림 센터
  - 읽음/안읽음 관리

- **이메일 알림**
  - 중요 결재 알림
  - 일일 요약 메일
  - 알림 설정 관리

- **Slack 연동**
  - Webhook URL 설정
  - 채널별 알림 설정
  - 멘션 기능

## 6. API 명세

### 6.1 RESTful API 구조
```
BASE_URL: https://api.liteware.com/api/v1

인증: Bearer Token (JWT)
Content-Type: application/json
```

### 6.2 주요 API 엔드포인트

#### 인증 API
```
POST   /auth/login              - 로그인
POST   /auth/logout             - 로그아웃
POST   /auth/refresh            - 토큰 갱신
POST   /auth/forgot-password    - 비밀번호 찾기
POST   /auth/reset-password     - 비밀번호 재설정
```

#### 사용자 API
```
GET    /users                   - 사용자 목록 조회
GET    /users/{id}             - 사용자 상세 조회
POST   /users                   - 사용자 생성
PUT    /users/{id}             - 사용자 수정
DELETE /users/{id}             - 사용자 삭제
PATCH  /users/{id}/password    - 비밀번호 변경
PATCH  /users/{id}/status      - 상태 변경
```

#### 조직 API
```
GET    /departments             - 부서 목록 조회
GET    /departments/tree        - 부서 트리 조회
GET    /departments/{id}       - 부서 상세 조회
POST   /departments             - 부서 생성
PUT    /departments/{id}       - 부서 수정
DELETE /departments/{id}       - 부서 삭제

GET    /positions               - 직급 목록 조회
POST   /positions               - 직급 생성
PUT    /positions/{id}         - 직급 수정
DELETE /positions/{id}         - 직급 삭제
```

#### 전자결재 API
```
# 문서 관리
GET    /approvals               - 결재 문서 목록
GET    /approvals/{id}         - 결재 문서 상세
POST   /approvals               - 결재 문서 작성
PUT    /approvals/{id}         - 결재 문서 수정
DELETE /approvals/{id}         - 결재 문서 삭제
POST   /approvals/{id}/submit  - 결재 상신
POST   /approvals/{id}/approve - 결재 승인
POST   /approvals/{id}/reject  - 결재 반려
POST   /approvals/{id}/cancel  - 결재 취소

# 결재선 관리
GET    /approvals/{id}/lines   - 결재선 조회
POST   /approvals/{id}/lines   - 결재선 설정
PUT    /approvals/{id}/lines   - 결재선 수정

# 결재 양식
GET    /approval-templates      - 양식 목록
GET    /approval-templates/{id} - 양식 상세
POST   /approval-templates      - 양식 생성
PUT    /approval-templates/{id} - 양식 수정

# 특수 문서
POST   /approvals/leave-request     - 휴가 신청
POST   /approvals/overtime-request  - 연장근무 신청
POST   /approvals/expense-request   - 비용 청구
GET    /users/{id}/leave-balance    - 휴가 잔여일수 조회
```

#### 게시판 API
```
# 게시판 관리
GET    /boards                  - 게시판 목록
GET    /boards/{id}            - 게시판 상세
POST   /boards                  - 게시판 생성
PUT    /boards/{id}            - 게시판 수정
DELETE /boards/{id}            - 게시판 삭제

# 게시글 관리
GET    /posts                   - 게시글 목록
GET    /posts/{id}             - 게시글 상세
POST   /posts                   - 게시글 작성
PUT    /posts/{id}             - 게시글 수정
DELETE /posts/{id}             - 게시글 삭제
PATCH  /posts/{id}/pin         - 상단 고정
POST   /posts/{id}/like        - 좋아요

# 댓글 관리
GET    /posts/{id}/comments    - 댓글 목록
POST   /posts/{id}/comments    - 댓글 작성
PUT    /comments/{id}          - 댓글 수정
DELETE /comments/{id}          - 댓글 삭제
```

#### 파일 API
```
POST   /files/upload            - 파일 업로드
GET    /files/{id}             - 파일 다운로드
DELETE /files/{id}             - 파일 삭제
GET    /files/{id}/preview     - 파일 미리보기
```

#### 알림 API
```
GET    /notifications           - 알림 목록
GET    /notifications/unread   - 읽지 않은 알림
PATCH  /notifications/{id}/read - 알림 읽음 처리
DELETE /notifications/{id}     - 알림 삭제
POST   /notifications/settings - 알림 설정
```

## 7. 화면 설계

### 7.1 레이아웃 구조
```
┌─────────────────────────────────────────────────────┐
│  Logo    [검색]          [알림] [메시지] [프로필▼]    │  Header
├───────┬─────────────────────────────────────────────┤
│       │                                             │
│  홈   │  ┌─────────────────────────────────────┐   │
│       │  │                                     │   │
│ 전자  │  │                                     │   │
│ 결재  │  │         Main Content Area          │   │
│       │  │                                     │   │
│ 게시  │  │                                     │   │
│ 판    │  │                                     │   │
│       │  │                                     │   │
│ 조직  │  │                                     │   │
│ 도    │  └─────────────────────────────────────┘   │
│       │                                             │
│ 설정  │                                             │
└───────┴─────────────────────────────────────────────┘
  Sidebar                  Content Area
```

### 7.2 주요 화면

#### 7.2.1 대시보드
- **위젯 구성**
  - 미결재 문서 (상위 5건)
  - 최근 공지사항 (상위 5건)
  - 오늘의 일정
  - 빠른 메뉴 (자주 사용하는 기능)
  - 휴가 현황
  - 부서 동향

#### 7.2.2 전자결재
- **기안 작성**
  - Step 1: 양식 선택
  - Step 2: 내용 작성
  - Step 3: 결재선 설정
  - Step 4: 최종 확인

- **결재 상세**
  ```
  ┌─────────────────────────────┐
  │ 문서 제목                    │
  ├─────────────────────────────┤
  │ 기안자: 홍길동 | 기안일: ... │
  ├─────────────────────────────┤
  │                             │
  │      문서 내용 영역          │
  │                             │
  ├─────────────────────────────┤
  │ 첨부파일: file1.pdf, ...    │
  ├─────────────────────────────┤
  │ 결재선                      │
  │ [기안] → [검토] → [승인]    │
  ├─────────────────────────────┤
  │ 결재의견:                   │
  │ [                       ]   │
  │ [승인] [반려] [보류]        │
  └─────────────────────────────┘
  ```

#### 7.2.3 조직도
```
┌──────────────┬─────────────────────┐
│  조직 트리   │   상세 정보          │
├──────────────┼─────────────────────┤
│ ▼ 회사       │  [프로필 사진]      │
│   ▼ 경영진   │  이름: 홍길동       │
│   ▼ 개발부   │  부서: 개발부       │
│     - 홍길동  │  직급: 과장         │
│     - 김철수  │  이메일: ...        │
│   ▷ 영업부   │  전화: ...          │
│   ▷ 인사부   │  입사일: ...        │
└──────────────┴─────────────────────┘
```

#### 7.2.4 게시판
- **목록 화면**
  ```
  [전체] [공지사항] [자유게시판] [부서게시판]
  
  [제목/내용/작성자 ▼] [검색어    ] [검색]
  
  ┌─────┬──────────────────┬────────┬────────┬──────┐
  │ No  │ 제목              │ 작성자  │ 작성일  │ 조회 │
  ├─────┼──────────────────┼────────┼────────┼──────┤
  │ 📌  │ [공지] 시스템 점검 │ 관리자  │ 12/01  │ 152  │
  │ 15  │ 회의록 공유       │ 김과장  │ 12/01  │  45  │
  │ 14  │ 문의사항입니다    │ 이대리  │ 11/30  │  23  │
  └─────┴──────────────────┴────────┴────────┴──────┘
  
  [이전] 1 2 3 4 5 [다음]         [글쓰기]
  ```

## 8. 보안 요구사항

### 8.1 인증/인가
- JWT 토큰 기반 인증
- Refresh Token 구현 (7일 유효)
- Access Token (1시간 유효)
- Role 기반 권한 관리 (RBAC)
- Method-level Security (@PreAuthorize)

### 8.2 데이터 보안
- 비밀번호 암호화 (BCrypt)
- 민감정보 암호화 (AES-256)
- SQL Injection 방지 (Prepared Statement)
- XSS 방어 (Input Validation, Output Encoding)
- CSRF 토큰 적용

### 8.3 통신 보안
- HTTPS 적용 (TLS 1.2+)
- API Rate Limiting
- CORS 설정
- Request/Response 로깅

### 8.4 파일 보안
- 파일 업로드 검증
  - 파일 크기 제한 (50MB)
  - 파일 타입 검증 (White List)
  - 파일명 sanitization
- 안티바이러스 스캔
- 파일 다운로드 권한 체크

## 9. 성능 요구사항

### 9.1 응답 시간
- 페이지 로딩: 2초 이내
- API 응답: 1초 이내
- 파일 업로드: 크기별 차등
  - 1MB 이하: 2초
  - 10MB 이하: 10초
  - 50MB 이하: 30초

### 9.2 동시 접속
- 최소: 100명
- 권장: 500명
- 최대: 1000명

### 9.3 가용성
- 목표 가용성: 99.5%
- 계획된 다운타임: 월 1회 (새벽 시간대)

### 9.4 데이터 용량
- 사용자당 저장공간: 5GB
- 데이터 보관 기간
  - 결재 문서: 5년
  - 게시글: 3년
  - 로그: 1년

## 10. 개발 일정

### Phase 1: 기반 구축 (2주)
- [ ] 프로젝트 초기 설정
- [ ] 데이터베이스 설계 및 구축
- [ ] Spring Security 설정
- [ ] 기본 MVC 구조 구현

### Phase 2: 인증 및 사용자 관리 (2주)
- [ ] 로그인/로그아웃 구현
- [ ] JWT 토큰 인증
- [ ] 사용자 CRUD
- [ ] 부서/직급 관리
- [ ] 권한 관리

### Phase 3: 전자결재 핵심 기능 (3주)
- [ ] 결재 문서 작성
- [ ] 결재선 설정
- [ ] 결재 처리 로직
- [ ] 결재함 구현
- [ ] 결재 양식 관리

### Phase 4: 전자결재 고급 기능 (2주)
- [ ] 휴가 신청 프로세스
- [ ] 연장근무 신청
- [ ] 비용 청구
- [ ] 결재 규칙 엔진
- [ ] 위임 설정

### Phase 5: 게시판 시스템 (2주)
- [ ] 게시판 CRUD
- [ ] 게시글 작성/조회
- [ ] 댓글 기능
- [ ] 파일 첨부
- [ ] 검색 기능

### Phase 6: 알림 시스템 (1주)
- [ ] 웹 알림 (SSE/WebSocket)
- [ ] 이메일 발송
- [ ] Slack 연동
- [ ] 알림 설정

### Phase 7: UI/UX 개발 (2주)
- [ ] Thymeleaf 레이아웃
- [ ] AdminLTE 적용
- [ ] 반응형 디자인
- [ ] 차트/대시보드
- [ ] UX 개선

### Phase 8: 테스트 및 최적화 (2주)
- [ ] 단위 테스트
- [ ] 통합 테스트
- [ ] 성능 테스트
- [ ] 보안 점검
- [ ] 버그 수정

### Phase 9: 배포 준비 (1주)
- [ ] 배포 환경 구성
- [ ] CI/CD 파이프라인
- [ ] 모니터링 설정
- [ ] 문서화
- [ ] 사용자 매뉴얼

**총 개발 기간: 약 17주 (4개월)**

## 11. 기술적 고려사항

### 11.1 확장성
- Microservice 전환 가능한 구조
- 모듈별 독립적 배포 가능
- 수평적 확장 지원 (Scale-out)
- 캐싱 전략 (Redis)

### 11.2 유지보수성
- Clean Code 원칙 준수
- 단위 테스트 Coverage 80% 이상
- API 버전 관리
- 로깅 및 모니터링

### 11.3 호환성
- 브라우저: Chrome, Firefox, Safari, Edge 최신 버전
- 모바일: 반응형 웹 지원
- API: RESTful, 추후 GraphQL 고려

### 11.4 국제화
- 다국어 지원 준비 (i18n)
- 타임존 처리
- 통화/날짜 형식

## 12. 리스크 관리

### 12.1 기술적 리스크
| 리스크 | 확률 | 영향 | 대응방안 |
|--------|------|------|----------|
| 성능 저하 | 중 | 높음 | 캐싱, 인덱싱, 쿼리 최적화 |
| 보안 취약점 | 낮음 | 매우 높음 | 정기 보안 점검, 패치 적용 |
| 외부 API 장애 | 중 | 중간 | Fallback 메커니즘, 재시도 로직 |
| 데이터 손실 | 낮음 | 매우 높음 | 정기 백업, 트랜잭션 관리 |

### 12.2 프로젝트 리스크
| 리스크 | 확률 | 영향 | 대응방안 |
|--------|------|------|----------|
| 일정 지연 | 중 | 높음 | 단계별 마일스톤, 버퍼 시간 확보 |
| 요구사항 변경 | 높음 | 중간 | Agile 방법론, 주기적 피드백 |
| 기술 역량 부족 | 낮음 | 중간 | 교육, 외부 컨설팅 |

## 13. 부록

### 13.1 용어집
| 용어 | 설명 |
|------|------|
| 기안 | 결재를 받기 위해 문서를 작성하는 행위 |
| 결재 | 제출된 문서를 검토하고 승인하는 행위 |
| 전결 | 위임받은 권한으로 최종 결정하는 행위 |
| 합의 | 결재 과정에서 관련 부서의 동의를 구하는 행위 |
| 후결 | 사후에 결재를 받는 행위 |

### 13.2 참고 자료
- Spring Boot Documentation: https://spring.io/projects/spring-boot
- Thymeleaf Documentation: https://www.thymeleaf.org/
- AdminLTE: https://adminlte.io/
- JWT: https://jwt.io/
- RESTful API Design: https://restfulapi.net/

### 13.3 변경 이력
| 버전 | 날짜 | 변경 내용 | 작성자 |
|------|------|----------|--------|
| 1.0.0 | 2025-01-05 | 초안 작성 | System |

---
*이 문서는 Liteware 프로젝트의 제품 요구사항 명세서입니다.*