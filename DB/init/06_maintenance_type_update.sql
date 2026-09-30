-- Update maintenance type ENUM to include all values from Java MaintenanceType enum
-- Current Java Enum: REPAIR, INSPECTION, REPLACEMENT, OTHER, CHECK, UPGRADE
ALTER TABLE maintenance 
MODIFY COLUMN type ENUM('REPAIR', 'INSPECTION', 'REPLACEMENT', 'OTHER', 'CHECK', 'UPGRADE') NOT NULL DEFAULT 'REPAIR';
