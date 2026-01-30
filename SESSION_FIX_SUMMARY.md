# 세션별 API 접근 문제 해결 요약

## 🔍 문제 원인
운영 서버에서 세션마다 API 접근이 다른 이유는 **Redis 서비스가 실행되지 않아** 세션 관리가 실패했기 때문입니다.

## ✅ 해결 내용

### 1. Docker Compose에 Redis 추가
- `docker-compose.yml`에 Redis 서비스 추가
- Redis 헬스 체크 설정
- 애플리케이션이 Redis 준비 후 시작하도록 설정

### 2. Redis 연결 설정 개선
- 타임아웃: 2초 → 5초
- 연결 풀 크기: 8 → 16
- 최소 유휴 연결: 0 → 2
- 환경 변수 지원 추가

### 3. 에러 처리 및 로깅 강화
- Redis 연결 실패 시 명확한 오류 메시지
- 상세한 로깅으로 문제 추적 가능
- Redis 연결 상태 확인 메서드 추가

### 4. 배포 스크립트 개선
- Redis 서비스도 함께 시작하도록 수정
- 배포 후 Redis 상태 확인 추가

## 🚀 배포 방법

### 기존 방법 (변경 없음)
```bash
./deploy.sh
```

### 수동 배포 시
```bash
# 1. docker-compose.yml과 Dockerfile을 서버에 복사
# 2. 서버에서 실행
cd /root/docker
docker-compose up -d --build redis app
```

## 📋 배포 후 확인

### 1. Redis 컨테이너 확인
```bash
docker ps | grep redis
# aiagent-redis 컨테이너가 실행 중이어야 함
```

### 2. 애플리케이션 로그 확인
```bash
docker logs aiagent-app | grep -i redis
# Redis 연결 성공 메시지 확인
```

### 3. API 테스트
```bash
# 세션 생성
curl -X POST http://your-server:8080/api/v2/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "테스트", "sessionId": "test-1"}'

# 같은 세션으로 재요청 (히스토리 유지 확인)
curl -X POST http://your-server:8080/api/v2/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "이전 대화 기억하나요?", "sessionId": "test-1"}'
```

## ⚠️ 주의사항

### 환경 변수 설정
운영 서버에서 다음 환경 변수를 설정해야 합니다:
- `REDIS_PASSWORD`: Redis 비밀번호 (기본값: redisjose1234)
- `GEMINI_API_KEY`: Google Gemini API 키
- `DB_URL`, `DB_USER`, `DB_PASSWORD`: 데이터베이스 연결 정보

### Docker Compose 파일
운영 서버의 `/root/docker` 디렉토리에 다음 파일들이 있어야 합니다:
- `docker-compose.yml` (업데이트된 버전)
- `Dockerfile`
- `counsel-ai.jar` (배포된 JAR 파일)

## 🔧 문제 발생 시

### Redis 연결 실패
1. Redis 컨테이너 상태 확인: `docker ps -a | grep redis`
2. Redis 로그 확인: `docker logs aiagent-redis`
3. 네트워크 연결 확인: `docker exec -it aiagent-app ping redis`
4. Redis 재시작: `docker-compose restart redis`

### 세션 저장 실패
1. 애플리케이션 로그 확인: `docker logs aiagent-app | grep SessionStore`
2. Redis 메모리 확인: `docker exec -it aiagent-redis redis-cli -a redisjose1234 INFO memory`
3. Redis 연결 테스트: `docker exec -it aiagent-redis redis-cli -a redisjose1234 ping`

## 📊 변경된 파일 목록

1. `docker-compose.yml` - Redis 서비스 추가
2. `application.properties` - Redis 연결 설정 개선
3. `SessionStore.java` - 에러 처리 및 로깅 강화
4. `deploy.sh` - Redis 서비스 시작 추가
5. `SESSION_ISSUE_ANALYSIS.md` - 상세 분석 문서 (신규)

## 🔎 "다른 세션은 답변할 수 없다"고 나올 때

