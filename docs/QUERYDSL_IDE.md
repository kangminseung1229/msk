# QueryDSL Q클래스 – IDE 컴파일 안 될 때

## 원인

- **Maven** 빌드(`./mvnw clean compile`)는 성공하지만, **IDE**(VS Code, Eclipse 등)에서만 QueryDSL Q클래스 컴파일이 실패하는 경우가 있습니다.
- IDE가 **Eclipse 스타일**로 `bin/`에 빌드하고, **Maven**은 `target/`을 사용합니다.
- Q클래스는 `target/generated-sources/annotations/`에 생성되며, IDE가 `bin/` 기준이면 서로 어긋납니다.

## 해결 방법

### 1. Maven으로 먼저 빌드

```bash
./mvnw clean compile
```

Q클래스가 `target/generated-sources/annotations/` 아래에 생성됩니다.

### 2. `bin/` 제거 후 프로젝트 갱신

- `bin/`은 Eclipse/IDE 출력 디렉터리입니다. **삭제**하고 Maven 결과만 사용하세요.

```bash
rm -rf bin
```

### 3. IDE에서 Maven 프로젝트 사용

- **VS Code**: `Java: Clean Java Language Server Workspace` 실행 후 창 새로고침.
- **Eclipse/STS**: 프로젝트 우클릭 → Maven → Update Project.
- **IntelliJ**: Maven 탭에서 Reload All Maven Projects.

이후 IDE는 `pom.xml` 기준으로 **target/** 출력과 `target/generated-sources/annotations` 소스 폴더를 사용합니다.

### 4. Q클래스 참조 시

- `bin/` 아래가 아니라 **`target/generated-sources/annotations/`** 아래 Q클래스를 사용하는지 확인하세요.
- `LawBasicInformationRepositoryExtensionImpl` 등에서 `QLawBasicInformation` 등을 참조할 때, 위 경로의 생성된 소스가 classpath에 포함되어 있어야 합니다.

## 요약

| 구분 | 출력/생성 위치 |
|------|----------------|
| Maven | `target/`, `target/generated-sources/annotations/` |
| IDE(Eclipse 스타일) | `bin/` (사용하지 말 것) |

**`./mvnw clean compile` → `bin/` 삭제 → IDE Maven 갱신** 순서로 진행하면 QueryDSL Q클래스 컴파일 이슈가 대부분 해결됩니다.
