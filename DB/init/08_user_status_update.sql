-- 1. Modify ENUM to include all potential values (REQUESTED, PENDING, APPROVED, REJECTED, BLOCKED, INACTIVE)
-- This ensures existing data (whether VARCHAR or ENUM) is preserved during the transition.
ALTER TABLE users MODIFY COLUMN status ENUM('REQUESTED', 'PENDING', 'APPROVED', 'REJECTED', 'BLOCKED', 'INACTIVE') NOT NULL DEFAULT 'PENDING';

-- 2. Migrate data
-- Update 'BLOCKED' to 'INACTIVE'
UPDATE users SET status = 'INACTIVE' WHERE status = 'BLOCKED';
-- Update 'REQUESTED' to 'PENDING' (Legacy value from initial schema)
UPDATE users SET status = 'PENDING' WHERE status = 'REQUESTED';

-- 3. Finalize ENUM (Remove deprecated values)
ALTER TABLE users MODIFY COLUMN status ENUM('PENDING', 'APPROVED', 'REJECTED', 'INACTIVE') NOT NULL DEFAULT 'PENDING';
