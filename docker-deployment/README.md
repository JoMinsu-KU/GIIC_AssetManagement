# GIIC Asset Management Docker Deployment

이 폴더는 기존 애플리케이션 소스를 수정하지 않고 MySQL, Spring Boot, React/Nginx를 Docker Compose로 실행하는 배포 패키지입니다.

## 구성

- `db`: MySQL 8.0, 내부 데이터 영구 볼륨
- `backend`: Java 17 기반 Spring Boot, 업로드 이미지 영구 볼륨
- `frontend`: React 정적 빌드와 Nginx
- Nginx가 `/api`와 `/uploads` 요청을 백엔드로 전달하므로 워크스테이션 IP가 달라져도 프론트엔드 API 주소를 다시 빌드할 필요가 없습니다.

## 새 워크스테이션에서 최초 실행

필수 프로그램은 Git과 Docker Desktop입니다. Java와 Node.js는 호스트에 설치하지 않아도 됩니다.

```powershell
git clone https://github.com/JoMinsu-KU/GIIC_AssetManagement.git
cd GIIC_AssetManagement\docker-deployment
.\scripts\Initialize-Environment.ps1
notepad .env
.\scripts\Start.ps1
```

브라우저에서 `http://localhost:3000`으로 접속합니다. 외부 PC에서는 `http://워크스테이션IP:3000`으로 접속하며 Windows 방화벽에서 TCP 3000 포트를 허용해야 합니다.

메일 링크를 외부에서도 열어야 하면 `.env`의 값을 실제 IP 또는 도메인으로 변경합니다.

```dotenv
APP_PUBLIC_URL=http://203.0.113.10:3000
APP_CORS_ALLOWED_ORIGINS=http://203.0.113.10:3000
```

프론트엔드는 동일 출처의 `/api`를 사용하므로 일반 API 호출에는 CORS가 발생하지 않습니다. HTTPS를 구성하면 `APP_COOKIE_SECURE=true`로 변경합니다.

## 기존 운영 데이터 이전

현재 워크스테이션에서 기존 MySQL 컨테이너와 `uploads` 폴더를 내보냅니다.

```powershell
cd C:\Users\choiLee\GIIC_Asset\docker-deployment
.\scripts\Export-Current-Data.ps1
```

새 워크스테이션에서 저장소를 받은 다음, 현재 워크스테이션의 `docker-deployment` 폴더를 새 저장소의 같은 위치에 덮어씁니다. 그러면 `backups\legacy-날짜시간`과 운영용 `.env`가 함께 전달됩니다. 이 두 항목은 Git에는 올라가지 않습니다.

현재 로컬 이전 패키지에는 운영용 `.env`가 포함되어 있습니다. `.env`에는 DB 비밀번호, JWT 키, Gmail 앱 비밀번호, Gemini API 키가 포함되므로 `docker-deployment` 폴더 전체를 암호화된 저장장치나 신뢰할 수 있는 내부 경로로만 전달해야 합니다. `.env`와 `backups`는 Git에서 제외됩니다.

새 워크스테이션에서 초기 환경을 생성한 뒤 백업을 복원합니다.

```powershell
.\scripts\Initialize-Environment.ps1
.\scripts\Restore.ps1 -BackupDirectory .\backups\legacy-날짜시간 -ConfirmRestore
.\scripts\Start.ps1
```

복원은 대상 DB 내용을 변경하므로 새 워크스테이션 또는 복원이 필요한 환경에서만 수행합니다. 업로드 파일은 빈 볼륨에 복원하는 것을 권장합니다.
백업에 포함된 `SHA256SUMS.txt`는 복원 전에 DB 덤프와 이미지 압축파일이 손상되지 않았는지 자동으로 검증하는 데 사용됩니다.

## 운영 명령

```powershell
# 시작 또는 새 소스 반영
.\scripts\Start.ps1

# 이미지 재빌드 없이 시작
.\scripts\Start.ps1 -SkipBuild

# DB와 업로드 이미지 백업
.\scripts\Backup.ps1

# 정지: 데이터 볼륨은 유지됨
.\scripts\Stop.ps1

# 상태 확인
docker compose --env-file .env -f compose.yml ps

# 로그 확인
docker compose --env-file .env -f compose.yml logs -f
```

`docker compose down -v`는 DB와 업로드 이미지 볼륨을 삭제하므로 운영 환경에서는 사용하지 않습니다.

## 보안 및 배포 확인

- `.env`와 `backups`는 이 폴더의 `.gitignore`에 의해 Git에서 제외됩니다.
- `MYSQL_ROOT_PASSWORD`, `MYSQL_PASSWORD`, `JWT_SECRET`은 초기화 스크립트가 무작위로 생성합니다.
- Gmail 앱 비밀번호와 Gemini API 키는 `.env`에만 보관합니다.
- 외부 공개 운영 시에는 방화벽 허용 범위를 제한하고 HTTPS 리버스 프록시를 추가하는 것을 권장합니다.
- DB와 백엔드 포트는 기본적으로 `127.0.0.1`에만 열리고, 외부에는 프론트엔드 포트만 노출됩니다.
