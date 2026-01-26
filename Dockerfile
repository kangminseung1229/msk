# 멀티 스테이지 빌드를 사용하여 최적화된 이미지 생성

# Stage 1: 빌드 스테이지
FROM maven:3.9-eclipse-temurin-17 AS build

# 작업 디렉토리 설정
WORKDIR /app

# Maven 의존성 캐시를 활용하기 위해 pom.xml 먼저 복사
COPY pom.xml ./

# 의존성 다운로드 (레이어 캐싱 최적화)
RUN mvn dependency:go-offline -B || true

# 소스 코드 복사
COPY src ./src

# 애플리케이션 빌드
RUN mvn clean package -DskipTests -B

# fat JAR만 app.jar로 복사 (Spring Boot -plain.jar 제외)
RUN cp /app/target/$$(find /app/target -maxdepth 1 -name '*.jar' ! -name '*-plain*' -print | head -1) /app/app.jar

# Stage 2: 실행 스테이지
FROM eclipse-temurin:17-jre-alpine

# 작업 디렉토리 설정
WORKDIR /app

# 빌드된 JAR 복사
COPY --from=build /app/app.jar app.jar

# 체크포인트 디렉토리 생성
RUN mkdir -p /app/checkpoints

# 포트 노출 (Spring Boot 기본 포트)
EXPOSE 8080

# 애플리케이션 실행
# 환경 변수는 docker-compose.yml 또는 docker run 시 설정
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]
