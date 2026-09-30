ALTER TABLE transfers
ADD COLUMN recipient_name VARCHAR(255),
ADD COLUMN recipient_department VARCHAR(255),
ADD COLUMN recipient_contact VARCHAR(255),
ADD COLUMN recipient_email VARCHAR(255),
ADD COLUMN transfer_location VARCHAR(255) NOT NULL,
ADD COLUMN remarks TEXT;
