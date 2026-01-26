#!/bin/bash

# 배포 스크립트
# 빌드 -> SCP 전송 -> Docker 배포

set -e  # 오류 발생 시 스크립트 중단

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 설정
REMOTE_HOST="mining.taxnet.co.kr"
REMOTE_PATH="/root/docker"
JAR_NAME="counsel-ai.jar"
SERVICE_NAME="counsel-ai"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}배포 스크립트 시작${NC}"
echo -e "${GREEN}========================================${NC}"

# 1. 빌드
echo -e "${YELLOW}[1/3] Maven 빌드 중...${NC}"
./mvnw clean package -DskipTests -B

if [ $? -ne 0 ]; then
    echo -e "${RED}빌드 실패!${NC}"
    exit 1
fi

# JAR 파일 찾기 (fat JAR만, -plain 제외)
JAR_FILE=$(find target -name "*.jar" ! -name "*-plain.jar" | head -n 1)

if [ -z "$JAR_FILE" ]; then
    echo -e "${RED}JAR 파일을 찾을 수 없습니다. (target/ 확인)${NC}"
    exit 1
fi

echo -e "${GREEN}빌드 완료: $JAR_FILE${NC}"

# 2. SCP로 전송
echo -e "${YELLOW}[2/3] 서버로 파일 전송 중...${NC}"
scp "$JAR_FILE" "${REMOTE_HOST}:${REMOTE_PATH}/${JAR_NAME}"

if [ $? -ne 0 ]; then
    echo -e "${RED}파일 전송 실패!${NC}"
    exit 1
fi

echo -e "${GREEN}파일 전송 완료${NC}"

# 3. SSH로 Docker 배포
echo -e "${YELLOW}[3/3] Docker 서비스 배포 중...${NC}"
ssh "${REMOTE_HOST}" "cd ${REMOTE_PATH} && docker-compose up -d --build ${SERVICE_NAME}"

if [ $? -ne 0 ]; then
    echo -e "${RED}배포 실패!${NC}"
    exit 1
fi

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}배포 완료!${NC}"
echo -e "${GREEN}========================================${NC}"
