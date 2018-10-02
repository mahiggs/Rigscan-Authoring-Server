-- remove old columns
ALTER TABLE rigscan_authoring.dbo.audit_protocols DROP CONSTRAINT fk_checked_out_by_users_id;
ALTER TABLE rigscan_authoring.dbo.audit_protocols DROP CONSTRAINT fk_checkout_performed_by_users_id;
ALTER TABLE rigscan_authoring.dbo.audit_protocols DROP COLUMN checked_out_by;
ALTER TABLE rigscan_authoring.dbo.audit_protocols DROP COLUMN checkout_performed_by;
ALTER TABLE rigscan_authoring.dbo.audit_protocols DROP COLUMN checked_out_reason;

-- add new table
CREATE TABLE audit_protocol_checkouts (
  id int identity(1,1) not null primary key,
  model_id uniqueidentifier not null UNIQUE,
  checked_out_by int not null,
  checkout_performed_by int not null,
  checked_out_reason nvarchar(max) null,
  foreign key (model_id) references models (id),
  foreign key (checked_out_by) references users (id),
  foreign key (checkout_performed_by) references users (id)
);

-- configure symmetric DS for the new table
INSERT INTO sym_trigger (trigger_id, source_table_name, channel_id, last_update_time, create_time)
  VALUES ('audit_protocol_checkouts', 'audit_protocol_checkouts', 'audit_protocol', current_timestamp, current_timestamp);

INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('audit_protocol_checkouts','backend2client', 41, current_timestamp, current_timestamp);

-- remove spurious configuration...we will not be receiving information from the client on the users
DELETE FROM sym_trigger_router WHERE trigger_id = 'users' AND router_id = 'client2backend';
DELETE FROM sym_trigger_router WHERE trigger_id = 'roles' AND router_id = 'client2backend';
DELETE FROM sym_trigger_router WHERE trigger_id = 'user_roles' AND router_id = 'client2backend';