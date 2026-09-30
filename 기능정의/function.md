# 자산관리 시스템 개발 명세서

## 1. 프로젝트 개요

본 시스템은 사업단 내부 자산을 전산으로 관리하기 위한 웹/모바일 기반 자산관리 시스템이다.  
스마트폰을 이용해 자산 라벨을 촬영하고 OCR을 통해 자동으로 정보를 입력한 후,
사용자가 보완 입력하여 자산을 등록·조회·관리할 수 있도록 한다.

---

## 2. 사용자 역할(Role) 정의

### USER
- 자산 등록 / 조회 / 수정 가능
- 유지·보수 등록 및 조회 가능
- 회원 승인 및 사용자 관리 불가
- 개발/서버 모니터링 접근 불가

### ADMIN
- USER의 모든 권한 포함
- 회원 승인 가능
- 사용자 관리 가능
- 개발/서버 모니터링 접근 불가

### SUPER
- ADMIN의 모든 권한 포함
- 서버 상태, OCR 상태, 로그 등 개발/운영 모니터링 가능
- 읽기 전용 모니터링 권한

---

## 3. 전체 아키텍처 개요

- Web / Mobile Client (Flutter / React / Web)
- Backend API Server (단일 서버, 논리 분리)
  - 업무 API 영역 (/api/**)
  - 모니터링 영역 (/monitoring/**)
- RDBMS (자산/사용자 데이터)
- Object Storage (이미지 저장)
- External OCR API (Cloud OCR)

운영 서버와 모니터링 서버는 **물리적으로 분리하지 않고**
URL 경로 및 권한 기반으로 논리 분리한다.

---

## 4. DB 테이블 설계

### 4.1 users (사용자 계정)

| 컬럼 | 필수 | 설명 |
|---|---|---|
| employee_no (PK) | O | 사번 (로그인 ID) |
| password | O | 비밀번호 해시 |
| name | O | 사용자명 |
| department | X | 소속 부서 |
| user_type | X | 직원/교수/외부 |
| email | X | 이메일 |
| phone | X | 연락처 |
| role | O | USER / ADMIN / SUPER |
| status | O | REQUESTED / APPROVED / BLOCKED |
| created_at | O | 생성일 |
| updated_at | O | 수정일 |

---

### 4.2 assets (자산 메인)

| 컬럼 | 필수 | OCR | 설명 |
|---|---|---|---|
| id (PK) | O | X | 내부 식별자 |
| asset_no | O | O | 자산번호 |
| old_asset_no | X | X | 구자산번호 |
| name | O | O | 물품명 |
| model | X | X | 모델명 |
| serial_no | X | X | 제조번호 |
| spec | X | O | 규격 |
| value | X | X | 자산금액 |
| purchase_date | O | O | 취득일자 |
| disposed_date | X | X | 불용일자 |
| status | O | X | 자산상태 |
| business_unit | X | X | 사업장명 |
| managing_dept | X | O | 관리부서 |
| location | X | X | 비치장소 |
| current_user_id | X | X | 현재 사용자 (FK) |
| assigned_at | X | X | 배정일자 |
| created_at | O | X | 생성일 |
| updated_at | O | X | 수정일 |

---

### 4.3 asset_images (자산 이미지)

| 컬럼 | 필수 | 설명 |
|---|---|---|
| id (PK) | O | |
| asset_id (FK) | O | 자산 ID |
| image_url | O | 이미지 URL |
| is_primary | X | 대표 이미지 여부 |
| uploaded_at | O | 업로드 일시 |

---

### 4.4 maintenance_history (유지/보수 이력)

| 컬럼 | 필수 | 설명 |
|---|---|---|
| id (PK) | O | |
| asset_id (FK) | O | 자산 ID |
| maintenance_date | O | 유지/보수 일자 |
| reason | O | 사유 |
| details | X | 상세 내용 |
| cost | X | 비용 |
| performed_by | X | 수행자 |
| created_at | O | 생성일 |
| updated_at | O | 수정일 |

---

## 5. 자산 등록 흐름 (OCR 기반)

1. 자산 등록 버튼 클릭
2. 라벨 이미지 촬영 또는 업로드
3. 서버에서 OCR 실행
4. OCR 결과를 자산 등록 폼에 자동 채움
5. 사용자가 나머지 필드 입력/수정
6. 서버에서 필수값 및 무결성 검증
7. 자산 등록 완료

### OCR 자동 채움 대상
- 자산번호
- 물품명
- 규격
- 관리부서
- 취득일자

---

## 6. 권한별 기능 요약

| 기능 | USER | ADMIN | SUPER |
|---|---|---|---|
| 로그인 | O | O | O |
| 회원가입 요청 | O | O | O |
| 회원 승인 | X | O | O |
| 사용자 관리 | X | O | O |
| 자산 등록 | O | O | O |
| 자산 수정 | O | O | O |
| 자산 조회 | O | O | O |
| 유지보수 등록 | O | O | O |
| 서버/개발 모니터링 | X | X | O |

---

## 7. SUPER 전용 모니터링 기능

- 서버 헬스 체크 (UP/DOWN)
- DB 연결 상태
- API 응답 시간
- OCR 성공/실패 통계
- 최근 에러 로그 요약
- 사용자 활동 요약

※ 모든 모니터링 기능은 읽기 전용

---

## 8. 보안 및 운영 원칙

- 비밀번호는 반드시 해시 저장
- APPROVED 상태 사용자만 로그인 가능
- SUPER 권한 최소화
- 모니터링 API는 SUPER 전용 경로 사용
- 운영 데이터는 물리 삭제하지 않음

---

## 9. 확장 고려 사항 (향후)

- 자산 이관 이력 테이블
- 감사 로그(Audit Log)
- OCR 결과 저장 테이블
- 외부 알림(Slack/Email) 연동
