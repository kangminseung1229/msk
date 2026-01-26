# 스트리밍 응답을 위한 Nginx 설정 가이드

## 문제 상황
로컬에서는 스트리밍이 잘 작동하지만, `mining.properties` 환경(프로덕션)에서는 스트리밍이 안 되고 한번에 나와서 느린 것처럼 느껴집니다.

## 원인
리버스 프록시(nginx 등)가 HTTP 응답을 버퍼링하여 스트리밍이 제대로 작동하지 않을 수 있습니다.

## 해결 방법

### 1. Nginx 설정 수정

SSE(Server-Sent Events) 스트리밍 엔드포인트에 대해 버퍼링을 비활성화해야 합니다.

#### `/etc/nginx/sites-available/your-site` 또는 nginx 설정 파일에 추가:

```nginx
server {
    listen 80;
    server_name mining.taxnet.co.kr;

    # SSE 스트리밍 엔드포인트에 대한 특별 설정
    location ~ ^/counsel-ai/api/(test/agent/stream|gemini/streaming-sse) {
        proxy_pass http://localhost:8080;
        
        # 버퍼링 비활성화 (스트리밍을 위해 필수)
        proxy_buffering off;
        proxy_cache off;
        
        # HTTP 버전 설정
        proxy_http_version 1.1;
        
        # 연결 유지 설정
        proxy_set_header Connection '';
        
        # 헤더 설정
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 타임아웃 설정 (스트리밍은 오래 걸릴 수 있음)
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
        
        # 청크 전송 활성화
        chunked_transfer_encoding on;
        
        # 압축 비활성화 (스트리밍 시 버퍼링 방지)
        gzip off;
    }

    # 일반 API 엔드포인트 (버퍼링 활성화 가능)
    location /counsel-ai/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 일반 API는 압축 활성화 가능
        gzip on;
        gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
    }
}
```

### 2. 주요 설정 설명

- **`proxy_buffering off`**: 프록시 버퍼링을 완전히 비활성화합니다. 이 설정이 가장 중요합니다.
- **`proxy_cache off`**: 프록시 캐시를 비활성화합니다.
- **`proxy_http_version 1.1`**: HTTP/1.1을 사용하여 청크 전송을 활성화합니다.
- **`chunked_transfer_encoding on`**: 청크 전송 인코딩을 명시적으로 활성화합니다.
- **`gzip off`**: 압축을 비활성화합니다. 압축은 버퍼링을 유발할 수 있습니다.
- **`proxy_read_timeout 300s`**: 읽기 타임아웃을 늘려 긴 스트리밍 응답을 처리할 수 있도록 합니다.

### 3. Nginx 설정 적용

```bash
# 설정 파일 검증
sudo nginx -t

# Nginx 재시작
sudo systemctl reload nginx
# 또는
sudo service nginx reload
```

### 4. Apache를 사용하는 경우

Apache를 리버스 프록시로 사용하는 경우:

```apache
<LocationMatch "^/counsel-ai/api/(test/agent/stream|gemini/streaming-sse)">
    ProxyPass http://localhost:8080
    ProxyPassReverse http://localhost:8080
    
    # 버퍼링 비활성화
    ProxyPreserveHost On
    RequestHeader set Connection ""
    
    # 청크 전송 활성화
    SetEnv proxy-nokeepalive 1
    SetEnv proxy-sendchunked 1
</LocationMatch>
```

### 5. 테스트

설정 적용 후 다음 명령어로 스트리밍이 제대로 작동하는지 확인:

```bash
# SSE 스트리밍 테스트
curl -N -H "Accept: text/event-stream" \
  https://mining.taxnet.co.kr/counsel-ai/api/test/agent/stream \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"message":"테스트 메시지","sessionId":"test-123"}'
```

`-N` 옵션은 버퍼링을 비활성화하여 실시간으로 응답을 받을 수 있게 합니다.

### 6. 추가 확인 사항

1. **로드 밸런서**: AWS ALB, F5 등 로드 밸런서를 사용하는 경우, 해당 설정에서도 버퍼링을 비활성화해야 합니다.

2. **CDN**: CloudFlare 등 CDN을 사용하는 경우, 해당 경로에 대해 캐싱을 비활성화해야 합니다.

3. **방화벽/보안 장비**: 일부 보안 장비가 응답을 버퍼링할 수 있습니다.

## 참고

- Spring Boot 애플리케이션에서는 `application-mining.properties`에 다음 설정이 추가되었습니다:
  - `server.compression.enabled=false`: HTTP 응답 압축 비활성화
  - 이 설정은 스트리밍 시 버퍼링을 방지합니다.

- `LlmNode.java`의 스트리밍 로직도 개선되어 델타만 전송하도록 최적화되었습니다.
