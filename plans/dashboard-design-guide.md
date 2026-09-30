# GIIC 자산 관리 시스템 - 대시보드 디자인 가이드

## 목차
1. [전체 레이아웃 구조](#전체-레이아웃-구조)
2. [헤더 디자인](#헤더-디자인)
3. [사이드바 네비게이션](#사이드바-네비게이션)
4. [대시보드 위젯 구성](#대시보드-위젯-구성)
5. [역할별 UI 차이](#역할별-ui-차이)
6. [권장 페이지 구현 순서](#권장-페이지-구현-순서)

---

## 전체 레이아웃 구조

### 레이아웃 타입: 사이드바 + 헤더 조합

```
┌─────────────────────────────────────────────────────────┐
│  Header (Logo, Search, User Info, Notifications)       │
├──────────┬──────────────────────────────────────────────┤
│          │                                              │
│ Sidebar  │  Main Content Area                           │
│          │  - Breadcrumbs                               │
│  - 대시보드 │  - Page Title                               │
│  - 자산관리 │  - Widgets / Tables / Forms                 │
│  - 이관관리 │                                              │
│  - 유지보수 │                                              │
│  - 사용자   │                                              │
│  - 설정    │                                              │
│          │                                              │
│          │                                              │
└──────────┴──────────────────────────────────────────────┘
```

### 디자인 원칙
- **깨끗한 흰색 배경** (`#ffffff`)
- **미묘한 그라데이션 액센트** (purple/blue: `#6B5B95` → `#3B82F6`)
- **카드 기반 UI** (둥근 모서리, 미묘한 그림자)
- **직관적인 아이콘 사용**
- **모바일 반응형 디자인**

---

## 헤더 디자인

### 구성 요소 (왼쪽 → 오른쪽)

```jsx
┌────────────────────────────────────────────────────────────────┐
│ [Logo] GIIC 자산관리  |  [검색바]  |  [알림] [사용자▼] [로그아웃] │
└────────────────────────────────────────────────────────────────┘
```

#### 1. 로고 및 시스템 이름
- **로고**: `logo.jpg` (높이: 40px)
- **시스템명**: "GIIC 자산 관리 시스템" 또는 "자산관리"
- **색상**: 텍스트는 `#1e293b`
- **위치**: 헤더 왼쪽, 사이드바 토글 버튼 옆

#### 2. 전역 검색바
- **위치**: 헤더 중앙
- **기능**: 자산명, 자산번호, 사용자명 통합 검색
- **스타일**: 
  - 둥근 검색바 (border-radius: 20px)
  - 배경: `#f8fafc`
  - 아이콘: 돋보기 아이콘
  - 플레이스홀더: "자산 또는 사용자 검색..."

#### 3. 우측 유틸리티
- **알림 아이콘**: 
  - 새 이관 요청, 유지보수 알림 표시
  - 빨간 뱃지로 개수 표시
- **사용자 드롭다운**:
  - 사용자 이름 및 역할 표시 ("홍길동님 (관리자)")
  - 드롭다운 메뉴: 내 정보, 설정, 로그아웃
- **로그아웃 버튼**: 간단한 아웃라인 버튼

### 헤더 스타일
```css
.header {
  height: 64px;
  background: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  box-shadow: 0 2px 4px rgba(0,0,0,0.04);
  display: flex;
  align-items: center;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 100;
}
```

---

## 사이드바 네비게이션

### 구조 및 메뉴 항목

#### 모든 사용자 공통 (USER, ADMIN, SUPER)
```
📊 대시보드
📦 자산 관리
   └ 자산 목록 (조회/검색)
   └ 자산 등록
🔄 이관 관리
   └ 이관 요청
   └ 이관 승인 (ADMIN, SUPER)
🔧 유지보수
   └ 유지보수 내역
   └ 유지보수 등록
```

#### ADMIN, SUPER 전용
```
👥 사용자 관리
   └ 사용자 목록
   └ 승인 대기
📊 리포트
   └ 자산 통계
   └ 부서별 현황
```

#### SUPER 전용
```
⚙️ 시스템 설정
   └ 서버 상태 모니터링
   └ OCR 상태/로그
   └ 기본 설정
```

### 사이드바 스타일
```css
.sidebar {
  width: 260px;
  background: #ffffff;
  border-right: 1px solid #e2e8f0;
  height: calc(100vh - 64px);
  position: sticky;
  top: 64px;
  overflow-y: auto;
}

.sidebar-item {
  padding: 12px 20px;
  color: #64748b;
  transition: all 0.2s;
}

.sidebar-item.active {
  background: linear-gradient(to right, rgba(59,130,246,0.1), transparent);
  color: #3B82F6;
  border-left: 3px solid #3B82F6;
}

.sidebar-item:hover {
  background: #f8fafc;
  color: #1e293b;
}
```

### 사이드바 접기/펼치기 기능
- **토글 버튼**: 헤더 좌측에 햄버거 메뉴
- **축소 모드**: 아이콘만 표시 (폭: 64px)
- **모바일**: 자동으로 오버레이 모드로 전환

---

## 대시보드 위젯 구성

### 레이아웃 구조 (그리드 시스템)
```
┌─────────────────────────────────────────────────────┐
│  📊 Dashboard > 대시보드                              │
├─────────────┬─────────────┬─────────────┬───────────┤
│  총 자산     │  정상 자산   │  수리중      │  이관 대기 │
│  1,234개    │  1,180개    │  45개       │  9건      │
├─────────────┴─────────────┴─────────────┴───────────┤
│  최근 등록 자산 (테이블)                  │ 자산 현황   │
│  - 자산명, 자산번호, 등록일, 상태          │ (파이 차트) │
│  - 최대 5개 표시                         │            │
│  - "전체보기" 링크                        │            │
├─────────────────────────────────────────┼───────────┤
│  대기중인 이관 요청 (USER)                │ 내 자산    │
│  승인 대기 이관 (ADMIN)                   │ 상태 요약  │
│  - 요청자, 자산명, 요청일                 │ (바 차트)  │
│  - 승인/거부 버튼 (ADMIN)                │            │
├─────────────────────────────────────────┴───────────┤
│  최근 유지보수 내역                                    │
│  - 자산명, 유지보수 유형, 일자, 담당자                 │
└─────────────────────────────────────────────────────┘
```

### 주요 위젯 상세

#### 1. 통계 카드 (Stats Cards)
4개의 주요 지표를 상단에 배치

```jsx
// 위젯 예시
<StatCard 
  title="총 자산"
  value="1,234"
  unit="개"
  icon={BoxIcon}
  color="#3B82F6"
  trend="+12 (이번 달)"
/>
```

**표시 지표**:
- **총 자산**: 전체 등록된 자산 수
- **정상 자산**: `NORMAL` 상태 자산
- **수리중**: `REPAIRING` 상태 자산
- **이관 대기**: `PENDING` 상태 이관 건수

**스타일**:
- 흰색 배경, 둥근 모서리 (border-radius: 12px)
- 미묘한 그림자: `box-shadow: 0 2px 8px rgba(0,0,0,0.06)`
- 그라데이션 아이콘 배경
- 호버 효과: 살짝 위로 이동 및 그림자 강화

#### 2. 최근 등록 자산 테이블
- **API**: `GET /api/assets?size=5&sort=createdAt,desc`
- **컬럼**: 자산명, 자산번호, 상태, 담당자, 등록일
- **기능**: 
  - 상태 뱃지 (색상 구분)
  - 행 클릭 시 상세 페이지로 이동
  - "전체보기" 버튼 → 자산 관리 페이지

#### 3. 자산 현황 차트
- **차트 유형**: 도넛 차트 (Doughnut Chart)
- **데이터**: 상태별 자산 분포 (NORMAL, REPAIRING, DISPOSED, LOST)
- **라이브러리**: Chart.js 또는 Recharts 권장
- **색상**:
  - NORMAL: `#10b981` (녹색)
  - REPAIRING: `#f59e0b` (주황)
  - DISPOSED: `#6b7280` (회색)
  - LOST: `#ef4444` (빨강)

#### 4. 이관 요청 현황
**USER 뷰**:
- 본인이 요청한 이관 목록 (최근 5건)
- 상태별 필터링

**ADMIN 뷰**:
- 승인 대기중인 이관 요청
- 인라인 승인/거부 버튼
- 긴급 표시 (오래된 요청 강조)

#### 5. 유지보수 내역
- **API**: `GET /api/maintenances?size=5&sort=createdAt,desc`
- **표시**: 자산명, 유지보수 유형, 일자, 담당자
- **유지보수 유형 뱃지**: 
  - 정기점검: 파랑
  - 수리: 주황
  - 교체: 빨강

#### 6. 빠른 작업 버튼 (Quick Actions)
우측 상단 또는 별도 섹션에 배치
```
[+ 자산 등록]  [이관 요청]  [유지보수 등록]
```
- 모든 사용자가 자산 등록, 유지보수 등록 가능
- 권한에 따라 동적으로 버튼 표시 (예: 이관 승인은 ADMIN+)

---

## 역할별 권한 및 UI 차이

### 권한 매트릭스 (Permission Matrix)

| 기능 (Feature) | USER | ADMIN | SUPER |
|---|:---:|:---:|:---:|
| 로그인 (Login) | ✓ | ✓ | ✓ |
| 회원가입 요청 (Member Registration Request) | ✓ | ✓ | ✓ |
| 회원 승인 (Member Approval) | ✗ | ✓ | ✓ |
| 사용자 목록 조회 (User List View) | ✗ | ✓ | ✓ |
| 사용자 정보 수정 (User Information Edit) | ✗ | ✓ | ✓ |
| 사용자 차단/해제 (User Block/Unblock) | ✗ | ✓ | ✓ |
| 자산 등록 (OCR 포함) (Asset Registration with OCR) | ✓ | ✓ | ✓ |
| 자산 조회 / 검색 (Asset View / Search) | ✓ | ✓ | ✓ |
| 자산 수정 (Asset Edit) | ✓ | ✓ | ✓ |
| 사용자 재배정 (User Reassignment) | ✓ | ✓ | ✓ |
| 자산 이미지 관리 (Asset Image Management) | ✓ | ✓ | ✓ |
| 유지/보수 등록 (Maintenance/Repair Registration) | ✓ | ✓ | ✓ |
| 유지/보수 조회 (Maintenance/Repair View) | ✓ | ✓ | ✓ |
| 서버 상태 모니터링 (Server Status Monitoring) | ✗ | ✗ | ✓ |
| OCR 상태/로그 확인 (OCR Status/Log Check) | ✗ | ✗ | ✓ |
| 시스템 설정 (System Settings) | ✗ | ✗ | ✓ |

### USER (일반 사용자)

#### 주요 권한
- **자산 관리**: 본인이 직접 자산을 등록(OCR 포함), 수정, 이미지 관리 가능
- **이관/유지보수**: 이관 요청 및 유지보수 등록 가능
- **제한사항**: 사용자 관리, 시스템 설정 접근 불가. 이관 승인 불가.

#### 대시보드
```
✅ 내 등록 자산 통계
✅ 내가 요청한 이관 현황
✅ 내 자산의 유지보수 내역
```

#### 메뉴
- 대시보드
- 자산 관리 (등록/조회/수정)
- 이관 관리 (요청)
- 유지보수 (등록/조회)

---

### ADMIN (관리자)

#### 주요 권한
- **USER의 모든 권한 포함**
- **사용자 관리**: 회원 가입 승인, 사용자 목록 조회, 정보 수정, 차단/해제
- **이관 관리**: 이관 요청 승인/거부
- **제한사항**: 시스템 설정, 서버 모니터링, OCR 로그 접근 불가

#### 대시보드
```
✅ 전체 자산 통계
✅ 전체 이관 요청 (승인 권한)
✅ 부서별 자산 현황
✅ 월간/연간 리포트
```

#### 메뉴
- 대시보드 (전체 통계)
- 자산 관리 (전체 기능)
- 이관 관리 (승인/거부)
- 유지보수 (전체 기능)
- **사용자 관리** (승인/목록/수정/차단)
- 리포트

---

### SUPER (최고 관리자)

#### 주요 권한
- **ADMIN의 모든 권한 포함**
- **시스템 관리**: 서버 상태 모니터링, OCR 로그 확인, 시스템 설정

#### 대시보드
```
✅ ADMIN의 모든 기능
✅ 시스템 상태 모니터링
✅ OCR 로그 및 오류 현황
```

#### 메뉴
- ADMIN의 모든 메뉴
- **시스템 설정**
  - 서버 상태
  - OCR 로그
  - 환경 설정

---

## 권장 페이지 구현 순서

### Phase 1: 핵심 기능 (우선순위 높음)

#### 1. 대시보드 페이지 (`/dashboard`)
**목적**: 시스템의 첫 화면, 주요 지표 한눈에 파악
**구현 요소**:
- 통계 카드 (4개)
- 최근 등록 자산 테이블
- 자산 현황 차트
- 역할별 동적 렌더링

**API 엔드포인트**:
- `GET /api/assets?size=5&sort=createdAt,desc`
- `GET /api/assets?status=NORMAL` (각 상태별 카운트)
- `GET /api/transfers?status=PENDING&size=5`
- `GET /api/maintenances?size=5&sort=createdAt,desc`

---

#### 2. 자산 목록 페이지 (`/assets`)
**목적**: 자산 검색, 필터링, 조회
**구현 요소**:
- 검색바 (자산명, 자산번호, 사용자)
- **보기 필터 (USER 전용)**:
  - "내가 등록한 자산" (기본값 또는 옵션)
  - "전체 자산"
- 필터 (상태, 부서, 담당자)
- 페이지네이션 테이블
- 엑셀 다운로드 버튼 (ADMIN+)
- 자산 등록 버튼 (모든 사용자)

**API 엔드포인트**:
- `GET /api/assets?keyword=&status=&dept=&page=0&size=20`

---

#### 3. 자산 상세 페이지 (`/assets/:id`)
**목적**: 자산 정보 상세 조회 및 수정
**구현 요소**:
- 자산 기본 정보 (읽기 전용 또는 수정 모드)
- 자산 이미지 갤러리
- 유지보수 이력 탭
- 이관 이력 탭
- 수정/삭제 버튼 (ADMIN+)

**API 엔드포인트**:
- `GET /api/assets/{assetId}`
- `PUT /api/assets/{assetId}` (수정)
- `DELETE /api/assets/{assetId}` (삭제)
- `GET /api/maintenances?asset_id={assetId}`
- `GET /api/transfers?asset_id={assetId}`

---

#### 4. 자산 등록 페이지 (`/assets/new`)
**목적**: 새 자산 등록 (모든 사용자)
**구현 요소**:
- 자산 정보 입력 폼
- 이미지 업로드 (드래그 앤 드롭)
- OCR 자동 입력 기능
- 유효성 검증

**API 엔드포인트**:
- `POST /api/assets`
- `POST /api/assets/ocr` (OCR 처리)
- `POST /api/assets/{assetId}/images` (이미지 업로드)

---

### Phase 2: 이관 및 유지보수 (우선순위 중)

#### 5. 이관 요청 페이지 (`/transfers/new`)
**목적**: 자산 이관 요청
**구현 요소**:
- 자산 선택 (검색 가능)
- 이관 받을 사용자 선택
- 이관 사유 입력
- 제출 버튼

**API 엔드포인트**:
- `POST /api/transfers`
- `GET /api/assets` (자산 검색용)
- `GET /api/users` (사용자 검색용)

---

#### 6. 이관 관리 페이지 (`/transfers`)
**목적**: 이관 요청 조회 및 승인/거부
**구현 요소**:
- 이관 목록 테이블
- 상태 필터 (PENDING, APPROVED, REJECTED)
- 승인/거부 모달 (ADMIN+)
- 이관 상세 정보

**API 엔드포인트**:
- `GET /api/transfers?status=&page=0&size=20`
- `PATCH /api/transfers/{transferId}/status` (승인/거부)

---

#### 7. 유지보수 내역 페이지 (`/maintenances`)
**목적**: 유지보수 이력 조회
**구현 요소**:
- 유지보수 내역 테이블
- 자산별/기간별 필터
- 유지보수 등록 버튼

**API 엔드포인트**:
- `GET /api/maintenances?asset_id=&page=0&size=20`

---

#### 8. 유지보수 등록 페이지 (`/maintenances/new`)
**목적**: 유지보수 기록 등록
**구현 요소**:
- 자산 선택
- 유지보수 유형 선택
- 작업 내용 입력
- 비용 입력
- 담당자 지정

**API 엔드포인트**:
- `POST /api/maintenances`

---

### Phase 3: 관리 기능 (우선순위 낮음)

#### 9. 사용자 관리 페이지 (`/users`) - SUPER만
**목적**: 사용자 조회 및 관리
**구현 요소**:
- 사용자 목록 테이블
- 상태별 필터 (APPROVED, PENDING, BLOCKED)
- 사용자 승인/거부/차단 기능
- 역할 변경 기능

**API 엔드포인트**:
- `GET /api/users?status=&page=0&size=20`
- `PATCH /api/users/{userId}/status`

---

#### 10. 통계 및 리포트 페이지 (`/reports`) - ADMIN+
**목적**: 시스템 통계 및 분석
**구현 요소**:
- 부서별 자산 현황
- 월별 이관 통계
- 유지보수 비용 분석
- 자산 감가상각 현황
- PDF/Excel 다운로드

**API 엔드포인트**:
- 커스텀 통계 API 필요 (백엔드 추가 개발)

---

#### 11. 내 정보 페이지 (`/profile`)
**목적**: 사용자 정보 조회 및 수정
**구현 요소**:
- 프로필 정보 (읽기 전용: 사번, 이름, 부서)
- 비밀번호 변경
- 알림 설정

**API 엔드포인트**:
- `GET /api/users/me`
- `PATCH /api/users/me/password`

---

#### 12. 시스템 설정 페이지 (`/settings`) - SUPER만
**목적**: 시스템 전반 설정
**구현 요소**:
- 부서 관리 (추가/수정/삭제)
- 자산 카테고리 관리
- 시스템 로그 조회
- 백업 및 복원

**API 엔드포인트**:
- 추가 API 개발 필요

---

## 컴포넌트 구조 권장사항

### 공통 컴포넌트
```
src/components/
├── layout/
│   ├── Header.js             # 헤더 컴포넌트
│   ├── Sidebar.js            # 사이드바 네비게이션
│   ├── DashboardLayout.js    # 전체 레이아웃 래퍼
│   └── Breadcrumbs.js        # 경로 표시
├── widgets/
│   ├── StatCard.js           # 통계 카드
│   ├── AssetStatusChart.js   # 자산 현황 차트
│   ├── RecentAssetsTable.js  # 최근 자산 테이블
│   └── PendingTransfers.js   # 이관 대기 위젯
├── common/
│   ├── Table.js              # 재사용 가능한 테이블
│   ├── Pagination.js         # 페이지네이션
│   ├── SearchBar.js          # 검색바
│   ├── StatusBadge.js        # 상태 뱃지
│   ├── Modal.js              # 모달 컴포넌트
│   └── Button.js             # 버튼 컴포넌트
└── forms/
    ├── AssetForm.js          # 자산 등록/수정 폼
    ├── TransferForm.js       # 이관 요청 폼
    └── MaintenanceForm.js    # 유지보수 등록 폼
```

---

## UI 라이브러리 권장사항

### 추천 라이브러리
1. **Ant Design** 또는 **Material-UI (MUI)**
   - 풍부한 컴포넌트 제공
   - 테이블, 모달, 폼 등 기본 제공
   - 한국어 지원

2. **Chart.js** 또는 **Recharts**
   - 차트 렌더링
   - 반응형 차트 지원

3. **React Icons**
   - 아이콘 라이브러리

4. **React Hook Form**
   - 폼 유효성 검증

5. **Axios**
   - API 통신 (이미 사용중)

---

## 색상 팔레트

### 주요 색상
```css
:root {
  /* Primary - Gradient */
  --gradient-purple: #6B5B95;
  --gradient-violet: #8B5CF6;
  --gradient-blue: #3B82F6;
  --gradient-cyan: #0EA5E9;

  /* Backgrounds */
  --bg-light: #f0f4f8;
  --bg-white: #ffffff;
  --bg-card: #ffffff;

  /* Text */
  --text-primary: #1e293b;
  --text-secondary: #64748b;
  --text-muted: #94a3b8;

  /* Status Colors */
  --status-normal: #10b981;     /* 녹색 - 정상 */
  --status-repairing: #f59e0b;  /* 주황 - 수리중 */
  --status-disposed: #6b7280;   /* 회색 - 폐기 */
  --status-lost: #ef4444;       /* 빨강 - 분실 */

  /* Action Colors */
  --action-primary: #3B82F6;
  --action-success: #10b981;
  --action-warning: #f59e0b;
  --action-danger: #ef4444;

  /* Borders */
  --border-light: #e2e8f0;
  --border-medium: #cbd5e1;
}
```

---

## 반응형 디자인 가이드

### 브레이크포인트
```css
/* Mobile */
@media (max-width: 768px) {
  - 사이드바: 오버레이 모드
  - 테이블: 카드 뷰로 전환
  - 통계 카드: 2열 또는 1열
}

/* Tablet */
@media (min-width: 769px) and (max-width: 1024px) {
  - 사이드바: 축소 모드 (아이콘만)
  - 통계 카드: 2x2 그리드
}

/* Desktop */
@media (min-width: 1025px) {
  - 사이드바: 전체 표시
  - 통계 카드: 1행 4열
}
```

---

## 접근성 (Accessibility)

### 권장 사항
1. **키보드 네비게이션**: 모든 요소 Tab 키로 접근 가능
2. **ARIA 레이블**: 스크린 리더 지원
3. **색상 대비**: WCAG AA 기준 준수
4. **포커스 표시**: 명확한 포커스 인디케이터
5. **에러 메시지**: 명확하고 구체적인 안내

---

## 성능 최적화

### 권장 사항
1. **코드 스플리팅**: React.lazy() 사용
2. **이미지 최적화**: WebP 형식, Lazy Loading
3. **메모이제이션**: React.memo, useMemo, useCallback
4. **가상 스크롤**: 긴 리스트는 react-window 사용
5. **API 캐싱**: React Query 또는 SWR 사용

---

## 다음 단계

### 구현 체크리스트
- [ ] 레이아웃 컴포넌트 생성 (Header, Sidebar, DashboardLayout)
- [ ] 공통 컴포넌트 라이브러리 구축
- [ ] 대시보드 페이지 구현 (Phase 1-1)
- [ ] 자산 관리 페이지 구현 (Phase 1-2~4)
- [ ] 이관 관리 페이지 구현 (Phase 2-5~6)
- [ ] 유지보수 페이지 구현 (Phase 2-7~8)
- [ ] 사용자 관리 페이지 구현 (Phase 3-9)
- [ ] 통계 및 리포트 페이지 구현 (Phase 3-10)

---

## 참고 자료

### 디자인 참고
- Ant Design Pro: https://pro.ant.design/
- Material Dashboard: https://www.creative-tim.com/product/material-dashboard-react
- Tailwind UI: https://tailwindui.com/components

### 기술 문서
- React Router: https://reactrouter.com/
- Ant Design: https://ant.design/
- Chart.js: https://www.chartjs.org/
- React Hook Form: https://react-hook-form.com/

---

**문서 작성일**: 2026-01-30  
**작성자**: AI Architect  
**버전**: 1.0
