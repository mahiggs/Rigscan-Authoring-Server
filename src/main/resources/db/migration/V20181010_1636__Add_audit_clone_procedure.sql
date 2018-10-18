CREATE PROC dbo.cloneAudit(@model_id as UNIQUEIDENTIFIER, @audit_id as UNIQUEIDENTIFIER, @version as nvarchar(255)) AS
  BEGIN
    DECLARE @NewAuditId uniqueidentifier = NEWID();
    DECLARE @NewModelId uniqueidentifier = NEWID();

    -- generate new ids
    SELECT id as old_id, NEWID() as new_id INTO #ZoneIdMap FROM zones WHERE model_id = @model_id;
    SELECT id as old_id, NEWID() as new_id INTO #OptionIdMap FROM options WHERE model_id = @model_id;
    SELECT id as old_id, NEWID() as new_id INTO #ConfigurationIdMap FROM configurations WHERE model_id = @model_id;
    SELECT configuration_options.id as old_id, NEWID() as new_id INTO #ConfigurationOptionIdMap FROM configuration_options
      INNER JOIN configurations ON configuration_options.configuration_id = configurations.id WHERE model_id = @model_id;
    SELECT id as old_id, NEWID() as new_id INTO #StepIdMap FROM steps WHERE audit_id = @audit_id;

    -- duplicate protocol with new id and version
    INSERT INTO audit_protocols(id, model_id, template, tracking_information, version, created_at, modified_at)
      SELECT @NewAuditId, model_id, template, tracking_information, @version, current_timestamp, current_timestamp FROM audit_protocols WHERE id = @audit_id
    -- duplicate versioned model information with new id and version
    INSERT INTO versioned_model_information (id, model_id, translation_codes, version, created_at, modified_at)
      SELECT @NewModelId, model_id, translation_codes, @version, current_timestamp, current_timestamp FROM versioned_model_information WHERE id = @model_id
    -- duplicate zones
    INSERT INTO zones (id, model_id, name, safety, position)
      SELECT new_id, @NewModelId, name, safety, position FROM zones INNER JOIN #ZoneIdMap on zones.id = #ZoneIdMap.old_id;
    -- duplicate zone translations
    INSERT INTO zone_translations (zone_id, language, name)
      SELECT new_id, language, name FROM zone_translations INNER JOIN #ZoneIdMap on zone_translations.zone_id = #ZoneIdMap.old_id;
    -- duplicate options
    INSERT INTO options (id, model_id, name)
      SELECT new_id, @NewModelId, name FROM options INNER JOIN #OptionIdMap on options.id = #OptionIdMap.old_id;
    -- duplicate option translations
    INSERT INTO option_translations (option_id, language, name)
      SELECT new_id, language, name FROM option_translations INNER JOIN #OptionIdMap on option_translations.option_id = #OptionIdMap.old_id;
    -- duplicate configurations
    INSERT INTO configurations (id, model_id, name)
      SELECT new_id, @NewModelId, name FROM configurations INNER JOIN #ConfigurationIdMap on configurations.id = #ConfigurationIdMap.old_id;
    -- duplicate configuration translations
    INSERT INTO configuration_translations (configuration_id, language, name)
      SELECT new_id, language, name FROM configuration_translations INNER JOIN #ConfigurationIdMap on configuration_translations.configuration_id = #ConfigurationIdMap.old_id;
    -- duplicate  configuration options
    INSERT INTO configuration_options (id, configuration_id, name)
      SELECT #ConfigurationOptionIdMap.new_id, #ConfigurationIdMap.new_id, name FROM configuration_options
                INNER JOIN #ConfigurationOptionIdMap on configuration_options.id = #ConfigurationOptionIdMap.old_id
                INNER JOIN #ConfigurationIdMap on configuration_options.configuration_id = #ConfigurationIdMap.old_id;
    -- duplicate steps
    INSERT INTO steps(id, step_id, audit_id, step_type, name, description, special, subsystem, subequipment, prompt, reference, zone_id, must_justify_na, pre_step, operational_constraints, protocol_types, position, range_min, range_max, range_unit_of_measure, range_decimal_places, flash_air_ssid, flash_air_folder_name, flash_air_filename_pattern, flash_air_network_password, free_text_tag, free_text_validation_expression, ios_data_logging_interval, ios_number_of_samples, created_at, modified_at)
    SELECT #StepIdMap.new_id, step_id, @NewAuditId, step_type, name, description, special, subsystem, subequipment, prompt,
           reference, #ZoneIdMap.new_id, must_justify_na, pre_step, operational_constraints, protocol_types, position, range_min,
           range_max, range_unit_of_measure, range_decimal_places, flash_air_ssid, flash_air_folder_name,
           flash_air_filename_pattern, flash_air_network_password, free_text_tag, free_text_validation_expression,
           ios_data_logging_interval, ios_number_of_samples, current_timestamp, current_timestamp
    FROM rigscan_authoring.dbo.steps
           INNER JOIN #StepIdMap ON steps.id = #StepIdMap.old_id
           INNER JOIN #ZoneIdMap ON steps.zone_id = #ZoneIdMap.old_id
    WHERE audit_id = @audit_id
    -- duplicate step translations
    INSERT INTO step_translations(step_id, language, name, description, special, prompt, reference)
    SELECT #StepIdMap.new_id, language, name, description, special, prompt, reference
    FROM step_translations
           INNER JOIN #StepIdMap on step_translations.step_id = #StepIdMap.old_id;
    -- duplicate step reference images
    INSERT INTO step_reference_images(step_id, file_path, position)
    SELECT #StepIdMap.new_id, file_path, position
    FROM step_reference_images
           INNER JOIN #StepIdMap on step_reference_images.step_id = #StepIdMap.old_id;
    -- duplicate step safety images
    INSERT INTO step_safety_images(step_id, file_path, position)
    SELECT #StepIdMap.new_id, file_path, position
    FROM step_safety_images
           INNER JOIN #StepIdMap on step_safety_images.step_id = #StepIdMap.old_id;
    -- duplicate step configuration options
    INSERT INTO step_configuration_options(step_id, configuration_option)
    SELECT #StepIdMap.new_id, #ConfigurationOptionIdMap.new_id
    FROM step_configuration_options
           INNER JOIN #StepIdMap on step_configuration_options.step_id = #StepIdMap.old_id
           INNER JOIN #ConfigurationOptionIdMap on step_configuration_options.configuration_option = #ConfigurationOptionIdMap.old_id;
    -- duplicate step options
    INSERT INTO step_options(step_id, [option])
    SELECT #StepIdMap.new_id, #OptionIdMap.new_id
    FROM step_options
           INNER JOIN #StepIdMap on step_options.step_id = #StepIdMap.old_id
           INNER JOIN #OptionIdMap on step_options.[option] = #OptionIdMap.old_id;
  END