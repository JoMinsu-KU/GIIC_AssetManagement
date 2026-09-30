# 자산관리 시스템 API 명세서

> **Implementation Status: Complete**
> 본 문서는 실제 구현된 서버 코드를 기준으로 동기화되었습니다.

## 1. 개요 (Overview)

본 문서는 자산관리 시스템의 RESTful API 명세서입니다.

*   **기본 URL**: `/api`
*   **데이터 형식**: JSON (application/json)
*   **인코딩**: UTF-8
*   **날짜 형식**: `YYYY-MM-DD` (날짜), `YYYY-MM-DD HH:mm:ss` (일시)
*   **인증 방식**: JWT (Access Token, Refresh Token)

---

## 2. 공통 사항 (Common)

### 2.1 공통 응답 형식 (Response Format)

모든 API 응답은 `ApiResponse<T>` 형식을 따릅니다.

**성공 시 (200 OK)**
```json
{
  "success": true,
  "data": { ... }, 
  "message": "요청이 성공적으로 처리되었습니다."
}
```

**실패 시 (Error)**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "E001",
    "message": "잘못된 요청입니다."
  }
}
```

---

## 3. 인증 API (Authentication: `/api/auth`)

*   `POST /api/auth/login`: 로그인 (Access/Refresh Token 발급)
*   `POST /api/auth/refresh`: 토큰 갱신
*   `POST /api/auth/logout`: 로그아웃
*   `POST /api/auth/signup`: 회원가입

---

## 4. 사용자 관리 API (User Management: `/api/users`)

*   `GET /api/users`: 사용자 목록 조회 (권한: ADMIN, SUPER)
*   `PUT /api/users/{employeeNo}/status`: 사용자 상태 변경 (APPROVED, REJECTED, BLOCKED)
*   `PUT /api/users/{employeeNo}/approve`: 가입 승인
*   `PUT /api/users/{employeeNo}/reject`: 가입 반려
*   `PUT /api/users/{employeeNo}/role`: 권한 수정
*   `PUT /api/users/{employeeNo}/deactivate`: 비활성화 처리
*   `GET /api/users/me`: 내 정보 조회

---

## 5. 자산 관리 API (Asset Management: `/api/assets`)

*   `POST /api/assets`: 자산 등록
*   `GET /api/assets`: 자산 목록 조회 (Pagination)
*   `GET /api/assets/{assetId}`: 자산 상세 조회
*   `PUT /api/assets/{assetId}`: 자산 정보 수정
*   `DELETE /api/assets/{assetId}`: 자산 삭제
*   `POST /api/assets/{assetId}/images`: 이미지 업로드 (multipart/form-data)
*   `POST /api/assets/ocr`: OCR 자산 정보 추출 (multipart/form-data)
*   `GET /api/assets/images/{filename}`: 이미지 파일 조회 (Resource 응답)

**(하위 호환을 위한 자산 폐기 라우팅 - 상세 폐기는 Disposal API 참고)**
*   `POST /api/assets/{assetId}/disposal`: 폐기 요청
*   `PUT /api/assets/{assetId}/disposal/approve`: 폐기 승인
*   `PUT /api/assets/{assetId}/disposal/reject`: 폐기 반려

---

## 6. 자산 이관 API (Asset Transfer: `/api/transfers`)

*   `POST /api/transfers`: 자산 이관 요청 (현재 사용자 -> 타 사용자)
*   `GET /api/transfers/my`: 내 이관 내역 조회
*   `GET /api/transfers/all`: 모든 이관 내역 조회 (권한: ADMIN, SUPER)
*   `GET /api/transfers`: 이관 목록 페이징 조회
*   `PUT /api/transfers/{id}/approve`: 이관 승인 (권한: ADMIN, SUPER)
*   `PUT /api/transfers/{id}/reject`: 이관 반려 (반려 사유 포함) (권한: ADMIN, SUPER)

---

## 7. 유지보수 API (Maintenance: `/api/maintenances`)

*   `GET /api/maintenances`: 유지보수 내역 조회 (권한 기반: 일반 사용자는 본인/부서 관련, SUPER는 전체)
*   `POST /api/maintenances`: 유지보수 요청 (자산 상태: IN_MAINTENANCE 변경)
*   `PUT /api/maintenances/{id}/approve`: 유지보수 승인 (권한: ADMIN, SUPER)
*   `PUT /api/maintenances/{id}/complete`: 유지보수 완료 처리 (권한: ADMIN, SUPER)
*   `PUT /api/maintenances/{id}/reject`: 유지보수 반려 (권한: ADMIN, SUPER)

---

## 8. 폐기 관리 API (Disposal: `/api/disposals`)

*   `GET /api/disposals`: 폐기 요청 내역 조회 (권한 기반)
*   `POST /api/disposals`: 폐기 요청
*   `PUT /api/disposals/{id}/approve`: 폐기 승인 (권한: ADMIN, SUPER)
*   `PUT /api/disposals/{id}/reject`: 폐기 반려 (권한: ADMIN, SUPER)

---

## 9. 알림 API (Notification: `/api/notifications`)

*   `GET /api/notifications`: 내 알림 목록 조회
*   `GET /api/notifications/unread-count`: 읽지 않은 알림 개수 조회
*   `PUT /api/notifications/{id}/read`: 특정 알림 읽음 처리
*   `PUT /api/notifications/read-all`: 모든 알림 읽음 처리

---

## 10. 에러 코드 (Error Codes)

| HTTP Status | Error Code | 설명 |
|---|---|---|
| 400 | E400_BAD_REQUEST | 잘못된 요청 파라미터 |
| 401 | E401_UNAUTHORIZED | 인증 실패 (토큰 만료/없음) |
| 403 | E403_FORBIDDEN | 접근 권한 없음 |
| 404 | E404_NOT_FOUND | 리소스를 찾을 수 없음 |
| 409 | E409_DUPLICATE | 데이터 중복 |
| 500 | E500_INTERNAL_SERVER_ERROR | 서버 내부 오류 |
