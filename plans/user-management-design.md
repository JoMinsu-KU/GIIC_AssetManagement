# 사용자 관리 시스템 설계 (User Management System Design)

> **Implementation Status: Complete**
> 본 문서는 실제 구현된 서버 코드를 기준으로 동기화되었습니다.

## 1. 시스템 개요 (System Analysis)

### 구현 상태 (Current State)
- **Entities**: 
  - `User`: `employeeNo`, `password`, `name`, `department`, `email`, `role`, `status` 등을 포함.
  - `Role`: `USER`, `ADMIN`, `SUPER`
  - `UserStatus`: `PENDING` (승인 대기), `APPROVED` (승인됨/활성), `REJECTED` (반려됨), `INACTIVE` (비활성화)
- **API**:
  - `UserController`: 사용자 목록 조회 (권한에 따른 필터링 포함) 및 승인/반려/권한변경/비활성화 처리.
- **Frontend**:
  - API Client: `users.js`에 관련 API 호출 구현 완료.
  - UI: `UserListPage`에서 조건부 렌더링을 통한 사용자 상태 관리 UI 구현.

### 핵심 비즈니스 로직 (Key Logic)
1.  **목록 조회 시 Role-based Filtering**:
    - `USER`: 접근 불가.
    - `ADMIN`: `SUPER` 권한을 가진 사용자를 제외한 전체 목록 조회.
    - `SUPER`: 모든 사용자 조회 가능.
2.  **권한 관리 (Manager Action Validation)**:
    - **불변 원칙**: `SUPER` 관리자의 상태나 권한은 어떠한 경우에도 변경할 수 없음.
    - `ADMIN` 관리자는 `ADMIN` 또는 `SUPER` 권한을 가진 사용자를 수정/관리할 수 없음 (오직 `USER` 대상만 가능).
    - `SUPER` 관리자는 `USER`와 `ADMIN` 대상을 수정/관리할 수 있음.

## 2. API 명세 (API Design)

### Endpoints (`/api/users`)

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/api/users` | 사용자 목록 페이징 및 필터링 조회 | ADMIN, SUPER |
| PUT | `/api/users/{employeeNo}/approve` | 가입 승인 (상태 -> APPROVED) + 알림 발송 | ADMIN, SUPER |
| PUT | `/api/users/{employeeNo}/reject` | 가입 반려 (상태 -> REJECTED) + 알림 발송 | ADMIN, SUPER |
| PUT | `/api/users/{employeeNo}/role` | 사용자 권한(Role) 변경 | ADMIN, SUPER |
| PUT | `/api/users/{employeeNo}/deactivate` | 사용자 비활성화 (상태 -> INACTIVE) | ADMIN, SUPER |
| PUT | `/api/users/{employeeNo}/status` | 상태 및 권한 동시 변경 (Legacy/General 용도) | ADMIN, SUPER |
| GET | `/api/users/me` | 현재 로그인된 사용자의 내 정보 조회 | Authenticated |

### 요청/응답 형식 (Request/Response Formats)

**권한 변경 요청 (`UserRoleUpdateRequest`)**
```json
{
  "role": "ADMIN" // USER, ADMIN, SUPER
}
```

**상태/권한 동시 변경 요청 (`UserStatusUpdateRequest`)**
```json
{
  "status": "APPROVED", // PENDING, APPROVED, REJECTED, INACTIVE
  "role": "ADMIN"       
}
```

## 3. 프론트엔드 설계 (Frontend Design)

### API Client (`src/api/users.js`)
- `getUsers(params)`: 상태(status), 권한(role), 검색어(keyword) 필터링을 포함한 목록 조회.
- `approveUser(employeeNo)`: 승인 요청.
- `rejectUser(employeeNo)`: 반려 요청.
- `updateUserRole(employeeNo, role)`: 권한 수정.
- `deactivateUser(employeeNo)`: 계정 비활성화.
- `getCurrentUser()`: 내 정보 조회.

### User List Page (`src/pages/UserListPage.js`)
- **Route**: `/users` (ProtectedRoute 적용, ADMIN 이상 접근).
- **구성 요소**:
  - **Filter Bar**: 
    - 상태 드롭다운 (전체, 대기, 활성, 거절, 비활성)
    - 권한 드롭다운 (전체, 일반, 관리자, 최고관리자)
    - 검색창 (이름/사번 검색)
  - **Data Table**:
    - 칼럼: 사번, 이름, 부서, 이메일, 권한, 상태, 액션
    - 상태 뱃지 시각화: 대기(노란색), 활성(초록색), 반려/비활성(회색/빨간색)
  - **Actions (버튼)**:
    - **승인 대기 (`PENDING`)**: [승인], [거절]
    - **활성 상태 (`APPROVED`)**: 
      - 권한에 따라 [권한 변경] 드롭다운/모달 및 [비활성화] 버튼 노출.
      - 본인 계정이나 상위 권한자(SUPER)인 경우 액션 버튼 비활성화 (또는 숨김).

## 4. 접근 권한 매트릭스 (Access Control Matrix)

| Actor | Action | Target | Allowed? | Logic Validation (UserService) |
|-------|--------|--------|----------|-------|
| USER | 목록 조회 | N/A | NO | 403 Forbidden |
| ADMIN | 목록 조회 | 전체 사용자 | YES | (단, `SUPER` 계정은 목록에서 제외됨) |
| SUPER | 목록 조회 | 전체 사용자 | YES | 모든 사용자 노출 |
| ADMIN | 승인/반려/수정 | PENDING / USER | YES | 정상 수행됨 |
| ADMIN | 승인/반려/수정 | ADMIN / SUPER | NO | 예외: "ADMIN은 ADMIN 이상의 사용자를 관리할 수 없습니다." |
| SUPER | 승인/반려/수정 | USER / ADMIN | YES | 정상 수행됨 |
| SUPER | 권한/상태 수정 | SUPER | NO | 예외: "SUPER 관리자는 변경할 수 없습니다." |
