# 유지보수 및 폐기 관리 기능 상세 설계

> **Implementation Status: Complete**
> 본 문서는 실제 구현된 서버 코드를 기준으로 동기화되었습니다.

## 1. 개요
본 문서는 자산 관리 시스템의 유지보수(수리) 및 폐기 관리 기능을 위한 상세 설계 문서입니다. 수리 요청/승인 프로세스와 폐기 신청/승인/거절 프로세스가 구현되어 자산의 생애 주기를 체계적으로 관리합니다.

## 2. 상태 관리 설계 (Status Management)

### 2.1 AssetStatus (자산 상태)
자산(`Asset`)의 상태 관리를 위해 다음과 같이 세분화되어 적용되었습니다.

```java
public enum AssetStatus {
    NORMAL,             // 정상/사용중
    ABNORMAL,           // 비정상/고장
    DISPOSED,           // 폐기됨 (승인 완료)
    REPAIRING,          // 수리중 (유지보수 승인됨)
    TRANSFER_PENDING,   // 이관 대기중
    DISPOSAL_PENDING,   // 폐기 신청됨 (승인 대기)
    DISPOSAL_REJECTED   // 폐기 거절됨
}
```

### 2.2 MaintenanceStatus (유지보수 상태)
유지보수(`Maintenance` 테이블) 요청의 진행 상태를 관리합니다.

```java
public enum MaintenanceStatus {
    PENDING,        // 대기중 (요청됨)
    IN_PROGRESS,    // 진행중 (승인됨 -> 자산 상태는 REPAIRING)
    COMPLETED,      // 완료됨 (완료 처리 -> 자산 상태는 NORMAL 복귀)
    REJECTED        // 거절됨 (반려 처리)
}
```

### 2.3 DisposalStatus (폐기 상태)
폐기(`Disposal` 테이블) 요청의 진행 상태를 관리합니다.

```java
public enum DisposalStatus {
    PENDING,        // 대기중 (요청됨 -> 자산 상태는 DISPOSAL_PENDING)
    APPROVED,       // 승인됨 (승인 처리 -> 자산 상태는 DISPOSED)
    REJECTED        // 거절됨 (반려 처리 -> 자산 상태는 DISPOSAL_REJECTED)
}
```

## 3. 엔티티 구조 설계 (Entity Design)

### 3.1 Maintenance (유지보수 엔티티)
별도의 `maintenance` 테이블에 데이터가 저장되며 유지보수의 신청부터 완료까지의 모든 기록을 남깁니다.

- **주요 필드**:
  - `asset_id`: 대상 자산
  - `requester_id`: 요청자 (`User`)
  - `type`: `MaintenanceType` (`REPAIR`, `CHECK`, `UPGRADE`)
  - `description`: 요청 증상 및 상세 내용
  - `status`: `MaintenanceStatus` (진행 상태)
  - `processor_id`: 처리자(관리자) ID
  - `rejectionReason`: 반려 시 작성된 거절 사유
  - `completedAt`: 처리가 완료되거나 반려된 일시

### 3.2 Disposal (폐기 엔티티)
폐기 요청 역시 단일 엔티티에서 승인 내역까지 관리하도록 `disposal` 테이블을 신설하여 구현되었습니다.

- **주요 필드**:
  - `asset_id`: 대상 자산
  - `requesterEmployeeNo`: 요청자 사번
  - `reason`: 폐기 요청 사유
  - `status`: `DisposalStatus` (진행 상태)
  - `processor_id`: 처리자(관리자) ID
  - `rejectionReason`: 반려 시 작성된 거절 사유
  - `processedAt`: 처리 완료(승인/반려) 일시

## 4. 백엔드 API 명세 (Backend API)

### 4.1 유지보수 (Maintenance Controller)
`/api/maintenances` 하위에 구성되었습니다.

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/api/maintenances` | 유지보수 요청 목록 조회 (권한 기반 필터링) | USER, ADMIN, SUPER |
| POST | `/api/maintenances` | 유지보수 요청 등록 | Authenticated |
| PUT | `/api/maintenances/{id}/approve` | 유지보수 승인 | ADMIN, SUPER |
| PUT | `/api/maintenances/{id}/complete`| 유지보수 완료 처리 | ADMIN, SUPER |
| PUT | `/api/maintenances/{id}/reject` | 유지보수 반려 (사유 포함) | ADMIN, SUPER |

### 4.2 폐기 (Disposal Controller)
`/api/disposals` 하위에 구성되었습니다.

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/api/disposals` | 폐기 요청 목록 조회 | USER, ADMIN, SUPER |
| POST | `/api/disposals` | 폐기 요청 등록 | Authenticated |
| PUT | `/api/disposals/{id}/approve` | 폐기 승인 | ADMIN, SUPER |
| PUT | `/api/disposals/{id}/reject` | 폐기 반려 (사유 포함) | ADMIN, SUPER |

> *참고: 하위 호환성을 위해 `AssetController`에도 `/api/assets/{assetId}/disposal...` 엔드포인트가 라우팅되어 있습니다.*

## 5. 프론트엔드 UI 설계 (Frontend Design)

### 5.1 유지보수 및 폐기 목록 (List Pages)
- `MaintenanceListPage.js`, `DisposalListPage.js`를 통해 각 상태(`PENDING`, `IN_PROGRESS`/`APPROVED`, `COMPLETED`, `REJECTED`)를 탭이나 필터로 구분하여 확인.
- 권한에 따라 화면 분기:
  - **일반 사용자(USER)**: 본인이 신청한 내역 또는 소속 부서 자산의 내역만 조회.
  - **관리자(ADMIN, SUPER)**: 모든 자산의 내역 조회 및 결재(승인/반려/완료) 버튼 노출.

### 5.2 반려 사유 확인 UI (Rejection Reason UI)
요청 반려 시 사용자 경험(UX) 강화를 위해 `MessageModal` 또는 Expandable UI를 통해 처리 사유를 명시적으로 제공합니다.
1. 목록에서 상태가 `REJECTED`인 항목의 "반려 사유 보기" 버튼 클릭.
2. `MessageModal` 창을 통해 관리자가 입력한 텍스트를 출력합니다.
3. 알림(Notification) 기능과 연동되어 뱃지를 통해 즉각적인 결과를 안내합니다.

## 6. 알림 통합 포인트 (Notification Integration)
*   **유지보수 요청**: `SUPER` 관리자에게 신규 등록 알림 (`NEW_REQUEST`).
*   **유지보수 승인/반려**: 신청자에게 결과 알림 전송.
*   **폐기 요청**: `ADMIN` 관리자에게 신규 등록 알림 (`NEW_REQUEST`).
*   **폐기 승인/반려**: 신청자에게 결과 알림 전송.
