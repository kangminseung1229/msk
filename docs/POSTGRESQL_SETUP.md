# PostgreSQL 및 pgvector 설정 가이드

## 📋 목차
1. [PostgreSQL 설치](#1-postgresql-설치)
2. [pgvector 확장 설치](#2-pgvector-확장-설치)
3. [데이터베이스 생성](#3-데이터베이스-생성)
4. [연결 테스트](#4-연결-테스트)

---

## 1. PostgreSQL 설치

### macOS (Homebrew)
```bash
# PostgreSQL 설치
brew install postgresql@15

# PostgreSQL 서비스 시작
brew services start postgresql@15

# PostgreSQL 접속
psql postgres
```

### Linux (Ubuntu/Debian)
```bash
# PostgreSQL 설치
sudo apt-get update
sudo apt-get install postgresql postgresql-contrib

# PostgreSQL 서비스 시작
sudo systemctl start postgresql
sudo systemctl enable postgresql

# PostgreSQL 접속
sudo -u postgres psql
```

### Docker (권장 - 간편함)
```bash
# PostgreSQL + pgvector 이미지 사용
docker run --name postgres-pgvector \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=consultation_db \
  -p 5432:5432 \
  -d pgvector/pgvector:pg15
```

---

## 2. pgvector 확장 설치

### 방법 1: Docker 이미지 사용 (가장 간단)

pgvector가 포함된 Docker 이미지를 사용하면 별도 설치가 필요 없습니다.

```bash
# pgvector 포함 이미지 사용
docker run --name postgres-pgvector \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=consultation_db \
  -p 5432:5432 \
  -d pgvector/pgvector:pg15

# 접속 확인
docker exec -it postgres-pgvector psql -U postgres -d consultation_db
```

### 방법 2: 수동 설치 (macOS)

```bash
# Homebrew로 pgvector 설치
brew install pgvector

# 또는 소스에서 빌드
git clone --branch v0.5.1 https://github.com/pgvector/pgvector.git
cd pgvector
make
make install
```

### 방법 3: 수동 설치 (Linux)

```bash
# 의존성 설치
sudo apt-get install build-essential postgresql-server-dev-15

# pgvector 다운로드 및 빌드
git clone --branch v0.5.1 https://github.com/pgvector/pgvector.git
cd pgvector
make
sudo make install
```

### 확장 활성화

PostgreSQL에 접속하여 확장을 활성화합니다:

```sql
-- PostgreSQL 접속
psql -U postgres -d consultation_db

-- pgvector 확장 설치
CREATE EXTENSION IF NOT EXISTS vector;

-- 확장 확인
\dx vector

-- 벡터 타입 테스트
SELECT '[1,2,3]'::vector;
```

---

## 3. 데이터베이스 생성

### SQL로 생성

```sql
-- PostgreSQL 접속 (postgres 사용자)
psql -U postgres

-- 데이터베이스 생성
CREATE DATABASE consultation_db;

-- 사용자 생성 (선택사항)
CREATE USER consultation_user WITH PASSWORD 'your_password';

-- 권한 부여
GRANT ALL PRIVILEGES ON DATABASE consultation_db TO consultation_user;

-- 데이터베이스 접속
\c consultation_db

-- pgvector 확장 활성화
CREATE EXTENSION IF NOT EXISTS vector;
```

### Docker Compose 사용 (권장)

`docker-compose.yml` 파일 생성:

```yaml
version: '3.8'

services:
  postgres:
    image: pgvector/pgvector:pg15
    container_name: postgres-pgvector
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: consultation_db
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```

실행:
```bash
docker-compose up -d
```

---

## 4. 연결 테스트

### application.properties 설정

```properties
# PostgreSQL 연결
spring.datasource.url=jdbc:postgresql://localhost:5432/consultation_db
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver
```

### 애플리케이션 실행

```bash
./mvnw spring-boot:run
```

### 로그 확인

애플리케이션 시작 시 다음과 같은 로그가 보이면 성공:

```
HikariPool-1 - Starting...
HikariPool-1 - Start completed.
```

### 수동 연결 테스트

```bash
# psql로 직접 접속
psql -h localhost -U postgres -d consultation_db

# 테이블 확인
\dt

# pgvector 확장 확인
SELECT * FROM pg_extension WHERE extname = 'vector';
```

---

## 🔧 문제 해결

### 1. pgvector 확장을 찾을 수 없음

**에러**:
```
ERROR: could not open extension control file "/usr/share/postgresql/15/extension/vector.control"
```

**해결**:
- pgvector가 제대로 설치되었는지 확인
- PostgreSQL 버전과 pgvector 버전 호환성 확인
- Docker 이미지 사용 권장: `pgvector/pgvector:pg15`

### 2. 연결 거부

**에러**:
```
Connection refused
```

**해결**:
- PostgreSQL 서비스가 실행 중인지 확인
- 포트가 올바른지 확인 (기본: 5432)
- 방화벽 설정 확인

### 3. 인증 실패

**에러**:
```
FATAL: password authentication failed
```

**해결**:
- 사용자명과 비밀번호 확인
- `pg_hba.conf` 파일에서 인증 방식 확인

### 4. 데이터베이스가 존재하지 않음

**에러**:
```
FATAL: database "consultation_db" does not exist
```

**해결**:
- 데이터베이스 생성 확인
- `CREATE DATABASE consultation_db;` 실행

---

## 📝 체크리스트

설정 완료 확인:

- [ ] PostgreSQL 설치 완료
- [ ] pgvector 확장 설치 완료
- [ ] 데이터베이스 생성 완료
- [ ] pgvector 확장 활성화 (`CREATE EXTENSION vector;`)
- [ ] application.properties 설정 완료
- [ ] 애플리케이션 연결 테스트 성공
- [ ] Spring AI Vector Store 테이블 자동 생성 확인

---

## 🚀 빠른 시작 (Docker)

가장 빠른 방법:

```bash
# 1. Docker로 PostgreSQL + pgvector 실행
docker run --name postgres-pgvector \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=consultation_db \
  -p 5432:5432 \
  -d pgvector/pgvector:pg15

# 2. 확장 활성화
docker exec -it postgres-pgvector psql -U postgres -d consultation_db -c "CREATE EXTENSION IF NOT EXISTS vector;"

# 3. application.properties 설정
# spring.datasource.url=jdbc:postgresql://localhost:5432/consultation_db
# spring.datasource.username=postgres
# spring.datasource.password=postgres

# 4. 애플리케이션 실행
./mvnw spring-boot:run
```

---

**마지막 업데이트**: 2025-01-XX
