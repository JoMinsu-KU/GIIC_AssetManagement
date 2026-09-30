ALTER TABLE asset_loss_requests
    ADD COLUMN remarks TEXT NULL AFTER loss_date;

UPDATE asset_loss_requests
SET remarks = '기존 손망 신청'
WHERE remarks IS NULL;

ALTER TABLE asset_loss_requests
    MODIFY COLUMN remarks TEXT NOT NULL;

ALTER TABLE assets
    ADD COLUMN loss_remarks TEXT NULL AFTER loss_requester_name;
