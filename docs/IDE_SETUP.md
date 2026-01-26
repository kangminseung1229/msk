# IDE에서 Spring Boot / Maven 인식 안 될 때

## 현상

- `org.springframework.boot.SpringApplication` 등 import에 빨간 밑줄
- "cannot resolve symbol" / "패키지 org.springframework.boot을(를) 찾을 수 없습니다"

## 원인

IDE( VS Code / Cursor )의 Java 확장이 **Maven 프로젝트**를 제대로 로드하지 못한 경우입니다.

## 해결 방법

### 1. 워크스페이스 루트 확인

**반드시 `pom.xml`이 있는 폴더(aiagent)를 루트로 열어야 합니다.**

- ✅ `파일 > 폴더 열기` → `aiagent` 선택
- ❌ 상위 폴더(LangGraph4j 등)만 열면 Maven 프로젝트로 인식되지 않을 수 있음

### 2. Java 확장 설치

- **Extension Pack for Java** (`vscjava.vscode-java-pack`) 설치
- **Maven for Java** (`vscjava.vscode-maven`) 설치

확장 설치 후 창 새로고침.

### 3. Java Language Server 워크스페이스 초기화

1. **Cmd+Shift+P** (Mac) / **Ctrl+Shift+P** (Windows)
2. **`Java: Clean Java Language Server Workspace`** 실행
3. **"Reload and delete"** 선택 후 재시작

이후 Maven이 `pom.xml` 기준으로 의존성/클래스패스를 다시 로드합니다.

### 4. Maven 프로젝트 수동 로드

1. **Cmd+Shift+P** → **`Java: Force Java Compilation`** → **Full**
2. 또는 **Maven** 사이드바에서 해당 프로젝트 **Reload** (새로고침 아이콘)

### 5. `.vscode/settings.json` 확인

다음 설정이 있으면 Maven 인식에 유리합니다.

```json
{
  "java.configuration.updateBuildConfiguration": "automatic",
  "java.import.maven.enabled": true,
  "maven.executable.path": "${workspaceFolder}/mvnw"
}
```

### 6. JDK 17 사용 확인

- **Cmd+Shift+P** → **`Java: Configure Java Runtime`**
- **Java 17**이 등록되어 있고, 프로젝트에 17이 선택되는지 확인

---

## 요약

1. **aiagent** 폴더를 워크스페이스 루트로 열기  
2. **Java Extension Pack** + **Maven for Java** 설치  
3. **Java: Clean Java Language Server Workspace** → Reload and delete  
4. 필요 시 **Maven Reload** / **Force Java Compilation**

이후에도 Spring Boot import가 안 잡히면, 터미널에서 `./mvnw compile` 이 성공하는지 확인한 뒤 **Cursor/VS Code 완전 종료 후 재실행**해 보세요.
