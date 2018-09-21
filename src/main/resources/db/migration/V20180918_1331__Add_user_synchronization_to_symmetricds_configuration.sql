-- add a new channel
INSERT INTO sym_channel (channel_id, processing_order, max_batch_size, enabled, description)
  VALUES ('other', 1, 100000, 1, 'Other data that needs to be synchronized.');

-- add triggers for the new tables
INSERT INTO sym_trigger (trigger_id, source_table_name, channel_id, last_update_time, create_time)
  VALUES ('users', 'users', 'other', current_timestamp, current_timestamp);
INSERT INTO sym_trigger (trigger_id, source_table_name, channel_id, last_update_time, create_time)
  VALUES ('roles', 'roles', 'other', current_timestamp, current_timestamp);
INSERT INTO sym_trigger (trigger_id, source_table_name, channel_id, last_update_time, create_time)
  VALUES ('user_roles', 'user_roles', 'other', current_timestamp, current_timestamp);

-- add trigger routers for the new tables
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('users','backend2client', 35, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('roles','backend2client', 36, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('user_roles','backend2client', 37, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('users','client2backend', 38, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('roles','client2backend', 39, current_timestamp, current_timestamp);
INSERT INTO sym_trigger_router (trigger_id,router_id,initial_load_order,last_update_time,create_time)
  VALUES ('user_roles','client2backend', 40, current_timestamp, current_timestamp);
