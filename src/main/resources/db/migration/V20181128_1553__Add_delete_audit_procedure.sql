CREATE PROC dbo.deleteAudit(@versioned_model_id as UNIQUEIDENTIFIER, @audit_id as UNIQUEIDENTIFIER) AS
  BEGIN
    DELETE FROM step_options WHERE step_id IN (SELECT id FROM steps WHERE audit_id = @audit_id);
    DELETE FROM step_configuration_options WHERE step_id IN (SELECT id FROM steps WHERE audit_id = @audit_id);
    DELETE FROM step_safety_images WHERE step_id IN (SELECT id FROM steps WHERE audit_id = @audit_id);
    DELETE FROM step_reference_images WHERE step_id IN (SELECT id FROM steps WHERE audit_id = @audit_id);
    DELETE FROM step_translations WHERE step_id IN (SELECT id FROM steps WHERE audit_id = @audit_id);
    DELETE FROM steps WHERE audit_id = @audit_id;
    DELETE FROM configuration_options WHERE configuration_id IN (SELECT configurations.id FROM configurations WHERE configurations.model_id = @versioned_model_id);
    DELETE FROM configuration_translations WHERE configuration_id IN (SELECT configurations.id FROM configurations WHERE configurations.model_id = @versioned_model_id);
    DELETE FROM configurations WHERE model_id = @versioned_model_id;
    DELETE FROM option_translations WHERE option_id IN (SELECT id FROM options WHERE options.model_id = @versioned_model_id);
    DELETE FROM options WHERE model_id = @versioned_model_id;
    DELETE FROM zone_translations WHERE zone_id IN (SELECT id FROM zones WHERE model_id = @versioned_model_id);
    DELETE FROM zones WHERE model_id = @versioned_model_id;
    DELETE FROM versioned_model_information WHERE id = @versioned_model_id;
    DELETE FROM audit_protocols WHERE id = @audit_id;
  END