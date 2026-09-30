-- Source: 자산합본.xlsx / Sheet1 / 실사용자 = '공용' (118 unique asset numbers).
-- Back up the database and verify all 118 rows are NORMAL before running.
-- Run once, before starting the backend version that maps shared_use.
ALTER TABLE assets
ADD COLUMN shared_use BIT(1) NOT NULL DEFAULT b'0';

CREATE TABLE IF NOT EXISTS asset_usage_change_history (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    asset_id BIGINT NOT NULL,
    asset_no VARCHAR(255) NOT NULL,
    previous_user_name VARCHAR(100) NULL,
    previous_user_department VARCHAR(100) NULL,
    previous_user_contact VARCHAR(100) NULL,
    source_file VARCHAR(100) NOT NULL,
    captured_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_asset_usage_source (asset_no, source_file)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TEMPORARY TABLE survey_shared_assets (
    asset_no VARCHAR(255) NOT NULL PRIMARY KEY
);

INSERT INTO survey_shared_assets (asset_no) VALUES
('1804465'), ('2300794'), ('2405232'), ('2405935'), ('2405936'),
('2405937'), ('2405938'), ('2405939'), ('2405940'), ('2405941'),
('2405942'), ('2405943'), ('2405944'), ('2405945'), ('2405947'),
('2405948'), ('2405953'), ('2405954'), ('2405955'), ('2405956'),
('2405957'), ('2405958'), ('2405959'), ('2405960'), ('2405961'),
('2405962'), ('2405963'), ('2405964'), ('2405965'), ('2405966'),
('2405968'), ('2405970'), ('2405971'), ('2405972'), ('2405974'),
('2405975'), ('2405976'), ('2405977'), ('2405978'), ('2405979'),
('2405980'), ('2405981'), ('2405982'), ('2405983'), ('2405984'),
('2405985'), ('2405986'), ('2405988'), ('2405989'), ('2405991'),
('2405992'), ('2405994'), ('2405995'), ('2405996'), ('2405997'),
('2405998'), ('2409842'), ('2409843'), ('2410103'), ('2410114'),
('2410735'), ('2410742'), ('2411303'), ('2500003'), ('2500464'),
('2500471'), ('2500473'), ('2500474'), ('2500475'), ('2500478'),
('2500479'), ('2500481'), ('2500501'), ('2501407'), ('2502011'),
('2502012'), ('2502013'), ('2502014'), ('2502193'), ('2502195'),
('2502202'), ('2502204'), ('2502215'), ('2502216'), ('2502220'),
('2502224'), ('2502236'), ('2502244'), ('2502245'), ('2502246'),
('2502247'), ('2502248'), ('2502249'), ('2502250'), ('2502251'),
('2502252'), ('2502253'), ('2502254'), ('2502255'), ('2502256'),
('2502257'), ('2502258'), ('2502259'), ('2502260'), ('2502261'),
('2502316'), ('2502318'), ('2502321'), ('2502322'), ('2502383'),
('2502384'), ('2502385'), ('2600246'), ('2600389'), ('2600408'),
('2600473'), ('2600525'), ('2600526');

START TRANSACTION;

-- Preserve pre-survey assignments without misrepresenting them as transfer requests.
INSERT INTO asset_usage_change_history (
    asset_id, asset_no, previous_user_name, previous_user_department,
    previous_user_contact, source_file
)
SELECT a.id, a.asset_no, a.actual_user_name, a.actual_user_department,
       a.actual_user_contact, '자산합본.xlsx'
FROM assets a
JOIN survey_shared_assets s ON s.asset_no = a.asset_no
WHERE a.status = 'NORMAL'
  AND (NULLIF(TRIM(a.actual_user_name), '') IS NOT NULL
       OR NULLIF(TRIM(a.actual_user_department), '') IS NOT NULL
       OR NULLIF(TRIM(a.actual_user_contact), '') IS NOT NULL)
ON DUPLICATE KEY UPDATE asset_id = asset_id;

UPDATE assets a
JOIN survey_shared_assets s ON s.asset_no = a.asset_no
SET a.shared_use = b'1',
    a.actual_user_name = NULL,
    a.actual_user_department = NULL,
    a.actual_user_contact = NULL,
    a.updated_at = NOW(6)
WHERE a.status = 'NORMAL';

COMMIT;

SELECT COUNT(*) AS source_count FROM survey_shared_assets;
SELECT COUNT(*) AS shared_count FROM assets WHERE shared_use = b'1';
SELECT COUNT(*) AS prior_assignment_count FROM asset_usage_change_history
WHERE source_file = '자산합본.xlsx';
