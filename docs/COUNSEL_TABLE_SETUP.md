# Counsel 테이블 설정 가이드

## 📋 개요

다른 RDB에 있는 `counsel` 테이블의 데이터를 가져와서 임베딩하고 Vector Store에 저장하는 방법을 설명합니다.

---

## 🔧 설정 방법

### 1. 데이터베이스 연결 설정

`application.properties`에 다른 RDB의 연결 정보를 추가합니다.

#### 방법 1: 같은 PostgreSQL 인스턴스의 다른 데이터베이스

```properties
# 기존 pgvector DB (임베딩 저장용)
spring.datasource.url=jdbc:postgresql://localhost:5432/consultation_db
spring.datasource.username=postgres
spring.datasource.password=postgres

# Counsel 테이블이 있는 다른 DB (읽기 전용)
# 별도 DataSource 설정 필요 (아래 참고)
```

#### 방법 2: 다른 서버의 데이터베이스

```properties
# Counsel 테이블이 있는 원본 DB
counsel.datasource.url=jdbc:postgresql://other-server:5432/original_db
counsel.datasource.username=readonly_user
counsel.datasource.password=readonly_password
```

---

## 📝 Counsel 엔티티 수정

`Counsel.java` 파일을 실제 테이블 구조에 맞게 수정해야 합니다.

### 1. 테이블 구조 확인

```sql
-- 다른 RDB에서 실행
\d counsel
-- 또는
DESCRIBE counsel;
```

### 2. 엔티티 필드 매핑

실제 테이블 컬럼명에 맞게 `@Column` 어노테이션을 수정하세요.

**예시 1: 컬럼명이 다른 경우**
```java
@Column(name = "counsel_id")  // 실제 컬럼명
private Long id;

@Column(name = "counsel_title")  // 실제 컬럼명
private String title;
```

**예시 2: 추가 필드가 있는 경우**
```java
@Column(name = "status")
private String status;

@Column(name = "user_id")
private Long userId;

@Column(name = "reg_date")
private LocalDateTime regDate;
```

---

## 🔄 다중 데이터소스 설정 (필요한 경우)

Counsel 테이블이 다른 데이터베이스에 있는 경우, 다중 데이터소스 설정이 필요합니다.

### 1. DataSource 설정 클래스

```java
@Configuration
public class DataSourceConfig {

    // pgvector DB (기본)
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    // Counsel 테이블이 있는 다른 DB
    @Bean
    @ConfigurationProperties("counsel.datasource")
    public DataSource counselDataSource() {
        return DataSourceBuilder.create().build();
    }

    // Counsel용 EntityManagerFactory
    @Bean
    public LocalContainerEntityManagerFactoryBean counselEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("counselDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("ai.langgraph4j.msk.entity")
                .persistenceUnit("counsel")
                .build();
    }

    // Counsel용 TransactionManager
    @Bean
    public PlatformTransactionManager counselTransactionManager(
            @Qualifier("counselEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
```

### 2. CounselRepository에 @Qualifier 추가

```java
@Repository
@Qualifier("counselEntityManagerFactory")
public interface CounselRepository extends JpaRepository<Counsel, Long> {
    // ...
}
```

---

## 📊 데이터 마이그레이션 방법

### 방법 1: 직접 연결 (권장)

다른 RDB에서 직접 읽어서 임베딩합니다.

**장점:**
- 실시간 데이터 반영 가능
- 원본 데이터 유지
- 별도 마이그레이션 불필요

**단점:**
- 다중 데이터소스 설정 필요
- 네트워크 지연 가능

### 방법 2: 데이터 복사

다른 RDB에서 pgvector DB로 데이터를 복사한 후 임베딩합니다.

**SQL 예시:**
```sql
-- pgvector DB에서 실행
CREATE TABLE counsel AS 
SELECT * FROM dblink(
    'host=other-server dbname=original_db user=readonly_user password=readonly_password',
    'SELECT * FROM counsel'
) AS t(id bigint, title text, content text, ...);
```

**장점:**
- 단일 데이터소스로 간단
- 빠른 조회

**단점:**
- 데이터 동기화 필요
- 저장 공간 추가 필요

---

## 🚀 사용 방법

### 1. Counsel 엔티티 수정

실제 테이블 구조에 맞게 `Counsel.java` 수정:

```java
@Entity
@Table(name = "counsel")
public class Counsel {
    @Id
    @Column(name = "실제_PK_컬럼명")
    private Long id;
    
    @Column(name = "실제_제목_컬럼명")
    private String title;
    
    // ... 실제 컬럼에 맞게 수정
}
```

### 2. application.properties 설정

```properties
# Counsel 테이블이 있는 DB 연결 정보
# 같은 DB인 경우 기존 설정 사용
# 다른 DB인 경우 다중 데이터소스 설정 필요
```

### 3. 임베딩 실행

#### API 호출
```bash
# 전체 상담 임베딩
curl -X POST http://localhost:8080/api/embedding/all

# 답변이 있는 상담만 임베딩
curl -X POST http://localhost:8080/api/embedding/with-answer

# 특정 상담 임베딩
curl -X POST http://localhost:8080/api/embedding/123
```

#### 코드에서 직접 호출
```java
@Autowired
private ConsultationEmbeddingService embeddingService;

// 전체 임베딩
int count = embeddingService.embedAllConsultations();

// 답변이 있는 것만
int count = embeddingService.embedConsultationsWithAnswer();
```

---

## 🔍 테이블 구조 확인 방법

### PostgreSQL
```sql
-- 테이블 구조 확인
\d counsel

-- 컬럼 정보 확인
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'counsel';
```

### MySQL
```sql
-- 테이블 구조 확인
DESCRIBE counsel;

-- 컬럼 정보 확인
SHOW COLUMNS FROM counsel;
```

### SQL Server
```sql
-- 테이블 구조 확인
EXEC sp_columns 'counsel';
```

---

## ⚠️ 주의사항

1. **컬럼명 매핑**: 실제 테이블의 컬럼명과 엔티티의 `@Column` 어노테이션이 일치해야 합니다.

2. **데이터 타입**: 엔티티의 필드 타입이 실제 컬럼 타입과 호환되어야 합니다.

3. **NULL 처리**: NULL 값이 가능한 컬럼은 엔티티에서도 nullable로 설정하세요.

4. **대용량 데이터**: 데이터가 많으면 배치로 나누어 처리하세요.

5. **인덱스**: 임베딩 후 검색 성능을 위해 적절한 인덱스가 필요합니다.

---

## 📝 체크리스트

- [ ] Counsel 테이블 구조 확인
- [ ] Counsel 엔티티 필드 매핑 수정
- [ ] 데이터베이스 연결 설정
- [ ] 다중 데이터소스 설정 (필요한 경우)
- [ ] 임베딩 서비스 테스트
- [ ] Vector Store에 데이터 저장 확인

---

**마지막 업데이트**: 2025-01-XX
