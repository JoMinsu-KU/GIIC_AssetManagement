-- Add the LOST status while preserving every existing asset status value.
ALTER TABLE assets
MODIFY COLUMN status ENUM(
    'NORMAL',
    'ABNORMAL',
    'DISPOSED',
    'REPAIRING',
    'TRANSFER_PENDING',
    'DISPOSAL_PENDING',
    'DISPOSAL_REJECTED',
    'LOSS_PENDING',
    'LOST'
) NOT NULL DEFAULT 'NORMAL';
