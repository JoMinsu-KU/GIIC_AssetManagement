-- Preserve existing values and allow the new unassigned asset state.
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
    'LOST',
    'UNUSED'
) NOT NULL DEFAULT 'NORMAL';
