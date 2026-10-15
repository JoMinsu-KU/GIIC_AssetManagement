# GIIC 자산관리 시스템 Docker 이전 패키지

이 패키지는 MySQL, Spring Boot, React/Nginx와 운영 데이터·사진을 새 워크스테이션에서 함께 실행합니다. 별도의 SQL 가져오기나 사진 복원 명령 없이 Docker Compose 한 번으로 초기화됩니다.

## 포함 내용

- `current-data/database.sql`: 운영 DB 전체 스냅샷
- `current-data/uploads.tar.gz`: 현재 참조되지 않는 파일까지 포함한 업로드 폴더 전체
- `current-data/upload-files.sha256`, `SHA256SUMS.txt`: 사진별·패키지별 무결성 검사값
- `current-data/seed-id.txt`: 정확한 DB 스냅샷 식별값
- `.env`: 기존 DB, JWT, 메일, Gemini 접속 설정. Git에는 저장되지 않음
- 상위 프로젝트 폴더: Docker 이미지 빌드에 필요한 백엔드·프론트엔드 소스

2026-10-01에 만든 스냅샷에는 자산 588건, 이미지 연결 261건, 업로드 파일 280개가 있습니다. 이미지 연결 261건 모두 실제 파일이 있음을 확인했고, 최신 행정지원팀 실사용자 반영도 포함했습니다. 소스 버전은 `current-data/source-commit.txt`에 기록했습니다.

## 새 워크스테이션 최초 실행

Docker Desktop을 설치하고 **패키지 전체**를 옮겨 압축을 풉니다. `docker-deployment`와 그 상위 프로젝트 폴더의 상대 위치를 유지하세요. 이미지·빌드 캐시·복원된 볼륨을 위해 여유 공간을 최소 8 GB 확보하는 것을 권장합니다.

PowerShell에서 압축 해제한 `docker-deployment` 폴더로 이동한 후:

```powershell
docker compose --env-file .env -f compose.yml up -d --build
docker compose --env-file .env -f compose.yml ps
```

브라우저에서 `http://localhost:3000`으로 접속합니다. 최초 실행 시 패키지 체크섬을 확인하고 **빈 DB 볼륨**에 SQL을 가져온 뒤, **빈 사진 볼륨**에 모든 파일을 복원·검증하고 애플리케이션을 시작합니다. 이미 데이터가 있는 볼륨을 자동으로 덮어쓰지 않습니다. 다른 스냅샷의 DB나 사진 볼륨이 감지되면 잘못된 데이터를 보여주는 대신 시작이 실패합니다.

초기화 로그 확인:

```powershell
docker compose --env-file .env -f compose.yml logs seed-check db-verify uploads-seed
```

프론트엔드는 Nginx의 `/api`, `/uploads`를 사용하므로 워크스테이션 IP가 달라져도 API 주소를 다시 빌드하지 않아도 됩니다. 패키지의 `APP_PUBLIC_URL`은 우선 `http://localhost:3000`입니다. **다른 PC에서 열 수 있는 이메일 링크를 보내기 전에는** `.env`의 `APP_PUBLIC_URL`을 새 PC의 IP 또는 도메인으로 바꾸고 백엔드를 재생성하세요. 외부 접속이 필요하면 Windows 방화벽의 TCP 3000을 허용 대상 네트워크에만 개방하세요. DB와 백엔드 포트는 기본적으로 `127.0.0.1`에만 바인딩됩니다.

## 실제 접속 IP 기록

Windows Docker Desktop의 공개 포트에서는 컨테이너가 Windows로 들어온 TCP 연결의 원래 IP 대신 Docker 게이트웨이 주소를 볼 수 있습니다. `scripts/Enable-RealClientIp.ps1`은 Windows에서 Caddy를 실행해 공개 포트 3000을 직접 받고, Docker Nginx를 `127.0.0.1:3001`로 옮깁니다. Caddy가 직접 연결한 상대 IP를 새 헤더로 설정하고, Nginx가 이를 백엔드로 전달합니다. 외부 요청에 실린 임의의 IP 헤더는 Caddy에서 덮어씁니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Enable-RealClientIp.ps1
```

스크립트는 공식 Caddy 릴리스의 SHA-512를 검증하고, 현재 사용자의 로그온 시 시작하는 `GIIC Asset Host Proxy` 작업을 등록합니다. 백엔드를 먼저 빌드한 뒤 `.env`를 백업하고 컨테이너를 재생성합니다. 중간에 실패하면 기존 `.env`와 공개 포트 설정을 복원합니다. 완료 후 `http://localhost:3000/healthz`가 `ok`를 반환해야 합니다. 로그온 세션이 없으면 이 사용자 작업도 실행되지 않으므로, 상시 운영 서버에서는 Windows 서비스로 등록하는 것이 좋습니다.

대시보드의 IP는 이 Windows PC에 **직접 연결한 장치의 주소**입니다. 앞단에 기관 프록시·VPN·NAT가 있으면 그 장치의 주소가 기록될 수 있으며, 별도의 신뢰 프록시 구성이 필요합니다. 이전에 게이트웨이 주소로 기록된 로그인 기록은 소급해서 복원할 수 없습니다.

## 데이터 안전

