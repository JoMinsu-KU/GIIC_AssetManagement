-- transfers 테이블에서 toUser 관련 컬럼 삭제
-- to_user 대신 recipient_* 필드를 사용합니다

-- 1. 외래키 삭제 (먼저 실행)
ALTER TABLE transfers DROP FOREIGN KEY FK392lf3bbjrd6ltoktblgtrfiw;

-- 2. to_user 관련 컬럼 삭제
ALTER TABLE transfers DROP COLUMN to_user_id;
ALTER TABLE transfers DROP COLUMN to_user_name;
ALTER TABLE transfers DROP COLUMN to_user_department;
ALTER TABLE transfers DROP COLUMN to_user_contact;
