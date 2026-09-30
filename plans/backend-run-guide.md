# 백엔드 서버 실행 가이드

## 1. 사전 준비

### 1.1 DB 실행 확인
먼저 Docker Desktop에서 MySQL 컨테이너가 실행 중인지 확인합니다.

```bash
docker ps
# asset_management_db 컨테이너가 Up 상태여야 함
```

만약 실행 중이 아니라면:
```bash
docker-compose -f docker-compose.dev.yml up -d
```

### 1.2 Gradle Wrapper 설치
프로젝트에 Gradle Wrapper가 없다면 설치해야 합니다. (최초 1회)

```bash
cd backend
gradle wrapper
```

---

## 2. 서버 실행 방법

### 방법 A: Gradle 명령어로 실행 (권장)

터미널에서 `backend` 디렉토리로 이동 후 다음 명령어를 실행합니다.

**Windows (PowerShell/CMD):**
```powershell
cd backend
./gradlew.bat bootRun
```

**Linux/Mac:**
```bash
cd backend
./gradlew bootRun
```

### 방법 B: IDE (IntelliJ/VS Code)에서 실행

1. **VS Code**: `AssetManagementApplication.java` 파일을 열고 `Run` 버튼 클릭 (Java Extension Pack 필요)
2. **IntelliJ**: 프로젝트를 열고 `AssetManagementApplication` 클래스의 `main` 메소드 실행

---

## 3. 실행 확인

서버가 정상적으로 실행되면 터미널에 다음과 같은 로그가 출력됩니다:

```
... Started AssetManagementApplication in 3.456 seconds (JVM running for 4.123)
```

**테스트:**
브라우저나 Postman으로 접속 확인:
`http://localhost:8080/api/auth/login` (403 Forbidden 또는 401 Unauthorized 응답이면 서버 정상 작동 중)

---

## 4. 문제 해결

**Q. "Port 8080 was already in use" 에러 발생**
- 이미 다른 프로세스가 8080 포트를 사용 중입니다.
- 해결: `netstat -ano | findstr :8080` (Windows) 으로 PID 확인 후 종료하거나, `application.yml`에서 포트를 변경하세요.

**Q. DB 연결 실패**
- Docker 컨테이너가 실행 중인지 확인하세요.
- `application.yml`의 DB 설정(username, password)이 `docker-compose.dev.yml`과 일치하는지 확인하세요.
