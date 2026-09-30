# 알림 시스템 상세 설계 (Notification System Design)

> **Implementation Status: Complete**
> 본 문서는 실제 구현된 서버 코드를 기준으로 동기화되었습니다.

## 1. 개요 (Overview)
본 문서는 자산 관리 시스템의 알림 기능을 위한 상세 설계 문서입니다. 사용자의 요청(이관, 유지보수, 폐기, 회원가입)에 대한 상태 변경(승인/거절) 및 관리자(ADMIN, SUPER)를 위한 새로운 요청 알림을 제공하여 업무 효율성과 사용자 경험을 향상시키는 것을 목적으로 합니다.

## 2. 엔티티 설계 (Entity Design)

### 2.1 Notification Entity
알림 데이터를 저장하는 메인 엔티티입니다.

```java
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "related_entity_type")
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    @Column(name = "is_read")
    @Builder.Default
    private boolean isRead = false;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

### 2.2 NotificationType Enum
알림의 유형을 정의합니다.

```java
public enum NotificationType {
    // 사용자 대상 알림
    TRANSFER_APPROVED,      // 이관 요청 승인
    TRANSFER_REJECTED,      // 이관 요청 거절
    MAINTENANCE_APPROVED,   // 유지보수 요청 승인
    MAINTENANCE_REJECTED,   // 유지보수 요청 거절
    MAINTENANCE_COMPLETED,  // 유지보수 완료
    DISPOSAL_APPROVED,      // 폐기 요청 승인
    DISPOSAL_REJECTED,      // 폐기 요청 거절
    SIGNUP_APPROVED,        // 회원가입 승인
    SIGNUP_REJECTED,        // 회원가입 거절
    ASSET_STATUS_CHANGED,   // 자산 상태 변경

