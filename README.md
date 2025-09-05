# Liteware - 그룹웨어 시스템

Liteware는 Spring Boot 기반의 현대적인 그룹웨어 시스템입니다. 전자결재, 게시판, 조직도 관리 등 기업에서 필요한 핵심 협업 기능을 제공합니다.

## 주요 기능

### 1. 인증 및 권한 관리
- JWT 기반 인증 시스템
- Spring Security를 활용한 권한 관리
- 사용자 로그인/로그아웃 관리
- 역할 기반 접근 제어 (RBAC)

### 2. 전자결재 시스템
- **결재 문서 유형**
  - 휴가 신청서
  - 연장근무 신청서
  - 경비 정산서
- **결재 프로세스**
  - 다단계 결재선 설정
  - 결재/반려/보류 처리
  - 결재 진행 상태 추적
  - 첨부파일 지원

### 3. 게시판 시스템
- 다양한 게시판 유형 지원 (공지사항, 자유게시판 등)
- 게시글 작성/수정/삭제
- 댓글 기능
- 파일 첨부
- 조회수 관리

### 4. 조직 관리
- 부서 관리 (계층구조 지원)
- 직급 관리
- 조직도 조회
- 부서별 인원 관리

### 5. 알림 시스템
- 실시간 알림 기능
- 결재 요청/승인 알림
- 게시글 댓글 알림
- 알림 읽음 처리

### 6. 파일 관리
- 파일 업로드/다운로드
- 파일 메타데이터 관리
- 다양한 파일 형식 지원

## 기술 스택

- **Backend**: Spring Boot 3.x, Java 17
- **Database**: MySQL 8.0
- **Cache**: Redis
- **Security**: Spring Security, JWT
- **ORM**: JPA/Hibernate
- **Build Tool**: Gradle
- **Container**: Docker, Docker Compose

## 시작하기

### 사전 요구사항

- Java 17 이상
- MySQL 8.0 이상
- Redis 7 이상
- Docker & Docker Compose (선택사항)

### 로컬 환경에서 실행하기

#### 1. 저장소 클론
```bash
git clone https://github.com/rosenari/liteware.git
cd liteware
```

#### 2. 데이터베이스 설정
MySQL에서 데이터베이스와 사용자를 생성합니다:
```sql
CREATE DATABASE liteware CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'liteware'@'localhost' IDENTIFIED BY 'liteware123!';
GRANT ALL PRIVILEGES ON liteware.* TO 'liteware'@'localhost';
FLUSH PRIVILEGES;
```

#### 3. Redis 실행
```bash
#  Docker 사용 권장
docker run -d -p 6379:6379 redis:7-alpine
```

#### 4. 애플리케이션 설정
`src/main/resources/application.yml` 파일에서 데이터베이스 연결 정보를 확인/수정합니다:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/liteware
    username: liteware
    password: liteware123!
```

#### 5. 애플리케이션 실행
```bash
# Gradle Wrapper 사용
./gradlew bootRun

# 또는 JAR 파일 빌드 후 실행
./gradlew bootJar
java -jar build/libs/liteware-0.0.1-SNAPSHOT.jar
```

애플리케이션이 시작되면 http://localhost:8080 에서 접속할 수 있습니다.

### Docker Compose로 실행하기

Docker Compose를 사용하면 모든 서비스(MySQL, Redis, 애플리케이션)를 한 번에 실행할 수 있습니다.

#### 1. 환경 변수 설정 (선택사항)
`.env` 파일을 생성하여 JWT 시크릿 키를 설정할 수 있습니다:
```bash
JWT_SECRET=your-very-secret-key-at-least-256-bits-long
```

#### 2. Docker Compose 실행
```bash
# 서비스 시작 (빌드 포함)
docker-compose up -d --build

# 로그 확인
docker-compose logs -f

# 특정 서비스 로그만 보기
docker-compose logs -f app
```

#### 3. 서비스 중지
```bash
# 서비스 중지
docker-compose down

# 서비스 중지 및 볼륨 삭제 (데이터 초기화)
docker-compose down -v
```

### Docker 명령어 모음

```bash
# 이미지 빌드
docker build -t liteware:latest .

# 컨테이너 실행 (네트워크 연결 필요)
docker run -d \
  --name liteware-app \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  liteware:latest

# 컨테이너 상태 확인
docker ps

# 컨테이너 로그 확인
docker logs liteware-app -f

# 컨테이너 접속
docker exec -it liteware-app sh

# 컨테이너 중지/시작
docker stop liteware-app
docker start liteware-app

# 컨테이너 및 이미지 삭제
docker rm liteware-app
docker rmi liteware:latest
```

## 테스트 실행

### 단위 테스트 실행
```bash
# 모든 테스트 실행
./gradlew test

