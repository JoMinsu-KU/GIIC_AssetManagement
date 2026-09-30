# React 프론트엔드 개발 가이드

## 목차
1. [프로젝트 개요](#1-프로젝트-개요)
2. [페이지 구조 및 라우팅](#2-페이지-구조-및-라우팅)
3. [API 엔드포인트 및 데이터 구조](#3-api-엔드포인트-및-데이터-구조)
4. [컴포넌트 구조 제안](#4-컴포넌트-구조-제안)
5. [상태 관리](#5-상태-관리)
6. [API 클라이언트 설정](#6-api-클라이언트-설정)
7. [구현 우선순위](#7-구현-우선순위)

---

## 1. 프로젝트 개요

### 1.1 기술 스택
- **프레임워크**: React 18+
- **라우팅**: React Router v6
- **HTTP 클라이언트**: Axios
- **상태 관리**: Context API / React Query (권장)
- **UI 라이브러리**: Material-UI / Ant Design (선택)
- **폼 관리**: React Hook Form + Yup
- **날짜 처리**: date-fns / dayjs

### 1.2 기본 정보
- **백엔드 API 베이스 URL**: `http://localhost:8080/api`
- **인증 방식**: JWT (Access Token + Refresh Token)
- **응답 형식**: 모든 API는 공통 `ApiResponse` 래퍼 사용
- **페이지네이션**: Spring Data의 Pageable 사용 (0-based → 1-based)

---

## 2. 페이지 구조 및 라우팅

### 2.1 라우팅 구조

```javascript
// src/routes/index.js
import { Routes, Route, Navigate } from 'react-router-dom';

// Public Routes (인증 불필요)
/login                    // 로그인
/signup                   // 회원가입

// Protected Routes (인증 필요)
/                         // 대시보드 (자산 요약)
/assets                   // 자산 목록
/assets/new               // 자산 등록
/assets/:id               // 자산 상세
/assets/:id/edit          // 자산 수정

/transfers                // 이관 목록
/transfers/new            // 이관 요청

/maintenance              // 유지보수 이력 (전체)

// Admin Routes (ADMIN, SUPER만 접근)
/users                    // 사용자 목록
/users/:employeeNo        // 사용자 상세/수정

// Profile
/profile                  // 내 정보
```

### 2.2 권한별 페이지 접근

| 페이지 | USER | ADMIN | SUPER |
|--------|------|-------|-------|
| 로그인/회원가입 | ✓ | ✓ | ✓ |
| 대시보드 | ✓ | ✓ | ✓ |
| 자산 관리 | ✓ | ✓ | ✓ |
| 이관 관리 | ✓ | ✓ | ✓ |
| 유지보수 관리 | ✓ | ✓ | ✓ |
| 사용자 관리 | ✗ | ✓ | ✓ |
| 모니터링 | ✗ | ✗ | ✓ |

### 2.3 Protected Route 컴포넌트

```javascript
// src/components/auth/ProtectedRoute.js
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';

const ProtectedRoute = ({ children, roles = [] }) => {
  const { user, isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (roles.length > 0 && !roles.includes(user?.role)) {
    return <Navigate to="/" replace />;
  }

  return children;
};

export default ProtectedRoute;
```

---

## 3. API 엔드포인트 및 데이터 구조

### 3.1 인증 API

#### 3.1.1 로그인
```javascript
// POST /api/auth/login
const login = async (employeeNo, password) => {
  const response = await client.post('/auth/login', {
    employeeNo,  // 사번 (주의: employeeNo, username 아님)
    password
  });
  return response.data;
};

// Response
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "dGhpc...",
    "role": "USER",  // USER, ADMIN, SUPER
    "name": "홍길동"
  },
  "message": "요청이 성공적으로 처리되었습니다."
}
```

#### 3.1.2 회원가입
```javascript
// POST /api/auth/signup
const signup = async (userData) => {
  await client.post('/auth/signup', {
    employeeNo: "20231234",
    password: "password123",
    name: "홍길동",
    department: "개발팀",
    email: "hong@example.com"
  });
};

// 회원가입 후 상태는 PENDING이며, ADMIN 승인 필요
```

#### 3.1.3 토큰 갱신
```javascript
// POST /api/auth/refresh
const refreshToken = async (refreshToken) => {
  const response = await client.post('/auth/refresh', {
    refreshToken
  });
  return response.data;
};
```

#### 3.1.4 로그아웃
```javascript
// POST /api/auth/logout
// Header: Authorization: Bearer {accessToken}
const logout = async () => {
  await client.post('/auth/logout');
};
```

### 3.2 사용자 관리 API

#### 3.2.1 사용자 목록 조회 (ADMIN+)
```javascript
// GET /api/users?page=1&size=20&role=USER&status=APPROVED&keyword=홍길동
const getUserList = async (params) => {
  const response = await client.get('/users', { params });
  return response.data;
};

// Response
{
  "success": true,
  "data": {
    "content": [
      {
        "employeeNo": "20231234",
        "name": "홍길동",
        "department": "개발팀",
        "email": "hong@example.com",
        "role": "USER",        // USER, ADMIN, SUPER
        "status": "APPROVED",  // APPROVED, BLOCKED, REJECTED, PENDING
        "createdAt": "2024-01-01T10:00:00"
      }
    ],
    "page_info": {
      "page": 1,
      "size": 20,
      "total_elements": 150,
      "total_pages": 8,
      "first": true,
      "last": false
    }
  }
}
```

#### 3.2.2 사용자 상태 수정 (ADMIN+)
```javascript
// PUT /api/users/{employeeNo}/status
const updateUserStatus = async (employeeNo, data) => {
  await client.put(`/users/${employeeNo}/status`, {
    status: "APPROVED",  // APPROVED, BLOCKED, REJECTED
    role: "USER"         // USER, ADMIN
  });
};
```

#### 3.2.3 내 정보 조회
```javascript
// GET /api/users/me
const getMyInfo = async () => {
  const response = await client.get('/users/me');
  return response.data;
};
```

### 3.3 자산 관리 API

#### 3.3.1 자산 등록
```javascript
// POST /api/assets
const createAsset = async (assetData) => {
  await client.post('/assets', {
    assetNo: "2024-0001",
    name: "MacBook Pro M3",
    model: "A2992",
    spec: "M3 Pro, 18GB RAM, 512GB SSD",
    purchaseDate: "2024-01-15",  // YYYY-MM-DD
    value: 2990000,
    managingDept: "IT지원팀",
    location: "A동 301호",
    status: "NORMAL",  // NORMAL, DISPOSED, REPAIRING, LOST
    // 실사용자 정보 (외부인 포함 가능)
    actualUserName: "홍길동",
    actualUserDepartment: "개발팀",
    actualUserContact: "010-1234-5678"
  });
};
```

#### 3.3.2 자산 목록 조회 (검색/필터)
```javascript
// GET /api/assets?page=1&size=20&keyword=맥북&status=NORMAL&dept=IT지원팀
const getAssetList = async (params) => {
  const response = await client.get('/assets', {
    params: {
      page: 1,
      size: 20,
      keyword: "맥북",           // 자산번호, 자산명 검색
      status: "NORMAL",         // AssetStatus enum
      dept: "IT지원팀",         // 관리 부서
      user_id: "20231234",      // 등록자 사번
      actualUserName: "홍길동"  // 실사용자 이름
    }
  });
  return response.data;
};

// Response
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "assetNo": "2024-0001",
        "name": "MacBook Pro M3",
        "model": "A2992",
        "spec": "M3 Pro, 18GB RAM, 512GB SSD",
        "purchaseDate": "2024-01-15",
        "value": 2990000,
        "managingDept": "IT지원팀",
        "location": "A동 301호",
        "status": "NORMAL",
        "actualUserName": "홍길동",
        "actualUserDepartment": "개발팀",
        "actualUserContact": "010-1234-5678",
        "registeredByName": "관리자",
        "registeredById": "admin001",
        "images": [
          {
            "id": 1,
            "imageUrl": "https://storage.../image.jpg",
            "isPrimary": true
          }
        ]
      }
    ],
    "page_info": { ... }
  }
}
```

#### 3.3.3 자산 상세 조회
```javascript
// GET /api/assets/{assetId}
const getAssetDetail = async (assetId) => {
  const response = await client.get(`/assets/${assetId}`);
  return response.data;
};
```

#### 3.3.4 자산 정보 수정
```javascript
// PUT /api/assets/{assetId}
const updateAsset = async (assetId, data) => {
  await client.put(`/assets/${assetId}`, {
    name: "MacBook Pro M3 (수정)",
    location: "A동 302호",
    status: "REPAIRING",
    actualUserName: "김철수",
    actualUserDepartment: "디자인팀",
    actualUserContact: "010-9876-5432"
  });
};
```

#### 3.3.5 자산 삭제
```javascript
// DELETE /api/assets/{assetId}
const deleteAsset = async (assetId) => {
  await client.delete(`/assets/${assetId}`);
};
```

#### 3.3.6 자산 이미지 업로드
```javascript
// POST /api/assets/{assetId}/images
// Content-Type: multipart/form-data
const uploadImage = async (assetId, file, isPrimary) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('is_primary', isPrimary);

  await client.post(`/assets/${assetId}/images`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
};
```

#### 3.3.7 OCR 자산 정보 추출
```javascript
// POST /api/assets/ocr
const ocrImage = async (file) => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await client.post('/assets/ocr', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;  // AssetRequest 형태로 반환
};

// Response - 자산 등록 폼에 자동 채움
{
  "success": true,
  "data": {
    "assetNo": "2024-0001",
    "name": "Samsung Monitor",
    "model": null,
    "spec": "27 inch, 4K",
    "purchaseDate": "2023-12-20",
    "value": null,
    "managingDept": "IT지원팀",
    "location": null,
    "status": "NORMAL",
    "actualUserName": null,
    "actualUserDepartment": null,
    "actualUserContact": null
  }
}
```

### 3.4 이관 관리 API

#### 3.4.1 이관 요청
```javascript
// POST /api/transfers
const createTransfer = async (transferData) => {
  await client.post('/transfers', {
    assetId: 101,
    toUserName: "김철수",           // 받는 사람 이름 (필수)
    toUserDepartment: "디자인팀",
    toUserContact: "010-9876-5432",
    reason: "부서 이동으로 인한 인계"
  });
};
```

#### 3.4.2 이관 목록 조회
```javascript
// GET /api/transfers?type=received&status=PENDING&page=1&size=20
const getTransferList = async (params) => {
  const response = await client.get('/transfers', {
    params: {
      type: 'received',  // sent (내가 요청한), received (내가 받을)
      status: 'PENDING', // PENDING, APPROVED, REJECTED
      page: 1,
      size: 20
    }
  });
  return response.data;
};

// Response
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "assetId": 101,
        "assetName": "MacBook Pro M3",
        "assetNo": "2024-0001",
        "fromUserName": "홍길동",
        "fromUserDepartment": "개발팀",
        "toUserName": "김철수",
        "toUserDepartment": "디자인팀",
        "toUserContact": "010-9876-5432",
        "requestedByName": "홍길동",
        "requestedById": "20231234",
        "processedByName": null,
        "processedById": null,
        "status": "PENDING",
        "reason": "부서 이동으로 인한 인계",
        "requestDate": "2024-01-15T10:00:00",
        "processDate": null
      }
    ],
    "page_info": { ... }
  }
}
```

#### 3.4.3 이관 승인
```javascript
// POST /api/transfers/{transferId}/approve
const approveTransfer = async (transferId) => {
  await client.post(`/transfers/${transferId}/approve`);
};
```

#### 3.4.4 이관 거절
```javascript
// POST /api/transfers/{transferId}/reject
const rejectTransfer = async (transferId) => {
  await client.post(`/transfers/${transferId}/reject`);
};
```

### 3.5 유지보수 이력 API

#### 3.5.1 유지보수 기록 등록
```javascript
// POST /api/assets/{assetId}/maintenance
const createMaintenance = async (assetId, maintenanceData) => {
  await client.post(`/assets/${assetId}/maintenance`, {
    maintenanceDate: "2024-02-01",
    type: "REPAIR",  // REPAIR, CHECK, UPGRADE
    reason: "화면 깜빡임 증상",
    details: "패널 교체 진행",
    cost: 150000,
    performedBy: "삼성전자서비스"
  });
};
```

#### 3.5.2 유지보수 목록 조회
```javascript
// GET /api/assets/{assetId}/maintenance?page=1&size=20
const getMaintenanceList = async (assetId, params) => {
  const response = await client.get(`/assets/${assetId}/maintenance`, {
    params
  });
  return response.data;
};

// Response
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "assetId": 101,
        "maintenanceDate": "2024-02-01",
        "type": "REPAIR",
        "reason": "화면 깜빡임 증상",
        "details": "패널 교체 진행",
        "cost": 150000,
        "performedBy": "삼성전자서비스",
        "registeredByName": "홍길동",
        "registeredById": "20231234"
      }
    ],
    "page_info": { ... }
  }
}
```

#### 3.5.3 유지보수 기록 수정
```javascript
// PUT /api/maintenance/{maintenanceId}
const updateMaintenance = async (maintenanceId, data) => {
  await client.put(`/maintenance/${maintenanceId}`, data);
};
```

#### 3.5.4 유지보수 기록 삭제
```javascript
// DELETE /api/maintenance/{maintenanceId}
const deleteMaintenance = async (maintenanceId) => {
  await client.delete(`/maintenance/${maintenanceId}`);
};
```

### 3.6 Enum 타입 정의

```javascript
// src/constants/enums.js

export const Role = {
  USER: 'USER',
  ADMIN: 'ADMIN',
  SUPER: 'SUPER'
};

export const UserStatus = {
  APPROVED: 'APPROVED',
  BLOCKED: 'BLOCKED',
  REJECTED: 'REJECTED',
  PENDING: 'PENDING'
};

export const AssetStatus = {
  NORMAL: 'NORMAL',
  DISPOSED: 'DISPOSED',
  REPAIRING: 'REPAIRING',
  LOST: 'LOST'
};

export const TransferStatus = {
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED'
};

export const MaintenanceType = {
  REPAIR: 'REPAIR',
  CHECK: 'CHECK',
  UPGRADE: 'UPGRADE'
};

// 한글 레이블
export const RoleLabels = {
  USER: '사용자',
  ADMIN: '관리자',
  SUPER: '슈퍼관리자'
};

export const UserStatusLabels = {
  APPROVED: '승인됨',
  BLOCKED: '차단됨',
  REJECTED: '거절됨',
  PENDING: '대기중'
};

export const AssetStatusLabels = {
  NORMAL: '정상',
  DISPOSED: '불용',
  REPAIRING: '수리중',
  LOST: '분실'
};

export const TransferStatusLabels = {
  PENDING: '대기중',
  APPROVED: '승인됨',
  REJECTED: '거절됨'
};

export const MaintenanceTypeLabels = {
  REPAIR: '수리',
  CHECK: '점검',
  UPGRADE: '업그레이드'
};
```

---

## 4. 컴포넌트 구조 제안

### 4.1 프로젝트 디렉토리 구조

```
src/
├── api/
│   ├── client.js           // Axios 인스턴스
│   ├── config.js           // API 설정
│   ├── auth.js             // 인증 API
│   ├── users.js            // 사용자 API
│   ├── assets.js           // 자산 API
│   ├── transfers.js        // 이관 API
│   └── maintenance.js      // 유지보수 API
├── components/
│   ├── auth/
│   │   ├── LoginForm.js
│   │   ├── SignupForm.js
│   │   └── ProtectedRoute.js
│   ├── layout/
│   │   ├── Layout.js       // 전체 레이아웃
│   │   ├── Header.js
│   │   ├── Sidebar.js
│   │   └── Footer.js
│   ├── common/
│   │   ├── Table.js        // 공통 테이블
│   │   ├── Pagination.js   // 페이지네이션
│   │   ├── SearchBar.js
│   │   ├── StatusBadge.js
│   │   ├── ConfirmDialog.js
│   │   └── LoadingSpinner.js
│   ├── assets/
│   │   ├── AssetList.js
│   │   ├── AssetDetail.js
│   │   ├── AssetForm.js
│   │   ├── AssetCard.js
│   │   ├── ImageUpload.js
│   │   └── OCRScanner.js
│   ├── users/
│   │   ├── UserList.js
│   │   ├── UserDetail.js
│   │   └── UserStatusModal.js
│   ├── transfers/
│   │   ├── TransferList.js
│   │   ├── TransferForm.js
│   │   └── TransferCard.js
│   └── maintenance/
│       ├── MaintenanceList.js
│       ├── MaintenanceForm.js
│       └── MaintenanceCard.js
├── contexts/
│   └── AuthContext.js      // 인증 상태 관리
├── hooks/
│   ├── useAuth.js
│   ├── useAssets.js
│   ├── useUsers.js
│   ├── useTransfers.js
│   └── useMaintenance.js
├── pages/
│   ├── Login.js
│   ├── Signup.js
│   ├── Dashboard.js
│   ├── AssetListPage.js
│   ├── AssetDetailPage.js
│   ├── AssetFormPage.js
│   ├── TransferListPage.js
│   ├── TransferFormPage.js
│   ├── UserListPage.js
│   ├── UserDetailPage.js
│   └── ProfilePage.js
├── routes/
│   └── index.js            // 라우팅 설정
├── constants/
│   └── enums.js            // Enum 상수
├── utils/
│   ├── formatters.js       // 날짜, 숫자 포맷
│   └── validators.js       // 유효성 검증
├── App.js
└── index.js
```

### 4.2 주요 컴포넌트 예시

#### 4.2.1 Layout 컴포넌트
```javascript
// src/components/layout/Layout.js
import { Outlet } from 'react-router-dom';
import Header from './Header';
import Sidebar from './Sidebar';
import { useAuth } from '../../contexts/AuthContext';

const Layout = () => {
  const { user } = useAuth();

  return (
    <div className="layout">
      <Header user={user} />
      <div className="layout-content">
        <Sidebar role={user?.role} />
        <main className="main-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default Layout;
```

#### 4.2.2 자산 목록 컴포넌트
```javascript
// src/components/assets/AssetList.js
import { useState, useEffect } from 'react';
import { getAssetList } from '../../api/assets';
import Table from '../common/Table';
import Pagination from '../common/Pagination';
import SearchBar from '../common/SearchBar';
import StatusBadge from '../common/StatusBadge';

const AssetList = () => {
  const [assets, setAssets] = useState([]);
  const [pageInfo, setPageInfo] = useState({});
  const [filters, setFilters] = useState({
    page: 1,
    size: 20,
    keyword: '',
    status: '',
    dept: ''
  });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchAssets();
  }, [filters]);

  const fetchAssets = async () => {
    try {
      setLoading(true);
      const response = await getAssetList(filters);
      setAssets(response.data.content);
      setPageInfo(response.data.page_info);
    } catch (error) {
      console.error('Failed to fetch assets:', error);
    } finally {
      setLoading(false);
    }
  };

  const columns = [
    { key: 'assetNo', label: '자산번호' },
    { key: 'name', label: '자산명' },
    { 
      key: 'status', 
      label: '상태',
      render: (value) => <StatusBadge status={value} type="asset" />
    },
    { key: 'actualUserName', label: '사용자' },
    { key: 'managingDept', label: '관리부서' },
    { key: 'location', label: '위치' }
  ];

  return (
    <div>
      <SearchBar 
        onSearch={(keyword) => setFilters({ ...filters, keyword, page: 1 })}
        filters={filters}
        onFilterChange={(newFilters) => setFilters({ ...filters, ...newFilters, page: 1 })}
      />
      <Table 
        columns={columns} 
        data={assets} 
        loading={loading}
        onRowClick={(asset) => navigate(`/assets/${asset.id}`)}
      />
      <Pagination 
        pageInfo={pageInfo}
        onPageChange={(page) => setFilters({ ...filters, page })}
      />
    </div>
  );
};

export default AssetList;
```

#### 4.2.3 자산 등록 폼 (OCR 포함)
```javascript
// src/components/assets/AssetForm.js
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import { createAsset, ocrImage } from '../../api/assets';
import { AssetStatus } from '../../constants/enums';
import OCRScanner from './OCRScanner';

const schema = yup.object({
  assetNo: yup.string().required('자산번호는 필수입니다'),
  name: yup.string().required('자산명은 필수입니다'),
  status: yup.string().required('상태는 필수입니다'),
  purchaseDate: yup.date().nullable(),
  // ... 기타 필드
});

const AssetForm = ({ onSubmit }) => {
  const [ocrLoading, setOcrLoading] = useState(false);
  const { register, handleSubmit, setValue, formState: { errors } } = useForm({
    resolver: yupResolver(schema),
    defaultValues: {
      status: AssetStatus.NORMAL
    }
  });

  const handleOCR = async (file) => {
    try {
      setOcrLoading(true);
      const response = await ocrImage(file);
      const ocrData = response.data;
      
      // OCR 결과를 폼에 자동 채움
      Object.keys(ocrData).forEach(key => {
        if (ocrData[key]) {
          setValue(key, ocrData[key]);
        }
      });
    } catch (error) {
      console.error('OCR failed:', error);
      alert('OCR 처리에 실패했습니다.');
    } finally {
      setOcrLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <OCRScanner onScan={handleOCR} loading={ocrLoading} />
      
      <div>
        <label>자산번호 *</label>
        <input {...register('assetNo')} />
        {errors.assetNo && <span>{errors.assetNo.message}</span>}
      </div>

      <div>
        <label>자산명 *</label>
        <input {...register('name')} />
        {errors.name && <span>{errors.name.message}</span>}
      </div>

      <div>
        <label>상태 *</label>
        <select {...register('status')}>
          {Object.entries(AssetStatus).map(([key, value]) => (
            <option key={key} value={value}>{AssetStatusLabels[value]}</option>
          ))}
        </select>
      </div>

      <div>
        <label>실사용자 이름</label>
        <input {...register('actualUserName')} />
      </div>

      <div>
        <label>실사용자 부서</label>
        <input {...register('actualUserDepartment')} />
      </div>

      <div>
        <label>실사용자 연락처</label>
        <input {...register('actualUserContact')} />
      </div>

      {/* ... 기타 필드 */}

      <button type="submit">등록</button>
    </form>
  );
};

export default AssetForm;
```

---

## 5. 상태 관리

### 5.1 AuthContext (인증 상태)

```javascript
// src/contexts/AuthContext.js
import { createContext, useContext, useState, useEffect } from 'react';
import { login as apiLogin, logout as apiLogout, refreshToken as apiRefresh } from '../api/auth';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // 페이지 로드 시 저장된 토큰 확인
    const token = localStorage.getItem('accessToken');
    if (token) {
      // TODO: 토큰 유효성 검증 또는 /users/me 호출
      loadUserInfo();
    } else {
      setLoading(false);
    }
  }, []);

  const loadUserInfo = async () => {
    try {
      const response = await getMyInfo();
      setUser(response.data);
      setIsAuthenticated(true);
    } catch (error) {
      logout();
    } finally {
      setLoading(false);
    }
  };

  const login = async (employeeNo, password) => {
    const response = await apiLogin(employeeNo, password);
    const { accessToken, refreshToken, role, name } = response.data;

    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);

    setUser({ employeeNo, role, name });
    setIsAuthenticated(true);

    return response.data;
  };

  const logout = async () => {
    try {
      await apiLogout();
    } catch (error) {
      console.error('Logout failed:', error);
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      setUser(null);
      setIsAuthenticated(false);
    }
  };

  const refreshAccessToken = async () => {
    try {
      const currentRefreshToken = localStorage.getItem('refreshToken');
      const response = await apiRefresh(currentRefreshToken);
      const { accessToken, refreshToken: newRefreshToken } = response.data;

      localStorage.setItem('accessToken', accessToken);
      if (newRefreshToken) {
        localStorage.setItem('refreshToken', newRefreshToken);
      }

      return accessToken;
    } catch (error) {
      logout();
      throw error;
    }
  };

  return (
    <AuthContext.Provider value={{
      user,
      isAuthenticated,
      loading,
      login,
      logout,
      refreshAccessToken
    }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};
```

### 5.2 React Query 사용 (권장)

```javascript
// src/hooks/useAssets.js
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getAssetList, getAssetDetail, createAsset, updateAsset, deleteAsset } from '../api/assets';

export const useAssets = (filters) => {
  return useQuery({
    queryKey: ['assets', filters],
    queryFn: () => getAssetList(filters),
    keepPreviousData: true,
  });
};

export const useAsset = (assetId) => {
  return useQuery({
    queryKey: ['asset', assetId],
    queryFn: () => getAssetDetail(assetId),
    enabled: !!assetId,
  });
};

export const useCreateAsset = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: createAsset,
    onSuccess: () => {
      queryClient.invalidateQueries(['assets']);
    },
  });
};

export const useUpdateAsset = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ assetId, data }) => updateAsset(assetId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries(['assets']);
      queryClient.invalidateQueries(['asset', variables.assetId]);
    },
  });
};

export const useDeleteAsset = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: deleteAsset,
    onSuccess: () => {
      queryClient.invalidateQueries(['assets']);
    },
  });
};
```

---

## 6. API 클라이언트 설정

### 6.1 향상된 Axios 클라이언트

```javascript
// src/api/client.js
import axios from 'axios';
import { API_BASE_URL } from './config';

const client = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

// Request Interceptor - 토큰 추가
client.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response Interceptor - 에러 처리 및 토큰 갱신
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

client.interceptors.response.use(
  (response) => {
    // ApiResponse 래퍼 자동 해제
    if (response.data?.success !== undefined) {
      return response.data;
    }
    return response;
  },
  async (error) => {
    const originalRequest = error.config;

    // 401 Unauthorized - 토큰 갱신 시도
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // 이미 갱신 중이면 큐에 추가
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(token => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return client(originalRequest);
        }).catch(err => {
          return Promise.reject(err);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) {
          throw new Error('No refresh token');
        }

        const response = await axios.post(`${API_BASE_URL}/auth/refresh`, {
          refreshToken
        });

        const { accessToken, refreshToken: newRefreshToken } = response.data.data;
        
        localStorage.setItem('accessToken', accessToken);
        if (newRefreshToken) {
          localStorage.setItem('refreshToken', newRefreshToken);
        }

        client.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        
        processQueue(null, accessToken);
        
        return client(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        
        // 토큰 갱신 실패 - 로그아웃 처리
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        window.location.href = '/login';
        
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    // 에러 메시지 추출
    const errorMessage = error.response?.data?.error?.message 
      || error.response?.data?.message 
      || error.message 
      || '요청 처리에 실패했습니다.';

    return Promise.reject({
      ...error,
      message: errorMessage
    });
  }
);

export default client;
```

### 6.2 API 모듈 예시

```javascript
// src/api/assets.js
import client from './client';

export const getAssetList = (params) => {
  return client.get('/assets', { params });
};

export const getAssetDetail = (assetId) => {
  return client.get(`/assets/${assetId}`);
};

export const createAsset = (data) => {
  return client.post('/assets', data);
};

export const updateAsset = (assetId, data) => {
  return client.put(`/assets/${assetId}`, data);
};

export const deleteAsset = (assetId) => {
  return client.delete(`/assets/${assetId}`);
};

export const uploadImage = (assetId, file, isPrimary) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('is_primary', isPrimary);

  return client.post(`/assets/${assetId}/images`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
};

export const ocrImage = (file) => {
  const formData = new FormData();
  formData.append('file', file);

  return client.post('/assets/ocr', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
};
```

```javascript
// src/api/auth.js (업데이트)
import client from './client';

export const login = async (employeeNo, password) => {
  const response = await client.post('/auth/login', { 
    employeeNo,  // 주의: employeeNo 사용
    password 
  });
  
  const { accessToken, refreshToken } = response.data;
  
  if (accessToken) {
    localStorage.setItem('accessToken', accessToken);
  }
  if (refreshToken) {
    localStorage.setItem('refreshToken', refreshToken);
  }
  
  return response;
};

export const signup = (userData) => {
  return client.post('/auth/signup', userData);
};

export const logout = async () => {
  try {
    await client.post('/auth/logout');
  } finally {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
  }
};

export const refreshToken = (refreshToken) => {
  return client.post('/auth/refresh', { refreshToken });
};
```

```javascript
// src/api/users.js
import client from './client';

export const getUserList = (params) => {
  return client.get('/users', { params });
};

export const updateUserStatus = (employeeNo, data) => {
  return client.put(`/users/${employeeNo}/status`, data);
};

export const getMyInfo = () => {
  return client.get('/users/me');
};
```

```javascript
// src/api/transfers.js
import client from './client';

export const getTransferList = (params) => {
  return client.get('/transfers', { params });
};

export const createTransfer = (data) => {
  return client.post('/transfers', data);
};

export const approveTransfer = (transferId) => {
  return client.post(`/transfers/${transferId}/approve`);
};

export const rejectTransfer = (transferId) => {
  return client.post(`/transfers/${transferId}/reject`);
};
```

```javascript
// src/api/maintenance.js
import client from './client';

export const getMaintenanceList = (assetId, params) => {
  return client.get(`/assets/${assetId}/maintenance`, { params });
};

export const createMaintenance = (assetId, data) => {
  return client.post(`/assets/${assetId}/maintenance`, data);
};

export const updateMaintenance = (maintenanceId, data) => {
  return client.put(`/maintenance/${maintenanceId}`, data);
};

export const deleteMaintenance = (maintenanceId) => {
  return client.delete(`/maintenance/${maintenanceId}`);
};
```

---

## 7. 구현 우선순위

### Phase 1: 인증 및 기본 레이아웃
1. 로그인/회원가입 페이지
2. AuthContext 및 ProtectedRoute 구현
3. Layout (Header, Sidebar) 구현
4. 대시보드 (간단한 통계)

### Phase 2: 자산 관리 (핵심 기능)
1. 자산 목록 조회 (검색, 필터, 페이지네이션)
2. 자산 상세 조회
3. 자산 등록 (OCR 포함)
4. 자산 수정/삭제
5. 이미지 업로드

### Phase 3: 이관 및 유지보수
1. 이관 요청 목록
2. 이관 요청 생성
3. 이관 승인/거절
4. 유지보수 이력 조회
5. 유지보수 기록 등록/수정

### Phase 4: 관리자 기능
1. 사용자 목록 조회
2. 사용자 승인/권한 관리
3. 대시보드 통계 강화

### Phase 5: 최적화 및 개선
1. React Query 도입
2. 에러 바운더리
3. 로딩 상태 개선
4. 반응형 디자인
5. 접근성 개선

---

## 8. 주요 고려사항

### 8.1 API 응답 처리
- 모든 API는 `ApiResponse` 래퍼로 감싸져 있음
- Axios 인터셉터에서 자동으로 `response.data`를 반환하도록 설정
- 에러 처리 시 `error.response.data.error.message` 확인

### 8.2 페이지네이션
- Spring Data는 0-based이지만, API는 1-based로 변환됨
- `page_info.page`는 1부터 시작
- 요청 시에도 `page=1`로 전송

### 8.3 날짜 처리
- 백엔드는 `LocalDate` (YYYY-MM-DD) 및 `LocalDateTime` (ISO 8601) 사용
- `date-fns` 또는 `dayjs` 사용 권장
- 입력 폼에서는 HTML5 date input 활용

### 8.4 파일 업로드
- `Content-Type: multipart/form-data` 사용
- FormData 객체로 전송
- 이미지 업로드는 자산 등록 후 별도로 수행

### 8.5 보안
- 토큰은 localStorage에 저장 (httpOnly 쿠키 고려 가능)
- XSS 방지를 위해 사용자 입력 sanitize
- HTTPS 사용 필수 (프로덕션)

### 8.6 에러 처리
```javascript
// src/utils/errorHandler.js
export const handleApiError = (error) => {
  if (error.response) {
    // 서버 응답 있음
    const { status, data } = error.response;
    
    switch (status) {
      case 400:
        return data.error?.message || '잘못된 요청입니다.';
      case 401:
        return '인증에 실패했습니다. 다시 로그인해주세요.';
      case 403:
        return '접근 권한이 없습니다.';
      case 404:
        return '요청한 리소스를 찾을 수 없습니다.';
      case 409:
        return '이미 존재하는 데이터입니다.';
      case 500:
        return '서버 오류가 발생했습니다.';
      default:
        return data.error?.message || '요청 처리에 실패했습니다.';
    }
  } else if (error.request) {
    // 요청은 전송했으나 응답 없음
    return '서버와 통신할 수 없습니다.';
  } else {
    // 요청 설정 중 오류
    return error.message || '알 수 없는 오류가 발생했습니다.';
  }
};
```

### 8.7 Formatter 유틸리티
```javascript
// src/utils/formatters.js
import { format, parseISO } from 'date-fns';
import { ko } from 'date-fns/locale';

export const formatDate = (dateString) => {
  if (!dateString) return '-';
  const date = typeof dateString === 'string' ? parseISO(dateString) : dateString;
  return format(date, 'yyyy-MM-dd', { locale: ko });
};

export const formatDateTime = (dateString) => {
  if (!dateString) return '-';
  const date = typeof dateString === 'string' ? parseISO(dateString) : dateString;
  return format(date, 'yyyy-MM-dd HH:mm:ss', { locale: ko });
};

export const formatCurrency = (value) => {
  if (!value) return '-';
  return new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: 'KRW'
  }).format(value);
};

export const formatNumber = (value) => {
  if (!value) return '-';
  return new Intl.NumberFormat('ko-KR').format(value);
};
```

---

## 9. 참고 문서

- [API 명세서](./api-specification.md)
- [기능 정의서](../기능정의/function.md)
- [백엔드 실행 가이드](./backend-run-guide.md)

---

## 10. 체크리스트

### 개발 환경 설정
- [ ] React 프로젝트 생성
- [ ] 필요한 패키지 설치 (axios, react-router-dom, react-hook-form, etc.)
- [ ] `.env` 파일 설정 (REACT_APP_API_BASE_URL)
- [ ] ESLint, Prettier 설정

### 인증 구현
- [ ] AuthContext 구현
- [ ] ProtectedRoute 구현
- [ ] 로그인 페이지
- [ ] 회원가입 페이지
- [ ] 토큰 갱신 로직

### API 클라이언트
- [ ] Axios 인스턴스 설정
- [ ] Request/Response 인터셉터
- [ ] API 모듈 구현 (auth, users, assets, transfers, maintenance)

### 레이아웃
- [ ] Layout 컴포넌트
- [ ] Header 컴포넌트
- [ ] Sidebar 컴포넌트 (권한별 메뉴)
- [ ] 반응형 디자인

### 자산 관리
- [ ] 자산 목록 페이지 (검색, 필터, 페이지네이션)
- [ ] 자산 상세 페이지
- [ ] 자산 등록 페이지 (OCR 포함)
- [ ] 자산 수정 페이지
- [ ] 이미지 업로드 기능

### 이관 관리
- [ ] 이관 목록 페이지
- [ ] 이관 요청 폼
- [ ] 이관 승인/거절 기능

### 유지보수 관리
- [ ] 유지보수 이력 목록
- [ ] 유지보수 기록 등록
- [ ] 유지보수 기록 수정/삭제

### 사용자 관리 (ADMIN+)
- [ ] 사용자 목록 페이지
- [ ] 사용자 승인/권한 관리

### 공통 컴포넌트
- [ ] Table 컴포넌트
- [ ] Pagination 컴포넌트
- [ ] SearchBar 컴포넌트
- [ ] StatusBadge 컴포넌트
- [ ] ConfirmDialog 컴포넌트
- [ ] LoadingSpinner 컴포넌트

### 테스트 및 최적화
- [ ] 단위 테스트
- [ ] 통합 테스트
- [ ] 성능 최적화 (React.memo, useMemo, useCallback)
- [ ] 코드 스플리팅
- [ ] 에러 바운더리

---

**작성일**: 2026-01-30  
**작성자**: Architect Mode  
**버전**: 1.0
