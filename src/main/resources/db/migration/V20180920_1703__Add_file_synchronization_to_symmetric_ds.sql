
INSERT INTO sym_file_trigger (trigger_id, base_dir, includes_files, before_copy_script, create_time, last_update_time, description)
  VALUES ('protocol_files', 'D:\authoring_backend_storage', '*', 'targetBaseDir = new java.io.File(engine.getParameterService().getTempDirectory(), "../../../files").getAbsolutePath();', current_timestamp, current_timestamp, 'This file trigger will synchronize all of the protocol images.');

INSERT INTO sym_file_trigger_router (trigger_id, router_id, target_base_dir, last_update_time, create_time)
  VALUES ('protocol_files', 'backend2client', '$' + '{localAppData}/RigscanAuthoring/files', current_timestamp, current_timestamp);