# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Liteware는 Spring Boot 3.2.0 기반 그룹웨어 시스템으로, 전자결재, 게시판, 조직관리 등의 기능을 제공합니다. TDD 방식으로 개발되었으며 Docker로 배포 가능합니다.

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
./gradlew test --tests "com.liteware.service.AuthServiceTest"

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
- **Entities**: `ApprovalDocument`, `ApprovalLine`, `ApprovalAttachment`
- **Document Types**: `LeaveRequest`, `OvertimeRequest`, `ExpenseRequest`
- **Workflow**: 순차적 결재선 처리, 상태 관리 (DRAFT → PENDING → APPROVED/REJECTED)
- **Service**: `ApprovalService`, `ApprovalWorkflowService`

#### Board System (게시판)
- **Entities**: `Board`, `Post`, `Comment`, `PostAttachment`
- **Features**: 다중 게시판, 댓글, 파일 첨부
- **Service**: `BoardService`, `CommentService`

#### Authentication & Security
- **JWT 기반 인증**: `JwtTokenProvider`, `JwtAuthenticationFilter`
- **Spring Security 설정**: `SecurityConfig`
- **CustomUserDetails**: 사용자 정보 및 권한 관리

#### Organization (조직관리)
- **Entities**: `Department` (계층구조), `Position`, `User`
- **Relationships**: User-Department-Position 매핑

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

#### Unit Testing
- 모든 Service 클래스에 대한 단위 테스트
- Mockito를 사용한 의존성 모킹
- 비즈니스 로직 검증 중심

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
- Credentials 허용: true

### API Documentation
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- API Docs: `http://localhost:8080/api-docs`

### Development Environment Setup Requirements
1. Java 17+
2. MySQL 8.0 (프로덕션) 또는 H2 (개발)
3. Gradle 8.5

### Common Troubleshooting

#### Test Failures
- H2 데이터베이스 설정 확인 (`application-test.yml`)
- Mock 객체 주입 확인
- 트랜잭션 롤백 설정 확인