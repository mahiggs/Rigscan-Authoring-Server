-- convert the version to be a string so we can have {pushed version}.{created version}
ALTER TABLE rigscan_authoring.dbo.audit_protocols DROP COLUMN version;
ALTER TABLE rigscan_authoring.dbo.audit_protocols ADD version NVARCHAR(255);

GO

-- set all of the audit protocols to version 1.0.0, when we do the final conversion, we will
-- need to manually determine the version for each
UPDATE rigscan_authoring.dbo.audit_protocols SET version='1.0.0';
ALTER TABLE rigscan_authoring.dbo.audit_protocols ALTER COLUMN version NVARCHAR(255) NOT NULL;

-- add a step_id for steps, this is how we will track the step
ALTER TABLE rigscan_authoring.dbo.steps ADD step_id UNIQUEIDENTIFIER NULL;
GO
UPDATE rigscan_authoring.dbo.steps SET step_id = id;
ALTER TABLE rigscan_authoring.dbo.steps ALTER COLUMN step_id UNIQUEIDENTIFIER NOT NULL;

-- create the versioned_model_information
CREATE TABLE versioned_model_information (
  id UNIQUEIDENTIFIER PRIMARY KEY NOT NULL,
  model_id UNIQUEIDENTIFIER NOT NULL,
  translation_codes nvarchar(MAX) NULL,
  version NVARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL,
  modified_at DATETIME NOT NULL,
  FOREIGN KEY (model_id) REFERENCES models (id) ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- initialize the versioned_model_information
INSERT INTO rigscan_authoring.dbo.versioned_model_information (id, model_id, translation_codes, version, created_at, modified_at)
  SELECT id, id, translation_codes, '1.0.0', created_at, modified_at FROM rigscan_authoring.dbo.models;

-- removed versioned information from the main models
ALTER TABLE rigscan_authoring.dbo.models DROP COLUMN translation_codes;
ALTER TABLE rigscan_authoring.dbo.models DROP COLUMN version;

-- move the foreign keys from model to versioned_model_information
ALTER TABLE rigscan_authoring.dbo.zones DROP CONSTRAINT FK__zones__model_id__2C80D9EF
ALTER TABLE rigscan_authoring.dbo.zones ADD FOREIGN KEY (model_id) REFERENCES versioned_model_information (id);
ALTER TABLE rigscan_authoring.dbo.options DROP CONSTRAINT FK__options__model_i__26C80099
ALTER TABLE rigscan_authoring.dbo.options ADD FOREIGN KEY (model_id) REFERENCES versioned_model_information (id);
ALTER TABLE rigscan_authoring.dbo.configurations DROP CONSTRAINT FK__configura__model__1E32BA98
ALTER TABLE rigscan_authoring.dbo.configurations ADD FOREIGN KEY (model_id) REFERENCES versioned_model_information (id);

INSERT INTO sym_trigger (trigger_id,source_table_name,channel_id,sync_on_incoming_batch,last_update_time,create_time)
  VALUES ('versioned_model_information','versioned_model_information','model', 1, current_timestamp, current_timestamp);

-- add the backend2client router in where necessary
UPDATE sym_trigger_router SET initial_load_order = initial_load_order + 1 WHERE initial_load_order > 5
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('versioned_model_information','backend2client', 6, current_timestamp, current_timestamp);
-- add the client2backend router in where necessary
UPDATE sym_trigger_router SET initial_load_order = initial_load_order + 1 WHERE initial_load_order > 21
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('versioned_model_information','client2backend', 22, current_timestamp, current_timestamp);