- 이 패키지는 실시간 복제가 아닌 **스냅샷**입니다. 내보낸 뒤 기존 서버에 새로 들어온 변경은 포함되지 않습니다. 최종 전환 직전에 기존 서버의 쓰기를 중단하고 스냅샷을 다시 준비해야 그 시점까지의 변경을 모두 옮길 수 있습니다.
- 새 PC의 데이터 볼륨을 삭제하는 `docker compose down -v`는 실행하지 마세요.
- 이 패키지는 빈 볼륨에서 자동 복원하므로 `Restore.ps1`을 실행하지 마세요.
- `.env`, `current-data`, 전달용 압축파일에는 비밀번호, 개인정보, 자산 사진이 있으므로 GitHub나 공개 클라우드에 올리지 말고 안전한 저장장치로 전달하세요.
- 새 PC의 건수와 사진을 확인할 때까지 기존 워크스테이션과 전달용 압축파일을 보관하세요.

## Windows 매일 백업

`scripts/Register-DailyBackup.ps1`은 현재 Windows 사용자 계정의 작업 스케줄러에 매일 실행 작업을 등록합니다. 이 워크스테이션은 `C:\Users\crazy\Desktop\Asset_DataBackup`에 매일 04:00 백업하도록 등록되어 있습니다. 등록 상태는 작업 스케줄러의 `GIIC Asset Daily Backup`에서 확인할 수 있습니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Register-DailyBackup.ps1 -DestinationDirectory 'C:\Users\crazy\Desktop\Asset_DataBackup' -Time '04:00'
```

`scripts/Backup-Scheduled.ps1`은 서비스를 중지하지 않고 **MySQL 데이터베이스 전체**와 `/app/uploads` 볼륨 전체를 `Asset_DataBackup\YYYY-MM-DD\GIIC_Asset_YYYY-MM-DD_HH-mm-ss.tar.gz`에 저장합니다. 실행일별 폴더를 자동 생성합니다. 압축파일에는 `database.sql`, `uploads.tar.gz`, `SHA256SUMS.txt`, `manifest.json`이 들어갑니다. 백업된 DB의 자산 이미지 참조가 압축된 파일에 모두 존재하는지 확인하고, 누락되면 최대 세 번 재시도합니다. 백업 로그와 최종 압축파일의 SHA-256 파일도 해당 날짜 폴더에 저장합니다. 수동으로 실행하려면:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Backup-Scheduled.ps1 -DestinationDirectory 'C:\Users\crazy\Desktop\Asset_DataBackup'
```

온라인 백업에서는 DB 덤프를 트랜잭션 시점으로 고정하지만, 이미지 볼륨은 그 뒤에 압축합니다. DB가 참조하는 사진의 포함 여부는 검증하며, 백업 중 추가된 파일이 압축파일에 더 들어갈 수 있습니다. Windows 사용자 세션이 열려 있고 Docker Desktop이 실행 중이어야 작업이 수행됩니다. PC가 꺼져 있거나 사용자가 로그아웃한 동안에는 예정 시각에 실행할 수 없습니다. 작업은 가능한 시점에 다시 시작하도록 설정되어 있습니다.

기존 백업을 자동으로 삭제하지 않습니다. 현재 백업 크기는 약 638 MB이므로 30일이면 약 19 GB를 사용합니다. 압축파일에는 개인정보와 자산 사진이 포함되므로 저장 폴더의 접근 권한을 제한하세요. 기존 `Restore.ps1`은 이 이전 패키지에서 수동 복원을 막도록 되어 있으므로, 아래의 빈 볼륨 전용 스크립트를 사용하세요.

### 날짜별 백업에서 빈 Docker 볼륨 복구

`Start.ps1`만 실행하면 `current-data`의 최초 이전 스냅샷이 복원됩니다. 최신 날짜별 백업을 사용하려면 **`Start.ps1`보다 먼저** `Restore-BackupToEmpty.ps1`을 실행하세요. 이 스크립트는 DB와 이미지 볼륨이 모두 없는 경우에만 새 볼륨을 만들고, 백업 압축파일의 체크섬을 검사한 뒤 전체 DB와 이미지를 복원하고 앱을 시작합니다. 기존 볼륨이나 컨테이너가 있으면 중단하며 운영 데이터를 덮어쓰지 않습니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Restore-BackupToEmpty.ps1 -BackupArchive 'C:\Users\crazy\Desktop\Asset_DataBackup\2026-10-01\GIIC_Asset_2026-10-01_14-53-21.tar.gz'
```

복구 시 Docker Desktop, 이 프로젝트의 전체 소스와 `docker-deployment\compose.yml`, `current-data`, 운영 `.env`가 필요합니다. 날짜별 압축파일에는 DB와 업로드 파일이 들어가며 소스와 `.env`는 들어가지 않습니다. 별도 임시 Docker 프로젝트에서 압축파일을 복원하고 DB·사진 건수, 컨테이너 헬스체크, HTTP 응답을 시험했습니다. 실제 운영 볼륨에는 복구를 실행하지 않았습니다.

데이터를 보존한 채 중지·재시작하려면:

```powershell
docker compose --env-file .env -f compose.yml down
docker compose --env-file .env -f compose.yml up -d
```
