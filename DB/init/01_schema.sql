-- Users 테이블
CREATE TABLE IF NOT EXISTS users (
    employee_no VARCHAR(20) PRIMARY KEY COMMENT '사번 (로그인 ID)',
    password VARCHAR(255) NOT NULL COMMENT '비밀번호 해시',
    name VARCHAR(50) NOT NULL COMMENT '사용자명',
    department VARCHAR(50) COMMENT '소속 부서',
    user_type VARCHAR(20) COMMENT '직원/교수/외부',
    email VARCHAR(100) UNIQUE COMMENT '이메일',
    phone VARCHAR(20) COMMENT '연락처',
    role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT 'USER / ADMIN / SUPER',
    status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED' COMMENT 'REQUESTED / APPROVED / BLOCKED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일'
) COMMENT='사용자 계정 관리';

-- Assets 테이블
CREATE TABLE IF NOT EXISTS assets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '내부 식별자',
    asset_no VARCHAR(50) NOT NULL UNIQUE COMMENT '자산번호 (OCR)',
    old_asset_no VARCHAR(50) COMMENT '구자산번호',
    name VARCHAR(100) NOT NULL COMMENT '물품명 (OCR)',
    model VARCHAR(100) COMMENT '모델명',
    serial_no VARCHAR(100) COMMENT '제조번호',
    spec TEXT COMMENT '규격 (OCR)',
    value DECIMAL(15, 2) COMMENT '자산금액',
    purchase_date DATE COMMENT '취득일자 (OCR)',
    disposed_date DATE COMMENT '불용일자',
    disposed_reason VARCHAR(500) COMMENT '불용 사유',
    status VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL / BROKEN / DISPOSED / LOST',
    business_unit VARCHAR(50) COMMENT '사업장명',
    managing_dept VARCHAR(50) COMMENT '관리부서 (OCR)',
    location VARCHAR(100) COMMENT '비치장소',
    current_user_id VARCHAR(20) COMMENT '현재 사용자 (FK)',
    assigned_at TIMESTAMP COMMENT '배정일자',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
    FOREIGN KEY (current_user_id) REFERENCES users(employee_no) ON DELETE SET NULL
) COMMENT='자산 정보 관리';

-- Asset Images 테이블
CREATE TABLE IF NOT EXISTS asset_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_id BIGINT NOT NULL COMMENT '자산 ID (FK)',
    image_url VARCHAR(255) NOT NULL COMMENT '이미지 URL',
    is_primary BOOLEAN DEFAULT FALSE COMMENT '대표 이미지 여부',
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '업로드 일시',
    FOREIGN KEY (asset_id) REFERENCES assets(id) ON DELETE CASCADE
) COMMENT='자산 이미지 관리';

-- Maintenance History 테이블
CREATE TABLE IF NOT EXISTS maintenance_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_id BIGINT NOT NULL COMMENT '자산 ID (FK)',
    maintenance_date DATE NOT NULL COMMENT '유지/보수 일자',
    reason VARCHAR(100) NOT NULL COMMENT '사유',
    details TEXT COMMENT '상세 내용',
    cost DECIMAL(15, 2) DEFAULT 0 COMMENT '비용',
    performed_by VARCHAR(50) COMMENT '수행자',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
    FOREIGN KEY (asset_id) REFERENCES assets(id) ON DELETE CASCADE
) COMMENT='유지/보수 이력 관리';

-- Asset Transfer History 테이블 (신규 추가)
CREATE TABLE IF NOT EXISTS asset_transfer_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_id BIGINT NOT NULL COMMENT '자산 ID (FK)',
    from_user_id VARCHAR(20) COMMENT '이전 사용자 (FK)',
    to_user_id VARCHAR(20) COMMENT '새 사용자 (FK)',
    transfer_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '이관 일자',
    reason VARCHAR(255) COMMENT '이관 사유',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
    FOREIGN KEY (asset_id) REFERENCES assets(id) ON DELETE CASCADE,
    FOREIGN KEY (from_user_id) REFERENCES users(employee_no) ON DELETE SET NULL,
    FOREIGN KEY (to_user_id) REFERENCES users(employee_no) ON DELETE SET NULL
) COMMENT='자산 사용자 이관 이력';

-- SUPER 관리자 계정은 운영 환경에서 별도로 생성합니다.
-- 공개된 기본 비밀번호를 사용하는 초기 계정은 보안상 생성하지 않습니다.
