# GIIC Asset Management

경남지능화혁신사업단 자산관리 시스템입니다. Spring Boot 백엔드와 React 프론트엔드, MySQL 데이터베이스로 구성됩니다.

## Requirements

- Java 17
- Node.js 18 이상
- Docker Desktop

## Environment

루트의 `.env.example`을 `.env`로 복사한 뒤 운영 환경에 맞는 값을 설정합니다. 실제 비밀번호, JWT Secret, Gmail 앱 비밀번호와 API Key는 Git에 커밋하지 않습니다.

새 워크스테이션에서는 다음 항목을 별도로 복원해야 합니다.

- MySQL 논리 백업 파일
- `uploads` 디렉터리
- 운영용 `.env`

## Run

MySQL을 실행합니다.

```powershell
docker compose up -d db
```

백엔드를 실행합니다.

```powershell
.\gradlew.bat bootRun
```

프론트엔드를 실행합니다.

```powershell
cd src\main\giic_frontend
npm ci
npm start
```

기본 포트는 프론트엔드 `3000`, 백엔드 `9090`, 로컬 MySQL 연결 `3907`입니다.

## Verification

```powershell
.\gradlew.bat test
cd src\main\giic_frontend
npm test -- --watchAll=false
npm run build
```

## Security

- `.env`, DB 백업, 업로드 이미지와 자산 원장은 저장소에서 제외됩니다.
- 초기 SUPER 계정은 공개된 기본 비밀번호로 생성하지 않습니다. 최초 운영 계정은 승인된 관리 절차를 통해 별도로 생성합니다.
- 외부 서비스 공개 시에는 Nginx 같은 리버스 프록시와 HTTPS 사용을 권장합니다.
