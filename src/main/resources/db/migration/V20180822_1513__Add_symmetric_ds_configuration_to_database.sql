-- create a channel to carry the equipment type hierarchy
INSERT INTO sym_channel (channel_id, processing_order, max_batch_size, enabled, description)
    VALUES ('equipment_type', 1, 100000, 1, 'Equipment type/subsystem/subequipment hierarchy');
-- create a channel to carry the model information
INSERT INTO sym_channel (channel_id, processing_order, max_batch_size, enabled, description)
VALUES ('model', 1, 100000, 1, 'Model information');
-- create a channel to carry the audit_protocol information
INSERT INTO sym_channel (channel_id, processing_order, max_batch_size, enabled, description)
VALUES ('audit_protocol', 1, 100000, 1, 'Audit protocol information');

-- create the groups
UPDATE sym_node_group SET description = 'The central backend server.' WHERE node_group_id = 'backend';
INSERT INTO sym_node_group (node_group_id, description) VALUES ('client', 'A client of the backend server.');

-- setup the relationship between the groups
INSERT INTO sym_node_group_link (source_node_group_id, target_node_group_id, data_event_action)
  VALUES ('backend', 'client', 'W');
INSERT INTO sym_node_group_link (source_node_group_id, target_node_group_id, data_event_action)
  VALUES ('client', 'backend', 'P');

-- setup the triggers
INSERT INTO sym_trigger (trigger_id,source_table_name,channel_id,last_update_time,create_time)
  VALUES ('product_companies','product_companies','equipment_type', current_timestamp, current_timestamp);
INSERT INTO sym_trigger (trigger_id,source_table_name,channel_id,last_update_time,create_time)
  VALUES ('equipment_types','equipment_types','equipment_type', current_timestamp, current_timestamp);
INSERT INTO sym_trigger (trigger_id,source_table_name,channel_id,last_update_time,create_time)
  VALUES ('subsystems','subsystems','equipment_type', current_timestamp, current_timestamp);
INSERT INTO sym_trigger (trigger_id,source_table_name,channel_id,last_update_time,create_time)
  VALUES ('subequipment','subequipment','equipment_type', current_timestamp, current_timestamp);
INSERT INTO sym_trigger (trigger_id,source_table_name,channel_id,sync_on_incoming_batch,last_update_time,create_time)
  VALUES ('models','models','model', 1, current_timestamp, current_timestamp);
INSERT INTO sym_trigger (trigger_id,source_table_name,channel_id,sync_on_incoming_batch,last_update_time,create_time)
  VALUES ('zones','zones','model', 1, current_timestamp, current_timestamp);
INSERT INTO sym_trigger (trigger_id,source_table_name,channel_id,sync_on_incoming_batch,last_update_time,create_time)
  VALUES ('zone_translations','zone_translations','model', 1, current_timestamp, current_timestamp);
INSERT INTO sym_trigger (trigger_id,source_table_name,channel_id,sync_on_incoming_batch,last_update_time,create_time)
  VALUES ('configurations','configurations','model', 1, current_timestamp, current_timestamp);
INSERT INTO sym_trigger (trigger_id,source_table_name,channel_id,sync_on_incoming_batch,last_update_time,create_time)
  VALUES ('configuration_translations','configuration_translations','model', 1, current_timestamp, current_timestamp);
INSERT INTO sym_trigger (trigger_id,source_table_name,channel_id,sync_on_incoming_batch,last_update_time,create_time)
  VALUES ('configuration_options','configuration_options','model', 1, current_timestamp, current_timestamp);
INSERT INTO sym_trigger (trigger_id,source_table_name,channel_id,sync_on_incoming_batch,last_update_time,create_time)
  VALUES ('options','options','model', 1, current_timestamp, current_timestamp);
INSERT INTO sym_trigger (trigger_id,source_table_name,channel_id,sync_on_incoming_batch,last_update_time,create_time)
  VALUES ('option_translations','option_translations','model', 1, current_timestamp, current_timestamp);
INSERT INTO sym_trigger (trigger_id,source_table_name,channel_id,sync_on_incoming_batch,last_update_time,create_time)
  VALUES ('audit_protocols','audit_protocols','audit_protocol', 1, current_timestamp, current_timestamp);
INSERT INTO sym_trigger (trigger_id,source_table_name,channel_id,sync_on_incoming_batch,last_update_time,create_time)
  VALUES ('steps','steps','audit_protocol', 1, current_timestamp, current_timestamp);
INSERT INTO sym_trigger (trigger_id,source_table_name,channel_id,sync_on_incoming_batch,last_update_time,create_time)
  VALUES ('step_translations','step_translations','audit_protocol', 1, current_timestamp, current_timestamp);
INSERT INTO sym_trigger (trigger_id,source_table_name,channel_id,sync_on_incoming_batch,last_update_time,create_time)
  VALUES ('step_configuration_options','step_configuration_options','audit_protocol', 1, current_timestamp, current_timestamp);
INSERT INTO sym_trigger (trigger_id,source_table_name,channel_id,sync_on_incoming_batch,last_update_time,create_time)
  VALUES ('step_options','step_options','audit_protocol', 1, current_timestamp, current_timestamp);
INSERT INTO sym_trigger (trigger_id,source_table_name,channel_id,sync_on_incoming_batch,last_update_time,create_time)
  VALUES ('step_reference_images','step_reference_images','audit_protocol', 1, current_timestamp, current_timestamp);
INSERT INTO sym_trigger (trigger_id,source_table_name,channel_id,sync_on_incoming_batch,last_update_time,create_time)
  VALUES ('step_safety_images','step_safety_images','audit_protocol', 1, current_timestamp, current_timestamp);

-- setup the routers
INSERT INTO sym_router (router_id,source_node_group_id,target_node_group_id,router_type,create_time,last_update_time)
  VALUES ('backend2client', 'backend', 'client', 'default', current_timestamp, current_timestamp);
INSERT INTO sym_router (router_id,source_node_group_id,target_node_group_id,router_type,create_time,last_update_time)
  VALUES ('client2backend', 'client', 'backend', 'default', current_timestamp, current_timestamp);

-- setup the links from backend to client
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('product_companies','backend2client', 1, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('equipment_types','backend2client', 2, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('subsystems','backend2client', 3, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('subequipment','backend2client', 4, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('models','backend2client', 5, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('zones','backend2client', 6, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('zone_translations','backend2client', 7, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('configurations','backend2client', 8, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('configuration_translations','backend2client', 9, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('configuration_options','backend2client', 10, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('options','backend2client', 11, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('option_translations','backend2client', 12, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('audit_protocols','backend2client', 13, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('steps','backend2client', 14, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('step_translations','backend2client', 15, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('step_configuration_options','backend2client', 16, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('step_options','backend2client', 17, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('step_reference_images','backend2client', 18, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('step_safety_images','backend2client', 19, current_timestamp, current_timestamp);

-- setup the links from client to backend
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('models','client2backend', 20, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('zones','client2backend', 21, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('zone_translations','client2backend', 22, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('configurations','client2backend', 23, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('configuration_translations','client2backend', 24, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('configuration_options','client2backend', 25, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('options','client2backend', 26, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('option_translations','client2backend', 27, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('audit_protocols','client2backend', 28, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('steps','client2backend', 29, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('step_translations','client2backend', 30, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('step_configuration_options','client2backend', 31, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('step_options','client2backend', 32, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('step_reference_images','client2backend', 33, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('step_safety_images','client2backend', 34, current_timestamp, current_timestamp);
