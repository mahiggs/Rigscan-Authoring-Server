-- add checked_out_at to provide information as to when checkout took place
ALTER TABLE rigscan_authoring.dbo.audit_protocol_checkouts ADD checked_out_at DATETIME NULL;

-- we have to exec this here due to the fact that SQL Server will check this statement and find that the column doesn't exist
-- yet due to the fact that the previous statement creates it
EXEC('UPDATE rigscan_authoring.dbo.audit_protocol_checkouts SET checked_out_at = CURRENT_TIMESTAMP WHERE checked_out_at IS NULL;')

ALTER TABLE rigscan_authoring.dbo.audit_protocol_checkouts ALTER COLUMN checked_out_at DATETIME NOT NULL;