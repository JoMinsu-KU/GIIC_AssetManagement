-- Existing installations: require a transfer destination for every transfer record.
-- The update keeps this migration safe if legacy rows contain a null or blank value.
UPDATE transfers
SET transfer_location = '기존 이관 기록 - 장소 미입력'
WHERE transfer_location IS NULL OR TRIM(transfer_location) = '';

ALTER TABLE transfers
MODIFY COLUMN transfer_location VARCHAR(255) NOT NULL;
