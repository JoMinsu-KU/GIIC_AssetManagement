-- Run only after a database backup and the application release containing UNUSED is ready.
-- DDL commits independently in MySQL; the following data changes are transactional.
ALTER TABLE assets
MODIFY COLUMN status ENUM(
    'NORMAL', 'ABNORMAL', 'DISPOSED', 'REPAIRING',
    'TRANSFER_PENDING', 'DISPOSAL_PENDING', 'DISPOSAL_REJECTED',
    'LOSS_PENDING', 'LOST', 'UNUSED'
) NOT NULL DEFAULT 'NORMAL';

START TRANSACTION;

-- Field-survey correction: these nine assets are all on the third floor.
UPDATE assets
SET location = 'PI-LAM 시너지홀(한마관 지상3층)'
WHERE asset_no BETWEEN '2600249' AND '2600257'
  AND location = '경남지능화혁신사업단교수연구실&협업랩(한마관 지상2층)';

-- Sixteen '미사용' rows and one '사용안함' row become unassigned assets.
UPDATE assets
SET status = 'UNUSED',
    actual_user_name = NULL,
    actual_user_department = NULL,
    actual_user_contact = NULL
WHERE asset_no IN (
    '2502230', '2502242', '2502243', '2502270', '2502278',
    '2502279', '2502280', '2600248', '2600249', '2600250',
    '2600251', '2600252', '2600253', '2600254', '2600255',
    '2600256', '2600257'
)
  AND status = 'NORMAL'
  AND location IS NOT NULL AND TRIM(location) <> ''
  AND (actual_user_name IS NULL OR actual_user_name = '' OR
       (asset_no = '2502278' AND actual_user_name = '사용안함'))
  AND actual_user_department IS NULL
  AND actual_user_contact IS NULL;

-- The fault marker was stored as a user name only because the UI lacked a fault state.
UPDATE assets
SET status = 'ABNORMAL', actual_user_name = NULL
WHERE asset_no = '2409859'
  AND status = 'NORMAL'
  AND actual_user_name IN ('고장', '고장으로 인한 유휴')
  AND actual_user_department IS NULL
  AND actual_user_contact IS NULL;

COMMIT;
