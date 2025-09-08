# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Principles

### 1. 최소 수정의 원칙 (Principle of Minimal Modification)
- 기존 코드를 수정할 때는 필요한 최소한의 변경만 수행
- 동작하는 코드는 리팩토링이 명확히 필요한 경우가 아니면 유지
- 새로운 기능 추가 시 기존 구조를 최대한 활용

### 2. 간결함의 원칙 (Principle of Simplicity)
- 복잡한 로직보다 단순하고 읽기 쉬운 코드 선호
- 과도한 추상화 지양
- 명확한 네이밍과 직관적인 구조 유지

### 3. 유지보수 우선 설계 (Maintainability First)
- 여러 구현 방법 중 가장 유지보수가 용이한 방식 선택
- 코드 가독성을 성능보다 우선시 (성능이 critical하지 않은 경우)
- 일관된 코딩 스타일과 패턴 유지

### 4. JPA Fetch 전략
- N+1 문제를 사전에 방지하기 위해 연관관계는 가능한 EAGER로 설정
- 순환 참조가 없고 데이터 크기가 크지 않은 경우 EAGER 적용
- 현재 적용 예시:
  - ApprovalDocument의 approvalLines, attachments, references: EAGER
  - Board, Post의 기본 연관관계: EAGER
- Lazy Loading 예외를 최소화하여 안정적인 서비스 운영

## Project Overview

Liteware는 Spring Boot 3.2.0 기반 그룹웨어 시스템으로, 전자결재, 게시판, 조직관리, 근태관리, 연차관리 등의 기능을 제공합니다. H2 통합 테스트 기반으로 개발되었으며 Docker로 배포 가능합니다.

## Development Commands

### Build & Run
```bash
# 애플리케이션 실행
./gradlew bootRun

# JAR 빌드
./gradlew bootJar

# 클린 빌드
./gradlew clean build
```

### Testing
```bash
# 모든 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "com.liteware.service.approval.ApprovalServiceTest"

# 특정 패키지 테스트 실행
./gradlew test --tests "com.liteware.service.*"

# Integration 테스트 실행 (태그 기반)
./gradlew integrationTest

# 테스트 리포트 확인
open build/reports/tests/test/index.html
```

### Docker
```bash
# Docker Compose로 전체 스택 실행 (MySQL + App)
docker-compose up -d --build

# 로그 확인
docker-compose logs -f app

# 중지 및 볼륨 정리
docker-compose down -v
```

## Architecture Overview

### Layered Architecture
프로젝트는 명확한 계층 분리를 따릅니다:

1. **Controller Layer** (`/controller`, `/controller/api`)
   - Web MVC 컨트롤러와 REST API 엔드포인트
   - Thymeleaf 뷰 반환 또는 JSON 응답

2. **Service Layer** (`/service`)
   - 비즈니스 로직 구현
   - 트랜잭션 관리 (@Transactional)
   - 도메인별 패키지 구성

3. **Repository Layer** (`/repository`)
   - JPA 데이터 접근
   - Spring Data JPA 인터페이스

4. **Model Layer** (`/model`)
   - `/entity`: JPA 엔티티 (BaseEntity 상속)
   - `/dto`: 데이터 전송 객체
   - 엔티티는 도메인별 서브패키지로 구성

### Core Domains

#### Approval System (전자결재)
- **Entities**: `ApprovalDocument`, `ApprovalLine`, `ApprovalAttachment`, `ApprovalReference`
- **Document Types**: `LeaveRequest` (휴가신청), `OvertimeRequest` (연장근무), `ExpenseRequest` (지출결의)
- **Workflow States**: 
  - DRAFT (임시저장) → PENDING (결재대기) → APPROVED (승인) / REJECTED (반려)
  - CANCELLED (취소 가능)
- **Services**: 
  - `ApprovalService`: 문서 CRUD, 첨부파일 관리, 참조자 관리
  - `ApprovalWorkflowService`: 결재 프로세스, 상태 전이, 알림 처리
- **Features**:
  - 순차/병렬 결재선 지원
  - 참조자 기능 (읽음 상태 관리)
  - 파일 첨부 (다중 파일 지원)
  - 긴급 문서 처리

#### Board System (게시판)
- **Entities**: `Board`, `Post`, `Comment`, `PostAttachment`
- **Board Types**: NOTICE (공지사항), FREE (자유게시판), QNA (Q&A), FAQ
- **Features**: 
  - 다중 게시판 지원
  - 계층형 댓글
  - 파일 첨부 (이미지, 문서)
  - 조회수 관리
  - 검색 기능 (제목, 내용, 작성자)
- **Services**: `BoardService`, `CommentService`

#### Authentication & Security
- **JWT 기반 인증**: `JwtTokenProvider`, `JwtAuthenticationFilter`
- **Spring Security 설정**: `SecurityConfig`
- **CustomUserDetails**: 사용자 정보 및 권한 관리

#### Organization (조직관리)
- **Entities**: 
  - `Department`: 계층형 부서 구조 (parentDepartment)
  - `Position`: 직급 관리 (level 기반)
  - `User`: 사용자 정보
  - `Role`: 권한 관리 (ADMIN, USER, MANAGER)
