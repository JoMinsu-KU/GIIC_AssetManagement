-- Asset Schema Redesign Migration Script

-- 1. Assets Table Changes
ALTER TABLE assets
ADD COLUMN actual_user_name VARCHAR(100) COMMENT '실사용자명 (내부/외부 구분없음)',
ADD COLUMN actual_user_department VARCHAR(100) COMMENT '실사용자 소속',
ADD COLUMN actual_user_contact VARCHAR(100) COMMENT '실사용자 연락처',
ADD COLUMN registered_by VARCHAR(20) COMMENT '자산등록자 (FK to users)',
ADD CONSTRAINT fk_assets_registered_by 
    FOREIGN KEY (registered_by) REFERENCES users(employee_no) ON DELETE SET NULL;

-- Migrate existing data for assets
-- Copy current user data to new actual_user_name field
UPDATE assets a
LEFT JOIN users u ON a.current_user_id = u.employee_no
SET a.actual_user_name = u.name,
    a.actual_user_department = u.department
WHERE a.current_user_id IS NOT NULL;

-- Set registered_by to current user as default (best guess)
UPDATE assets a
SET a.registered_by = a.current_user_id
WHERE a.current_user_id IS NOT NULL;


-- 2. Transfers Table Changes
ALTER TABLE transfers
ADD COLUMN from_user_name VARCHAR(100) COMMENT '이전 실사용자명',
ADD COLUMN from_user_department VARCHAR(100) COMMENT '이전 실사용자 소속',
ADD COLUMN to_user_name VARCHAR(100) COMMENT '신규 실사용자명',
ADD COLUMN to_user_department VARCHAR(100) COMMENT '신규 실사용자 소속',
ADD COLUMN to_user_contact VARCHAR(100) COMMENT '신규 실사용자 연락처',
ADD COLUMN requested_by VARCHAR(20) COMMENT '이관요청자 (FK)',
ADD COLUMN processed_by VARCHAR(20) COMMENT '이관처리자 (FK)',
ADD COLUMN status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING / APPROVED / REJECTED',
ADD COLUMN request_date TIMESTAMP NULL COMMENT '요청일시',
ADD COLUMN process_date TIMESTAMP NULL COMMENT '처리일시',
ADD CONSTRAINT fk_transfers_requested_by 
    FOREIGN KEY (requested_by) REFERENCES users(employee_no) ON DELETE SET NULL,
ADD CONSTRAINT fk_transfers_processed_by 
    FOREIGN KEY (processed_by) REFERENCES users(employee_no) ON DELETE SET NULL;

-- Migrate existing data for transfers
UPDATE transfers t
LEFT JOIN users u1 ON t.from_user_id = u1.employee_no
LEFT JOIN users u2 ON t.to_user_id = u2.employee_no
SET t.from_user_name = u1.name,
    t.from_user_department = u1.department,
    t.to_user_name = u2.name,
    t.to_user_department = u2.department,
    t.requested_by = t.to_user_id, -- Assume receiver requested it
    t.status = 'APPROVED',         -- Existing records are considered approved
    t.process_date = t.transfer_date,
    t.request_date = t.transfer_date;


-- 3. Maintenance Table Changes
ALTER TABLE maintenance
ADD COLUMN registered_by VARCHAR(20) COMMENT '등록자 (시스템 사용자)',
ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'REPAIR' COMMENT 'REPAIR / INSPECTION / UPGRADE / OTHER',
ADD CONSTRAINT fk_maintenance_registered_by 
    FOREIGN KEY (registered_by) REFERENCES users(employee_no) ON DELETE RESTRICT;

-- Migrate existing data for maintenance
-- Set registered_by to 'admin' as fallback
UPDATE maintenance SET registered_by = 'admin';