    // 관리자 대상 알림
    NEW_REQUEST             // 새로운 요청 도착 (이관, 유지보수, 폐기, 가입 등)
}
```

## 3. 백엔드 설계 (Backend Design)

### 3.1 NotificationRepository
JPA Repository를 사용하여 데이터 접근을 처리합니다.

```java
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientEmployeeNoOrderByCreatedAtDesc(String employeeNo);
    long countByRecipientEmployeeNoAndIsReadFalse(String employeeNo);
}
```

### 3.2 NotificationService
비즈니스 로직을 담당합니다.

*   **createNotification**: 단일 사용자 대상 알림 생성
*   **getNotifications**: 사용자별 알림 목록 전체 조회
*   **getUnreadCount**: 읽지 않은 알림 개수 조회
*   **markAsRead**: 특정 알림 읽음 처리
*   **markAllAsRead**: 사용자의 모든 알림 읽음 처리
*   **notifyAdmins**: `Role.ADMIN` 권한을 가진 모든 사용자에게 알림 전송 (이관, 폐기 등)
*   **notifySuper**: `Role.SUPER` 권한을 가진 모든 사용자에게 알림 전송 (유지보수 등)
*   **notifyAdminsAndSuper**: `Role.ADMIN`과 `Role.SUPER` 모두에게 알림 전송 (회원가입 등)

### 3.3 NotificationController
REST API 엔드포인트를 제공합니다.

*   `GET /api/notifications`: 내 알림 목록 조회 (List 반환, Pagination 미적용)
*   `GET /api/notifications/unread-count`: 읽지 않은 알림 개수 조회
*   `PUT /api/notifications/{id}/read`: 단일 알림 읽음 처리
*   `PUT /api/notifications/read-all`: 전체 알림 읽음 처리

## 4. 프론트엔드 설계 (Frontend Design)

### 4.1 UI 구성 요소

1.  **Header Bell Icon (헤더 알림 아이콘)**
    *   **위치**: 로그인한 사용자의 헤더 우측 상단 (프로필 아이콘 옆).
    *   **기능**:
        *   읽지 않은 알림이 있을 경우 빨간색 뱃지(Badge)에 개수 표시.
        *   클릭 시 알림 드롭다운(Dropdown) 토글.

2.  **Notification Dropdown (알림 드롭다운)**
    *   **구성**:
        *   최신 알림 표시.
        *   각 항목: 제목, 메시지, 시간('방금 전', '1시간 전' 등 Date-fns 활용).
        *   읽지 않은 알림은 `unread` CSS 클래스로 시각적 강조.
        *   상단 또는 하단에 "모두 읽음" 버튼.
    *   **인터랙션**:
        *   알림 항목 클릭 시 해당 상세 페이지로 이동하며 API를 통해 읽음 처리됨.

3.  **NotificationListPage (알림 목록 페이지)**
    *   **경로**: `/notifications`
    *   **기능**:
        *   전체 알림 이력을 목록 형태로 표시.
        *   `isRead` 상태에 따라 스타일 분리.

### 4.2 API 연동 로직 및 Navigation
*   **Polling**: 1분 주기(`setInterval`)로 `unread-count`와 목록을 재요청하여 최신 상태 유지.
*   **Navigation**: 알림 클릭 시 `relatedEntityType`에 따라 적절한 라우트로 이동 (`useNavigate` 활용).
    *   `TRANSFER` -> `/transfers`
    *   `MAINTENANCE` -> `/maintenances`
    *   `DISPOSAL` -> `/disposals`
    *   `USER` -> `/users` (회원가입/상태변경 시)

## 5. 통합 포인트 (Integration Points)

실제 구현된 각 Service에서 상태 변경 시 알림 메서드를 호출합니다.

| 트리거 이벤트 | 발송 대상 함수 | 수신자 | 알림 유형 | 메시지 예시 |
|---|---|---|---|---|
| **회원가입 요청** | `notifyAdminsAndSuper` | ADMIN, SUPER | `NEW_REQUEST` | "[홍길동]님의 회원가입 요청이 있습니다." |
| **이관 요청** | `notifyAdmins` | ADMIN | `NEW_REQUEST` | "[자산명] 이관 요청이 등록되었습니다." |
| **이관 승인** | `createNotification` | 요청자(보내는사람) | `TRANSFER_APPROVED` | "이관 요청이 승인되었습니다." |
| **이관 거절** | `createNotification` | 요청자(보내는사람) | `TRANSFER_REJECTED` | "이관 요청이 반려되었습니다." |
| **유지보수 요청** | `notifySuper` | SUPER | `NEW_REQUEST` | "[자산명] 유지보수 요청이 등록되었습니다." |
| **폐기 요청** | `notifyAdmins` | ADMIN | `NEW_REQUEST` | "[자산명] 폐기 요청이 등록되었습니다." |

*참고: 권한 분리 규칙에 따라 유지보수 관련 처리는 주로 SUPER 관리자가, 이관/폐기 등 일반 자산 상태 변경은 ADMIN 관리자가 주로 담당하므로 알림 수신 대상이 구분되어 구현되었습니다.*

## 6. API 명세 (API Specification)

### 6.1 내 알림 목록 조회
*   **URL**: `GET /api/notifications`
*   **Auth**: Authenticated User

**Response Success (200 OK)**
```json
{
  "success": true,
  "data": [
    {
      "id": 10,
      "type": "MAINTENANCE_APPROVED",
      "title": "유지보수 승인",
      "message": "노트북(A-123) 수리 요청이 승인되었습니다.",
      "relatedEntityType": "MAINTENANCE",
      "relatedEntityId": 55,
      "isRead": false,
      "createdAt": "2024-02-03T10:00:00"
    }
  ]
}
```

### 6.2 읽지 않은 알림 개수 조회
*   **URL**: `GET /api/notifications/unread-count`
*   **Auth**: Authenticated User

**Response Success (200 OK)**
```json
{
  "success": true,
  "data": 3
}
```

### 6.3 알림 읽음 처리
*   **URL**: `PUT /api/notifications/{id}/read`
*   **Auth**: Authenticated User

### 6.4 모든 알림 읽음 처리
*   **URL**: `PUT /api/notifications/read-all`
*   **Auth**: Authenticated User