- **Relationships**: 
  - User ↔ Department (N:1)
  - User ↔ Position (N:1)
  - User ↔ Role (N:N)
- **Services**: `DepartmentService`, `PositionService`, `UserService`

#### Attendance & Leave (근태/연차관리)
- **Entities**: 
  - `Attendance`: 출퇴근 기록
  - `AnnualLeave`: 연차 관리
- **Features**:
  - 출퇴근 체크
  - 근무시간 계산
  - 연차 잔여일수 관리
  - 연차 사용 내역
- **Services**: `AttendanceService`, `AnnualLeaveService`

#### Notification (알림)
- **Entity**: `Notification`
- **Types**: 결재 요청, 결재 완료, 댓글 알림, 시스템 공지
- **Features**: 
  - 실시간 알림
  - 읽음 상태 관리
  - 알림 보관 기간 설정
- **Service**: `NotificationService`

### Key Technical Patterns

#### Entity Management
```java
// 모든 엔티티는 BaseEntity 상속
@MappedSuperclass
public abstract class BaseEntity {
    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

#### Service Transaction Pattern
```java
@Service
@Transactional(readOnly = true)
public class XxxService {
    @Transactional
    public void modifyingMethod() { }
}
```

#### Approval Workflow Processing
결재 처리는 `ApprovalWorkflowService`에서 순차적으로 처리:
1. 현재 결재자 확인
2. 결재 상태 업데이트
3. 다음 결재자 활성화 또는 최종 상태 설정

### Database Configuration

#### Development (H2 In-Memory)
- 자동 스키마 생성: `ddl-auto: create-drop`
- H2 Console: `http://localhost:8080/h2-console`

#### Production (MySQL/PostgreSQL)
- Docker Compose로 MySQL 8.0 제공

### Test Strategy

#### Integration Testing with H2
- **모든 Service 테스트를 H2 기반 통합 테스트로 구현**
- `BaseServiceTest` 상속을 통한 공통 테스트 환경 제공
- 실제 데이터베이스 트랜잭션 및 영속성 검증
- `@Transactional` 및 `@Rollback`으로 테스트 격리

#### Test Data Setup
- `BaseServiceTest`에서 공통 테스트 데이터 초기화
  - 부서, 직급, 권한 기본 데이터
  - 테스트용 사용자 생성 헬퍼 메서드
- 각 도메인별 테스트 데이터 구성

#### Test Naming Convention
```java
@Test
@DisplayName("사용자 생성 성공 테스트")
void createUser_Success() { }

@Test
@DisplayName("중복 이메일로 사용자 생성 시 예외 발생")
void createUser_DuplicateEmail_ThrowsException() { }
```

### Important Configurations

#### JWT Configuration
- Secret key: `application.yml`의 `jwt.secret` 설정
- 토큰 만료: 1시간 (3600000ms)
- Refresh 토큰: 7일 (604800000ms)

#### File Upload
- 최대 파일 크기: 50MB
- 업로드 디렉토리: `./uploads` (설정 가능)

#### CORS Settings
- 허용 Origin: `http://localhost:3000`, `http://localhost:8080`
- 허용 메서드: GET, POST, PUT, DELETE, OPTIONS
- Credentials 허용: true

### API Documentation
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- API Docs: `http://localhost:8080/api-docs`

### Key API Endpoints

#### Authentication
- POST `/api/auth/login` - 로그인
- POST `/api/auth/signup` - 회원가입
- POST `/api/auth/refresh` - 토큰 갱신

#### Approval
- GET `/api/approval/documents` - 문서 목록
- POST `/api/approval/documents` - 문서 작성
- PUT `/api/approval/documents/{id}` - 문서 수정
- POST `/api/approval/documents/{id}/submit` - 결재 상신
- POST `/api/approval/documents/{id}/approve` - 승인
- POST `/api/approval/documents/{id}/reject` - 반려

#### Board
- GET `/api/boards` - 게시판 목록
- GET `/api/boards/{boardId}/posts` - 게시글 목록
- POST `/api/boards/{boardId}/posts` - 게시글 작성
- GET `/api/posts/{postId}` - 게시글 상세
- POST `/api/posts/{postId}/comments` - 댓글 작성

### Development Environment Setup Requirements
1. Java 17+
2. MySQL 8.0 (프로덕션) 또는 H2 (개발)
3. Gradle 8.5+
4. Docker & Docker Compose (선택사항)

### Common Troubleshooting

#### Test Failures
- H2 데이터베이스 설정 확인 (`application-test.yml`)
- `BaseServiceTest` 상속 여부 확인
- 트랜잭션 롤백 설정 확인
- 테스트 데이터 초기화 순서 확인

#### Application Startup Issues
- H2 Console 접속: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- 로그 레벨 조정: `application.yml`의 `logging.level`

#### Docker Issues
- MySQL 컨테이너 헬스체크 확인
- 네트워크 설정 확인
- 볼륨 권한 문제 해결