# 자산 이관 기능 설계 문서

> **Implementation Status: Complete**
> 본 문서는 실제 구현된 서버 코드를 기준으로 동기화되었습니다.

## 1. 개요 (Overview)

자산의 실사용자(actualUser)가 변경될 때 이관 신청-승인 프로세스를 통해 체계적으로 관리하고, 모든 이관 이력을 추적 가능하도록 합니다.
현재 시스템은 등록되지 않은 외부 사용자 대상 이관 정보도 수용할 수 있도록 유연한 데이터 구조를 갖추고 있습니다.

## 2. 상태 관리 (Status Management)

### 2.1 자산 상태 (AssetStatus)
- **NORMAL**: 이관 전/후 정상 상태
- **TRANSFER_PENDING**: 이관 대기 상태 (신청됨)

### 2.2 이관 요청 상태 (TransferStatus)
- **PENDING**: 이관 대기중
- **APPROVED**: 승인 완료
- **REJECTED**: 거절 완료

## 3. 엔티티 설계 (Entity Design)

`Transfer` 테이블은 이관의 주체, 객체, 대상을 명확히 정의합니다.

- **기본 정보**: `asset` (대상 자산), `status` (진행 상태), `reason` (이관 사유)
- **실사용자(과거)**: `fromUserName`, `fromUserDepartment`
- **실사용자(미래/대상)**: `recipientName`, `recipientDepartment`, `recipientContact`, `recipientEmail`
- **부가 정보**: `transferLocation`, `remarks`
- **처리 내역**:
  - `requestedBy`: 신청자 (시스템 사용자 `User`)
  - `processedBy`: 처리자 (ADMIN/SUPER `User`)
  - `rejectReason`: **반려 시 사유 저장 필드**
  - `requestDate`, `processDate`

## 4. 백엔드 API 명세 (Backend API)

`/api/transfers` 하위의 컨트롤러를 통해 작동합니다.

| Method | Endpoint | Description | Request Body | Access |
|---|---|---|---|---|
| POST | `/api/transfers` | 이관 신청 | `TransferRequest` (이관 대상 정보 및 사유) | USER, ADMIN, SUPER |
| GET | `/api/transfers/my` | 내 이관 목록 조회 | - | Authenticated |
| GET | `/api/transfers/all`| 모든 이관 목록 조회 | - | ADMIN, SUPER |
| GET | `/api/transfers` | 이관 목록 페이징 조회 | 파라미터: `type`(sent/received), `status` | Authenticated |
| PUT | `/api/transfers/{id}/approve` | 이관 승인 | - | ADMIN, SUPER |
| PUT | `/api/transfers/{id}/reject` | 이관 반려 | `TransferStatusUpdateRequest` (`reason` 필드 필수) | ADMIN, SUPER |

## 5. 백엔드 핵심 비즈니스 로직 (Service Logic)

### 5.1 이관 신청 (`createTransfer`)
1. 자산의 상태가 `TRANSFER_PENDING`인지 확인 (중복 신청 방지).
2. `Transfer` 엔티티 생성 및 `PENDING` 상태로 저장.
3. 자산의 상태를 `TRANSFER_PENDING`으로 업데이트.
4. ADMIN 관리자에게 신규 요청 알림(`NEW_REQUEST`) 발송.

### 5.2 이관 승인 (`approveTransfer`)
1. ADMIN 또는 SUPER 권한을 가진 관리자가 승인 요청.
2. `Transfer` 엔티티 상태를 `APPROVED`로 변경 및 `processDate`, `processedBy` 업데이트.
3. 대상 자산의 실사용자 정보(`actualUserName`, `actualUserDepartment` 등)를 이관 대상자 정보로 업데이트.
4. 자산 상태를 `NORMAL`로 변경.
5. 신청자에게 `TRANSFER_APPROVED` 알림 전송.

### 5.3 이관 반려 (`rejectTransfer`)
1. ADMIN 또는 SUPER 권한을 가진 관리자가 반려 사유(`TransferStatusUpdateRequest.reason`)와 함께 거절 요청.
2. `Transfer` 엔티티 상태를 `REJECTED`로 변경, `rejectReason` 저장.
3. 실사용자 정보 유지, 자산 상태만 `NORMAL`로 롤백.
4. 신청자에게 `TRANSFER_REJECTED` 알림 전송.

## 6. 프론트엔드 연동 및 UI

### 6.1 이관 관리 (관리자 뷰)
- `TransferListPage`에서 `Transfer` 목록 조회 시, `PENDING` 상태의 항목에 대해 "승인" 및 "반려" 버튼을 제공.
- 반려 버튼 클릭 시 `MessageModal` 또는 사유 입력 폼을 띄워 `rejectReason`을 필수적으로 수집하여 서버로 전송.

### 6.2 자산 상세 페이지 (Asset Detail)
- 자산의 상태가 `NORMAL`일 때만 "이관 신청" 버튼 활성화.
- 상태가 `TRANSFER_PENDING`일 경우 화면 상단 혹은 상태 배지(Badge)를 통해 처리 대기중임을 시각적으로 안내.
- 이관 요청 거절된 내역(`REJECTED`) 클릭 시 모달 또는 펼침 영역을 통해 `rejectReason`을 출력.