서버 로그에는 **세션 A, 세션 B 모두 "스트리밍 채팅 완료"**로 찍히는데, 클라이언트에서는 "다른 세션에서는 답변이 안 된다"고 할 수 있습니다. 이 경우 백엔드는 정상이므로 아래를 순서대로 확인하세요.

### 1. Redis가 실제로 연결되어 있는지
- 앱 로그에 `SessionStore: Redis 연결 실패` 같은 에러가 없어야 합니다.
- Redis가 꺼져 있으면 `loadSession`/`getHistory`는 실패해도 채팅 자체는 완료됩니다(히스토리만 비어 있음).  
  → "다른 세션" 문제의 직접 원인은 아닐 수 있지만, 배포 시 `SPRING_DATA_REDIS_HOST=redis`(또는 `REDIS_HOST=redis`)가 설정되어 있는지, Redis 컨테이너가 떠 있는지 확인하세요.

### 2. Nginx(또는 프록시)의 SSE/스트리밍 설정
- 스트리밍(SSE)은 연결을 오래 유지합니다. **proxy_read_timeout**이 짧으면 중간에 끊겨서 클라이언트에 "연결 오류" / "스트리밍 중 오류"로 보일 수 있습니다.
- **버퍼링**을 끄는 것이 좋습니다.  
  예시:
  ```nginx
  proxy_buffering off;
  proxy_read_timeout 300s;   # 필요에 따라 300초 이상
  proxy_http_version 1.1;
  chunked_transfer_encoding off;
  ```
- "첫 번째 세션(탭)만 되고, 두 번째 세션(탭)은 안 된다"면, 프록시에서 동시 장시간 연결 수 제한이 있는지도 확인하세요.

### 3. 브라우저 동시 연결 수
- 같은 도메인에 대한 동시 HTTP 연결 수가 제한되어 있어서, 여러 탭에서 동시에 스트리밍하면 한쪽이 실패할 수 있습니다.
- 증상: 한 탭에서는 되고, 다른 탭에서 "연결 오류가 발생했습니다" 등으로 끝남.

### 4. 프론트엔드(클라이언트) 동작
- "다른 세션"이 **다른 브라우저 / 다른 기기 / 시크릿 창**인 경우, 세션 ID가 달라서 **새 세션**으로 들어갑니다. 서버는 새 세션도 동일하게 처리합니다.
- 클라이언트에서 "세션 전환" 시 이전 세션에 대해 "답변할 수 없습니다" 같은 문구를 **UI로만** 보여주는 경우, 그건 백엔드가 막은 것이 아니라 프론트 정책일 수 있습니다.  
  → 표시되는 **정확한 문구**와 **어떤 상황에서**(같은 브라우저 다른 탭, 다른 기기, 세션 전환 버튼 등) 나오는지 확인하면 원인 좁히는 데 도움이 됩니다.

### 5. 확인용 요약
| 확인 항목 | 방법 |
|----------|------|
| 서버에서 두 세션 모두 완료 여부 | 로그에서 `ChatV2Service: 스트리밍 채팅 완료 - sessionId: ...` 가 각 세션 ID마다 찍히는지 |
| Redis 연결 | 로그에 `SessionStore: Redis 연결 실패` 없음, 배포 시 Redis 호스트/비밀번호 설정 |
| 프록시 타임아웃/버퍼링 | Nginx 등에서 `proxy_buffering off`, `proxy_read_timeout` 충분히 긴지 |
| 클라이언트 메시지 | "답변할 수 없습니다"가 서버 에러 응답인지, 프론트에서만 보여주는 문구인지 |

---

## 📝 추가 권장 사항

1. **Redis 모니터링**: Redis 메모리 사용량 및 연결 수 모니터링
2. **백업**: Redis 데이터 백업 정책 수립
3. **고가용성**: 필요시 Redis Sentinel 또는 Cluster 구성 고려
4. **로깅**: Redis 연결 실패 시 알림 설정 고려
