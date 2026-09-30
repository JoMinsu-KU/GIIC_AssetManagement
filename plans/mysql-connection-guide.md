# MySQL 접속 및 Workbench 연결 가이드

## 1. Docker Compose로 MySQL 실행

먼저 프로젝트 루트 디렉토리에서 다음 명령어로 MySQL 컨테이너를 실행합니다.

```bash
docker-compose -f docker-compose.dev.yml up -d
```

실행 확인:
```bash
docker ps
# asset_management_db 컨테이너가 실행 중인지 확인
```

---

## 2. MySQL Workbench 연결 설정

MySQL Workbench를 실행하고 **[+]** 버튼을 클릭하여 새 연결을 생성합니다.

### 연결 정보 입력

| 항목 | 값 | 비고 |
|------|----|------|
| **Connection Name** | `Asset Management (Local)` | 원하는 이름 입력 |
| **Connection Method** | `Standard (TCP/IP)` | 기본값 |
| **Hostname** | `127.0.0.1` | 로컬호스트 |
| **Port** | `3306` | docker-compose 포트 |
| **Username** | `root` | 또는 `asset_user` |
| **Password** | `Store in Vault...` 클릭 | 비밀번호 입력 |

### 사용할 계정 정보

**1. Root 계정 (모든 권한)**
- Username: `root`
- Password: `root_password`

**2. 애플리케이션용 계정 (일반 권한)**
- Username: `asset_user`
- Password: `asset_password`

---

## 3. 연결 테스트

1. `Test Connection` 버튼 클릭
2. "Successfully made the MySQL connection" 메시지가 뜨면 성공 ✅
3. `OK`를 눌러 저장 후 접속

## 4. 초기 데이터 확인

접속 후 `asset_management` 스키마를 선택하면 다음 테이블들을 확인할 수 있습니다:
- `users`: 초기 SUPER 관리자 계정(`admin`)이 생성되어 있습니다.
- `assets`
- `asset_images`
- `maintenance_history`
- `asset_transfer_history`

## 5. 문제 해결

**Q. 접속이 거부되나요?**
- Docker 컨테이너가 실행 중인지 확인하세요 (`docker ps`)
- 포트 3306이 이미 다른 MySQL에 의해 사용 중인지 확인하세요.
  - 사용 중이라면 `docker-compose.dev.yml`에서 포트를 `3307:3306` 등으로 변경하세요.

**Q. 한글이 깨지나요?**
- `status` 명령어 실행 시 `Server characterset: utf8mb4`인지 확인하세요.
- 기본 설정이 utf8mb4로 되어 있어 한글/이모지 모두 지원합니다.
