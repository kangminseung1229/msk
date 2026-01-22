# Docker 설정 가이드

## 사전 요구사항

- Docker 및 Docker Compose 설치
- Google Gemini API 키

## 빌드 및 실행

### 1. 환경 변수 설정

```bash
export GEMINI_API_KEY=your-gemini-api-key-here
```

또는 `.env` 파일 생성:

```bash
echo "GEMINI_API_KEY=your-gemini-api-key-here" > .env
```

### 2. Docker Compose로 전체 스택 실행

```bash
docker-compose up -d
```

이 명령은 다음을 실행합니다:
- PostgreSQL 데이터베이스 (pgvector 확장 포함)
- Spring Boot 애플리케이션

### 3. 애플리케이션만 빌드 및 실행

```bash
# 이미지 빌드
docker build -t msk-app .

# 컨테이너 실행
docker run -d \
  --name msk-app \
  -p 8080:8080 \
  -e GEMINI_API_KEY=your-api-key \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/consultation_db \
  -e DB_USER=postgres \
  -e DB_PASSWORD=postgres \
  msk-app
```

### 4. 로그 확인

```bash
# 모든 서비스 로그
docker-compose logs -f

# 특정 서비스 로그
docker-compose logs -f app
docker-compose logs -f postgres
```

### 5. 중지 및 정리

```bash
# 서비스 중지
docker-compose down

# 볼륨까지 삭제 (데이터 삭제됨)
docker-compose down -v
```

## 환경 변수

### 필수 환경 변수

- `GEMINI_API_KEY`: Google Gemini API 키

### 선택적 환경 변수

- `DB_URL`: 데이터베이스 연결 URL (기본값: `jdbc:postgresql://postgres:5432/consultation_db`)
- `DB_USER`: 데이터베이스 사용자명 (기본값: `postgres`)
- `DB_PASSWORD`: 데이터베이스 비밀번호 (기본값: `postgres`)
- `SPRING_PROFILES_ACTIVE`: Spring 프로파일 (기본값: `prod`)

## 포트

- `8080`: Spring Boot 애플리케이션
- `5432`: PostgreSQL 데이터베이스

## 헬스 체크

애플리케이션 헬스 체크:

```bash
curl http://localhost:8080/actuator/health
```

## 문제 해결

### 데이터베이스 연결 오류

PostgreSQL이 준비될 때까지 애플리케이션이 대기하도록 `depends_on`과 `healthcheck`가 설정되어 있습니다.

### API 키 오류

환경 변수 `GEMINI_API_KEY`가 올바르게 설정되었는지 확인하세요.

### 포트 충돌

다른 포트를 사용하려면 `docker-compose.yml`의 포트 매핑을 수정하세요.