# 특정 테스트 클래스만 실행
./gradlew test --tests "com.liteware.service.AuthServiceTest"

# 특정 패키지의 테스트만 실행
./gradlew test --tests "com.liteware.service.*"

# 테스트 리포트 확인
open build/reports/tests/test/index.html  # Mac
xdg-open build/reports/tests/test/index.html  # Linux
```

### 테스트 커버리지 확인
```bash
# JaCoCo를 사용한 커버리지 측정 (build.gradle에 플러그인 추가 필요)
./gradlew test jacocoTestReport

# 커버리지 리포트 확인
open build/reports/jacoco/test/html/index.html
```

### 테스트 환경 설정
테스트는 `src/test/resources/application-test.yml` 설정을 사용하며, H2 인메모리 데이터베이스를 사용합니다.

## API 엔드포인트

### 인증
- `POST /api/auth/login` - 로그인
- `POST /api/auth/signup` - 회원가입
- `POST /api/auth/logout` - 로그아웃

### 전자결재
- `GET /api/approval/documents` - 결재 문서 목록
- `POST /api/approval/documents` - 결재 문서 작성
- `GET /api/approval/documents/{id}` - 결재 문서 상세
- `POST /api/approval/documents/{id}/approve` - 결재 승인
- `POST /api/approval/documents/{id}/reject` - 결재 반려

### 게시판
- `GET /api/boards/{boardId}/posts` - 게시글 목록
- `POST /api/boards/{boardId}/posts` - 게시글 작성
- `GET /api/posts/{id}` - 게시글 상세
- `PUT /api/posts/{id}` - 게시글 수정
- `DELETE /api/posts/{id}` - 게시글 삭제
- `POST /api/posts/{id}/comments` - 댓글 작성

### 조직도
- `GET /api/departments` - 부서 목록
- `GET /api/departments/{id}/members` - 부서원 목록
- `GET /api/positions` - 직급 목록

### 알림
- `GET /api/notifications` - 알림 목록
- `PUT /api/notifications/{id}/read` - 알림 읽음 처리
- `DELETE /api/notifications/{id}` - 알림 삭제

## 프로젝트 구조

```
liteware/
├── src/
│   ├── main/
│   │   ├── java/com/liteware/
│   │   │   ├── config/          # 설정 클래스
│   │   │   ├── controller/      # 컨트롤러
│   │   │   ├── model/           # 엔티티, DTO
│   │   │   ├── repository/      # 리포지토리
│   │   │   ├── service/         # 서비스 로직
│   │   │   └── security/        # 보안 관련
│   │   └── resources/
│   │       ├── templates/       # Thymeleaf 템플릿
│   │       └── application.yml  # 설정 파일
│   └── test/                    # 테스트 코드
├── build.gradle                 # Gradle 빌드 설정
├── Dockerfile                   # Docker 이미지 빌드
├── docker-compose.yml          # Docker Compose 설정
└── README.md                   # 프로젝트 문서
```

## 개발 가이드

### 브랜치 전략
- `main` - 프로덕션 배포 브랜치
- `develop` - 개발 통합 브랜치
- `feature/*` - 기능 개발 브랜치
- `hotfix/*` - 긴급 수정 브랜치

### 커밋 메시지 규칙
- `FEAT:` - 새로운 기능 추가
- `FIX:` - 버그 수정
- `DOCS:` - 문서 수정
- `STYLE:` - 코드 포맷팅
- `REFACTOR:` - 코드 리팩토링
- `TEST:` - 테스트 코드
- `CHORE:` - 빌드 업무, 패키지 매니저 수정 등

## 문제 해결

### 포트 충돌
기본 포트가 사용 중인 경우:
- 애플리케이션: `application.yml`에서 `server.port` 변경
- MySQL: `docker-compose.yml`에서 포트 매핑 변경
- Redis: `docker-compose.yml`에서 포트 매핑 변경

### 데이터베이스 연결 실패
1. MySQL 서비스가 실행 중인지 확인
2. 사용자 권한 확인
3. 방화벽 설정 확인
4. `application.yml`의 연결 정보 확인

### Docker 빌드 실패
1. Docker 데몬이 실행 중인지 확인
2. 디스크 공간 확인
3. `docker system prune`로 불필요한 리소스 정리

## 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.

## 기여하기

프로젝트에 기여하고 싶으시다면:
1. 프로젝트를 Fork 합니다
2. Feature 브랜치를 생성합니다 (`git checkout -b feature/AmazingFeature`)
3. 변경사항을 커밋합니다 (`git commit -m 'FEAT: Add some AmazingFeature'`)
4. 브랜치에 푸시합니다 (`git push origin feature/AmazingFeature`)
5. Pull Request를 생성합니다

## 문의

프로젝트 관련 문의사항이 있으시면 이슈를 등록해주세요.
