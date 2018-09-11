-- create equipment type hierarchy, actual data will be synchronized by SymmetricDS
CREATE TABLE product_companies (
  product_company nvarchar(255) PRIMARY KEY NOT NULL
);

CREATE TABLE equipment_types (
  id int PRIMARY KEY NOT NULL,
  name nvarchar(255) NOT NULL,
  product_company nvarchar(255) NOT NULL,
  description nvarchar(255) NOT NULL,
  created_at DATETIME NOT NULL,
  modified_at DATETIME NOT NULL,
  FOREIGN KEY (product_company) REFERENCES product_companies (product_company) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE TABLE subsystems (
  id int PRIMARY KEY NOT NULL,
  equipment_id int NOT NULL,
  name nvarchar(255) NOT NULL,
  description nvarchar(255) NOT NULL,
  created_at DATETIME NOT NULL,
  modified_at DATETIME NOT NULL,
  FOREIGN KEY (equipment_id) REFERENCES equipment_types (id) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE TABLE subequipment (
  id int PRIMARY KEY NOT NULL,
  subsystem_id int NOT NULL,
  name nvarchar(255) NOT NULL,
  description nvarchar(255) NOT NULL,
  created_at DATETIME NOT NULL,
  modified_at DATETIME NOT NULL,
  FOREIGN KEY (subsystem_id) REFERENCES subsystems (id) ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- model

CREATE TABLE models (
  id UNIQUEIDENTIFIER PRIMARY KEY NOT NULL,
  model_id nvarchar(255) UNIQUE NOT NULL,
  equipment_type INT NOT NULL,
  translation_codes nvarchar(MAX) NULL,
  version INT NOT NULL,
  template BIT NOT NULL,
  created_at DATETIME NOT NULL,
  modified_at DATETIME NOT NULL,
  FOREIGN KEY (equipment_type) REFERENCES equipment_types (id) ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- configurations

CREATE TABLE configurations (
  id UNIQUEIDENTIFIER PRIMARY KEY NOT NULL,
  model_id UNIQUEIDENTIFIER NOT NULL,
  name nvarchar(255) NOT NULL,
  FOREIGN KEY (model_id) REFERENCES models (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE configuration_translations (
  configuration_id UNIQUEIDENTIFIER NOT NULL,
  language nvarchar(255) NOT NULL,
  name nvarchar(255) NULL,
  PRIMARY KEY (configuration_id, language),
  FOREIGN KEY (configuration_id) REFERENCES configurations (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE configuration_options (
  id UNIQUEIDENTIFIER PRIMARY KEY NOT NULL,
  configuration_id UNIQUEIDENTIFIER NOT NULL,
  name nvarchar(255) NOT NULL,
  FOREIGN KEY (configuration_id) REFERENCES configurations (id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- options

CREATE TABLE options (
  id UNIQUEIDENTIFIER PRIMARY KEY NOT NULL,
  model_id UNIQUEIDENTIFIER NOT NULL,
  name nvarchar(255) NOT NULL,
  FOREIGN KEY (model_id) REFERENCES models (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE option_translations (
  option_id UNIQUEIDENTIFIER NOT NULL,
  language nvarchar(255) NOT NULL,
  name nvarchar(255) NULL,
  PRIMARY KEY (option_id, language),
  FOREIGN KEY (option_id) REFERENCES options (id) ON DELETE CASCADE ON UPDATE CASCADE
);
-- zone

CREATE TABLE zones (
  id UNIQUEIDENTIFIER PRIMARY KEY NOT NULL,
  model_id UNIQUEIDENTIFIER NOT NULL,
  name nvarchar(255) NOT NULL,
  safety BIT NOT NULL,
  position INT NOT NULL,
  FOREIGN KEY (model_id) REFERENCES models (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE zone_translations (
  zone_id UNIQUEIDENTIFIER NOT NULL,
  language nvarchar(255) NOT NULL,
  name nvarchar(255) NULL,
  PRIMARY KEY (zone_id, language),
  FOREIGN KEY (zone_id) REFERENCES zones (id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- audit

CREATE TABLE audit_protocols (
  id UNIQUEIDENTIFIER PRIMARY KEY NOT NULL,
  model_id UNIQUEIDENTIFIER NOT NULL,
  template BIT NOT NULL,
  tracking_information NVARCHAR(MAX) NULL,
  version INT NOT NULL,
  created_at DATETIME NOT NULL,
  modified_at DATETIME NOT NULL,
  FOREIGN KEY (model_id) REFERENCES models (id) ON DELETE NO ACTION ON UPDATE CASCADE
);

CREATE TABLE steps
(
  id UNIQUEIDENTIFIER PRIMARY KEY NOT NULL,
  audit_id UNIQUEIDENTIFIER NOT NULL,
  step_type integer NOT NULL,
  name nvarchar(255) NULL,
  description nvarchar(MAX) NULL,
  special nvarchar(MAX),
  subsystem integer NULL,
  subequipment integer NULL,
  prompt nvarchar(255) NULL,
  reference nvarchar(MAX),
  zone_id UNIQUEIDENTIFIER,
  must_justify_na BIT NULL,
  pre_step BIT NULL,
  operational_constraints integer NULL,
  protocol_types bigint NULL,
  position INT NOT NULL,
  range_min FLOAT NULL,
  range_max FLOAT NULL,
  range_unit_of_measure nvarchar(255) NULL,
  range_decimal_places nvarchar(255) NULL,
  flash_air_ssid nvarchar(255) NULL,
  flash_air_folder_name nvarchar(255) NULL,
  flash_air_filename_pattern nvarchar(255) NULL,
  flash_air_network_password nvarchar(255) NULL,
  free_text_tag nvarchar(255) NULL,
  free_text_validation_expression nvarchar(255) NULL,
  ios_data_logging_interval integer NULL,
  ios_number_of_samples integer NULL,
  created_at DATETIME NOT NULL,
  modified_at DATETIME NOT NULL,
  FOREIGN KEY (audit_id) REFERENCES audit_protocols (id),
  FOREIGN KEY (subsystem) REFERENCES subsystems (ID),
  FOREIGN KEY (subequipment) REFERENCES subequipment (ID),
  FOREIGN KEY (zone_id) REFERENCES zones (ID)
);

CREATE TABLE step_translations (
  step_id UNIQUEIDENTIFIER NOT NULL,
  language nvarchar(255) NOT NULL,
  name nvarchar(255) NULL,
  description nvarchar(MAX) NULL,
  special nvarchar(MAX),
  prompt nvarchar(255) NULL,
  reference nvarchar(MAX),
  PRIMARY KEY (step_id, language),
  FOREIGN KEY (step_id) REFERENCES steps (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE step_options (
  step_id UNIQUEIDENTIFIER NOT NULL,
  [option] UNIQUEIDENTIFIER NOT NULL,
  PRIMARY KEY (step_id, [option]),
  FOREIGN KEY (step_id) REFERENCES steps (id),
  FOREIGN KEY ([option]) REFERENCES options (id)
);

CREATE TABLE step_reference_images (
  step_id UNIQUEIDENTIFIER NOT NULL,
  file_path nvarchar(255) NOT NULL,
  position INT NOT NULL,
  PRIMARY KEY (step_id, file_path),
  FOREIGN KEY (step_id) REFERENCES steps (id),
);

CREATE TABLE step_safety_images (
  step_id UNIQUEIDENTIFIER NOT NULL,
  file_path nvarchar(255) NOT NULL,
  position INT NOT NULL,
  PRIMARY KEY (step_id, file_path),
  FOREIGN KEY (step_id) REFERENCES steps (id),
);

CREATE TABLE step_configuration_options (
  step_id UNIQUEIDENTIFIER NOT NULL,
  configuration_option UNIQUEIDENTIFIER NOT NULL,
  PRIMARY KEY (step_id, configuration_option),
  FOREIGN KEY (step_id) REFERENCES steps (id),
  FOREIGN KEY (configuration_option) REFERENCES configuration_options (id)
)
