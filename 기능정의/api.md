좋아 👍
아래는 **방금 정리한 API 명세서를 그대로 담은 `.md` 파일용 콘텐츠**야.
👉 그대로 복사해서 `API_SPEC.md` 같은 이름으로 저장하면 돼.

(불필요한 설명 없이 **파일 내용만** 정리했어)

---

````markdown
# 자산관리 시스템 API 명세서

## 0. 공통 사항

### 인증
- 인증 방식: JWT
- Header: `Authorization: Bearer {token}`
- `status = APPROVED` 인 사용자만 로그인 및 API 접근 가능

### 권한(Role)
- USER / ADMIN / SUPER
- API별 접근 권한은 서버에서 Role 기반 검증

### 공통 응답 형식
```json
{
  "success": true,
  "data": {},
  "message": ""
}
````

### 공통 페이징/정렬 파라미터

목록 조회 API에서 사용되는 공통 파라미터입니다.

| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| page | int | 1 | 페이지 번호 (1부터 시작) |
| size | int | 20 | 페이지당 항목 수 (최대 100) |
| sort | string | created_at,desc | 정렬 필드,방향 (asc/desc) |

**사용 예시:**
```
GET /api/assets?page=1&size=20&sort=purchase_date,desc
GET /api/users?page=2&size=10&sort=name,asc
```

**응답 형식 (페이징 정보 포함):**
```json
{
  "content": [...],
  "page": 1,
  "size": 20,
  "total_elements": 150,
  "total_pages": 8,
  "first": true,
  "last": false
}
```

---

## 1. 인증 / 사용자 API

### 1.1 회원가입 요청

* **POST** `/api/auth/signup`
* 권한: Public

**Request**

```json
{
  "employee_no": "20231234",
  "name": "홍길동",
  "department": "경남지능화혁신사업단",
  "password": "plain_password"
}
```

**처리 내용**

* users.status = REQUESTED
* users.role = USER

---

### 1.2 로그인

* **POST** `/api/auth/login`
* 권한: Public

**Request**

```json
{
  "employee_no": "20231234",
  "password": "plain_password"
}
```

**Response**

```json
{
  "token": "JWT_TOKEN",
  "role": "USER"
}
```

---

### 1.3 내 정보 조회

* **GET** `/api/users/me`
* 권한: USER, ADMIN, SUPER

### 1.4 비밀번호 변경

* **PUT** `/api/users/me/password`
* 권한: USER, ADMIN, SUPER

**설명**

현재 로그인한 사용자의 비밀번호를 변경합니다

**Request**

```json
{
  "current_password": "string (필수)",
  "new_password": "string (필수, 8자 이상)"
}
```

**Response**

* 200 OK (성공 시)

**에러**

* 400: 현재 비밀번호 불일치
* 400: 새 비밀번호 정책 미충족

### 1.5 비밀번호 재설정 요청

* **POST** `/api/auth/reset-password`
* 권한: Public

**설명**

비밀번호 재설정 이메일을 발송합니다

**Request**

```json
{
  "email": "string (필수)"
}
```

**Response**

* 200 OK (이메일 발송 성공)

---

## 2. 회원 승인 / 사용자 관리 API

### 2.1 회원가입 요청 목록 조회

* **GET** `/api/users/requests`
* 권한: ADMIN, SUPER

---

### 2.2 회원 승인 / 반려

* **POST** `/api/users/{employee_no}/approve`
* 권한: ADMIN, SUPER

**Request**

```json
{
  "approved": true,
  "role": "USER"
}
```

---

### 2.3 사용자 목록 조회

* **GET** `/api/users`
* 권한: ADMIN, SUPER

**Query Parameters**

* status
* role
* department

---

### 2.4 사용자 정보 수정

* **PUT** `/api/users/{employee_no}`
* 권한: ADMIN, SUPER

**Request**

```json
{
  "department": "변경 부서",
  "role": "ADMIN",
  "status": "BLOCKED"
}
```

---

## 3. OCR API

### 3.1 OCR 실행

* **POST** `/api/assets/ocr`
* 권한: USER, ADMIN, SUPER

**Request**

* Content-Type: multipart/form-data
* image 파일 업로드

**Response**

```json
{
  "asset_no": "2501518",
  "name": "Macbook Pro 16",
  "spec": "355.7×248.1×16.8(mm)",
  "managing_dept": "경남지능화혁신사업단",
  "purchase_date": "2025-09-26",
  "raw_text": "..."
}
```

---

## 4. 자산 관리 API

### 4.1 자산 등록

* **POST** `/api/assets`
* 권한: USER, ADMIN, SUPER

**Request**

```json
{
  "asset_no": "2501518",
  "name": "Macbook Pro 16",
  "status": "정상",
  "purchase_date": "2025-09-26",
  "spec": "...",
  "managing_dept": "...",
  "location": "...",
  "value": 3000000,
  "current_user_id": "20231234"
}
```

---

### 4.2 자산 목록 조회

* **GET** `/api/assets`
* 권한: USER, ADMIN, SUPER

**Query Parameters**

* asset_no
* name
* status
* managing_dept
* location
* current_user_id

---

### 4.3 자산 상세 조회

* **GET** `/api/assets/{asset_id}`
* 권한: USER, ADMIN, SUPER

---

### 4.4 자산 수정

* **PUT** `/api/assets/{asset_id}`
* 권한: USER, ADMIN, SUPER

---

### 4.5 자산 이미지 업로드

* **POST** `/api/assets/{asset_id}/images`
* 권한: USER, ADMIN, SUPER

### 4.6 자산 불용 처리

* **PUT** `/api/assets/{id}/dispose`
* 권한: ADMIN, SUPER

**설명**

자산을 불용 처리합니다 (물리적 삭제 대신 상태 변경)

**Request**

```json
{
  "disposed_reason": "string (필수, 불용 사유)"
}
```

**Response**

```json
{
  "id": 1,
  "asset_no": "2501518",
  "status": "DISPOSED",
  "disposed_date": "2026-01-28",
  "disposed_reason": "노후화로 인한 폐기"
}
```

---

## 5. 유지/보수 API

### 5.1 유지/보수 등록

* **POST** `/api/assets/{asset_id}/maintenance`
* 권한: USER, ADMIN, SUPER

**Request**

```json
{
  "maintenance_date": "2026-01-10",
  "reason": "정기 점검",
  "details": "이상 없음",
  "cost": 0
}
```

---

### 5.2 유지/보수 목록 조회

* **GET** `/api/assets/{asset_id}/maintenance`
* 권한: USER, ADMIN, SUPER

---

## 6. SUPER 전용 모니터링 API

### 6.1 서버 헬스 체크

* **GET** `/monitoring/health`
* 권한: SUPER

---

### 6.2 API 성능 통계

* **GET** `/monitoring/metrics`
* 권한: SUPER

---

### 6.3 OCR 통계

* **GET** `/monitoring/ocr-stats`
* 권한: SUPER

---

### 6.4 에러 로그 요약

* **GET** `/monitoring/logs`
* 권한: SUPER

---

## 7. 에러 코드 규칙

| HTTP Code | 의미      |
| --------- | ------- |
| 400       | 요청 값 오류 |
| 401       | 인증 실패   |
| 403       | 권한 없음   |
| 404       | 리소스 없음  |
| 409       | 중복 데이터  |
| 500       | 서버 오류   |

---

## 8. 비고

* 자산번호(asset_no)는 중복 불가
* OCR 결과는 반드시 사용자 확인 후 저장
* 모니터링 API는 읽기 전용

```

---

## ✅ 다음 단계 안내

이제 이 파일을 기준으로 바로 다음이 가능해:

- Swagger(OpenAPI) 자동 생성
- 프론트 API 연동
- 백엔드 컨트롤러/라우터 구현
- QA 테스트 시나리오 작성

다음으로 원하면  
👉 **Swagger YAML**, **DTO 정의서**, **에러 케이스 정리** 중 하나 바로 만들어줄게.
```
