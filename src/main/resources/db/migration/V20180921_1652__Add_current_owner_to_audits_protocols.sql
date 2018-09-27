-- add a column by which the audit can be checked out
ALTER TABLE rigscan_authoring.dbo.audit_protocols ADD checked_out_by INT NULL;
-- add a column to track who checked out the audit for the user, this will be the same as checked_out_by, except for
-- when the user is a LITE_USER and must have the checkout performed by an admin
ALTER TABLE rigscan_authoring.dbo.audit_protocols ADD checkout_performed_by INT NULL;
-- add a column by which the user can provide a reason for why they are checking out this audit
ALTER TABLE rigscan_authoring.dbo.audit_protocols ADD checked_out_reason NVARCHAR(max) NULL;
-- if the user is updated, we prevent it (as this should never happen), if the user is deleted, then we allow it and automatically
-- "check it in"
ALTER TABLE rigscan_authoring.dbo.audit_protocols ADD CONSTRAINT fk_checked_out_by_users_id FOREIGN KEY (checked_out_by) REFERENCES users (id) ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE rigscan_authoring.dbo.audit_protocols ADD CONSTRAINT fk_checkout_performed_by_users_id FOREIGN KEY (checkout_performed_by) REFERENCES users (id) ON UPDATE NO ACTION ON DELETE NO ACTION;