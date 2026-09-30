ALTER TABLE assets
    ADD COLUMN loss_date DATE NULL,
    ADD COLUMN loss_requester_id VARCHAR(20) NULL,
    ADD COLUMN loss_requester_name VARCHAR(100) NULL;

CREATE TABLE asset_loss_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    asset_id BIGINT NOT NULL,
    requester_employee_no VARCHAR(20) NOT NULL,
    requester_name VARCHAR(100) NOT NULL,
    loss_date DATE NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    rejection_reason TEXT NULL,
    processor_id VARCHAR(20) NULL,
    processed_at DATETIME(6) NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    INDEX idx_asset_loss_asset_id (asset_id),
    INDEX idx_asset_loss_requester (requester_employee_no),
    INDEX idx_asset_loss_status (status),
    CONSTRAINT fk_asset_loss_asset
        FOREIGN KEY (asset_id) REFERENCES assets(id) ON DELETE CASCADE,
    CONSTRAINT fk_asset_loss_processor
        FOREIGN KEY (processor_id) REFERENCES users(employee_no) ON DELETE SET NULL
);
