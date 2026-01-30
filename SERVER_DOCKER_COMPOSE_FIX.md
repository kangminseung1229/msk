# 서버 docker-compose.yml 수정 가이드

## 🔍 발견된 문제

실제 서버의 `docker-compose.yml`을 확인한 결과, 다음 문제들이 발견되었습니다:

### 1. 환경 변수 이름 불일치
**문제**:
- `docker-compose.yml`: `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT`, `SPRING_DATA_REDIS_PASSWORD` 사용
- `application.properties`: `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` 사용

**영향**:
- Spring Boot가 환경 변수를 제대로 읽지 못함
- Redis 연결 실패로 세션 관리 불가

### 2. Redis 헬스 체크 누락
**문제**:
- Redis 서비스에 헬스 체크가 없음
- `depends_on`이 단순히 컨테이너 시작만 기다림 (실제 준비 상태 확인 안 함)

**영향**:
- Redis가 완전히 준비되기 전에 애플리케이션이 시작될 수 있음
- 연결 실패 가능성 증가

## ✅ 수정 사항

### 1. 환경 변수 이름 수정
```yaml
# 변경 전
- SPRING_DATA_REDIS_HOST=redis
- SPRING_DATA_REDIS_PORT=6379
- SPRING_DATA_REDIS_PASSWORD=${REDIS_PASSWORD}

# 변경 후
- REDIS_HOST=redis
- REDIS_PORT=6379
- REDIS_PASSWORD=${REDIS_PASSWORD}
```

### 2. Redis 헬스 체크 추가
```yaml
redis:
  healthcheck:
    test: ["CMD", "redis-cli", "--raw", "incr", "ping"]
    interval: 10s
    timeout: 3s
    retries: 5
```

### 3. depends_on 조건 추가
```yaml
counsel-ai:
  depends_on:
    redis:
      condition: service_healthy  # 헬스 체크 통과 후 시작
```

## 🚀 적용 방법

### 방법 1: 파일 직접 수정 (권장)

서버에 SSH 접속 후:

```bash
# 1. 백업
cp /root/docker/docker-compose.yml /root/docker/docker-compose.yml.backup

# 2. 파일 수정
vi /root/docker/docker-compose.yml
```

**수정할 부분**:

1. **Redis 헬스 체크 추가** (141-151줄):
```yaml
  redis:
    image: redis:7-alpine
    container_name: redis-cache
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes --requirepass ${REDIS_PASSWORD}
    networks:
      - taxnet_network
    restart: unless-stopped
    healthcheck:  # 추가
      test: ["CMD", "redis-cli", "--raw", "incr", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5
```

2. **counsel-ai 환경 변수 수정** (124-126줄):
```yaml
      # 변경 전
      - SPRING_DATA_REDIS_HOST=redis
      - SPRING_DATA_REDIS_PORT=6379
      - SPRING_DATA_REDIS_PASSWORD=${REDIS_PASSWORD}
      
      # 변경 후
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - REDIS_PASSWORD=${REDIS_PASSWORD}
```

3. **depends_on 조건 추가** (132-133줄):
```yaml
    depends_on:
      redis:
        condition: service_healthy  # 추가
```

### 방법 2: 제공된 파일 사용

```bash
# 1. 로컬에서 수정된 파일을 서버로 전송
scp docker-compose-server-fix.yml root@mining.taxnet.co.kr:/root/docker/docker-compose.yml

# 2. 서버에서 확인 후 적용
ssh root@mining.taxnet.co.kr
cd /root/docker
docker-compose up -d --build redis counsel-ai
```

## 📋 적용 후 확인

### 1. Redis 헬스 체크 확인
```bash
docker ps
# redis-cache 컨테이너의 STATUS에 "healthy" 표시 확인
```

### 2. 환경 변수 확인
```bash
docker exec counsel-ai-service env | grep REDIS
# 다음이 출력되어야 함:
# REDIS_HOST=redis
# REDIS_PORT=6379
# REDIS_PASSWORD=...
```

### 3. 애플리케이션 로그 확인
```bash
docker logs counsel-ai-service | grep -i redis
# Redis 연결 성공 메시지 확인
```

### 4. API 테스트
```bash
# 세션 생성 테스트
curl -X POST http://localhost:9096/api/v2/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "테스트", "sessionId": "test-1"}'

# 같은 세션으로 재요청 (히스토리 유지 확인)
curl -X POST http://localhost:9096/api/v2/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "이전 대화 기억하나요?", "sessionId": "test-1"}'
```

## ⚠️ 주의사항

1. **환경 변수 REDIS_PASSWORD 설정 확인**
   ```bash
   # 서버에서 확인
   echo $REDIS_PASSWORD
   # 설정되어 있지 않으면 설정 필요
   export REDIS_PASSWORD=your-redis-password
   ```

2. **기존 Redis 데이터**
   - Redis 볼륨(`redis_data`)에 기존 데이터가 있을 수 있음
   - 비밀번호가 변경되면 기존 데이터 접근 불가
   - 필요시 Redis 데이터 백업 고려

3. **다른 서비스 영향**
   - `nginx` 서비스가 `counsel-ai`에 의존하므로, `counsel-ai`가 정상 시작되어야 함
   - 배포 시 순서: Redis → counsel-ai → nginx

## 🔧 문제 발생 시

### Redis 연결 실패
```bash
# 1. Redis 컨테이너 상태 확인
docker ps -a | grep redis
docker logs redis-cache

# 2. Redis 비밀번호 확인
docker exec redis-cache redis-cli -a $REDIS_PASSWORD ping
# PONG이 출력되어야 함

# 3. 네트워크 연결 확인
docker exec counsel-ai-service ping redis
```

### 환경 변수 미적용
```bash
# 1. 컨테이너 재시작
docker-compose restart counsel-ai

# 2. 환경 변수 재확인
docker exec counsel-ai-service env | grep REDIS

# 3. 필요시 재빌드
docker-compose up -d --build counsel-ai
```

## 📝 변경 요약

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| Redis 환경 변수 | `SPRING_DATA_REDIS_*` | `REDIS_*` |
| Redis 헬스 체크 | 없음 | 추가됨 |
| depends_on 조건 | 없음 | `service_healthy` 추가 |
