-- 1. 
ALTER TABLE qrtz_job_details ADD (
  is_durable_tmp        BOOLEAN DEFAULT FALSE NOT NULL,
  is_nonconcurrent_tmp  BOOLEAN DEFAULT FALSE NOT NULL,
  is_update_data_tmp    BOOLEAN DEFAULT FALSE NOT NULL,
  requests_recovery_tmp BOOLEAN DEFAULT FALSE NOT NULL
);

UPDATE qrtz_job_details
SET
  is_durable_tmp = CASE LOWER(is_durable) 
                     WHEN '1' THEN TRUE WHEN 'y' THEN TRUE WHEN 't' THEN TRUE                     
                     ELSE FALSE 
                   END,
  is_nonconcurrent_tmp = CASE LOWER(is_nonconcurrent) 
                     WHEN '1' THEN TRUE WHEN 'y' THEN TRUE WHEN 't' THEN TRUE
                     ELSE FALSE 
                   END,
  is_update_data_tmp = CASE LOWER(is_update_data) 
                     WHEN '1' THEN TRUE WHEN 'y' THEN TRUE WHEN 't' THEN TRUE
                     ELSE FALSE 
                   END,
  requests_recovery_tmp = CASE LOWER(requests_recovery) 
                     WHEN '1' THEN TRUE WHEN 'y' THEN TRUE WHEN 't' THEN TRUE
                     ELSE FALSE 
                   END;

ALTER TABLE qrtz_job_details DROP COLUMN is_durable;
ALTER TABLE qrtz_job_details RENAME COLUMN is_durable_tmp TO is_durable;

ALTER TABLE qrtz_job_details DROP COLUMN is_nonconcurrent;
ALTER TABLE qrtz_job_details RENAME COLUMN is_nonconcurrent_tmp TO is_nonconcurrent;

ALTER TABLE qrtz_job_details DROP COLUMN is_update_data;
ALTER TABLE qrtz_job_details RENAME COLUMN is_update_data_tmp TO is_update_data;

ALTER TABLE qrtz_job_details DROP COLUMN requests_recovery;
ALTER TABLE qrtz_job_details RENAME COLUMN requests_recovery_tmp TO requests_recovery;


-- 2. 
ALTER TABLE qrtz_simprop_triggers ADD (
  bool_prop_1_tmp BOOLEAN,
  bool_prop_2_tmp BOOLEAN
);

UPDATE qrtz_simprop_triggers
SET
  bool_prop_1_tmp = CASE LOWER(bool_prop_1) 
                      WHEN '1' THEN TRUE WHEN 'y' THEN TRUE WHEN 't' THEN TRUE
                      WHEN '0' THEN FALSE WHEN 'n' THEN FALSE WHEN 'f' THEN FALSE
                      ELSE NULL 
                    END,
  bool_prop_2_tmp = CASE LOWER(bool_prop_2) 
                      WHEN '1' THEN TRUE WHEN 'y' THEN TRUE WHEN 't' THEN TRUE
                      WHEN '0' THEN FALSE WHEN 'n' THEN FALSE WHEN 'f' THEN FALSE
                      ELSE NULL 
                    END;

ALTER TABLE qrtz_simprop_triggers DROP COLUMN bool_prop_1;
ALTER TABLE qrtz_simprop_triggers RENAME COLUMN bool_prop_1_tmp TO bool_prop_1;

ALTER TABLE qrtz_simprop_triggers DROP COLUMN bool_prop_2;
ALTER TABLE qrtz_simprop_triggers RENAME COLUMN bool_prop_2_tmp TO bool_prop_2;


-- 3. 
ALTER TABLE qrtz_fired_triggers ADD (
  is_nonconcurrent_tmp   BOOLEAN,
  requests_recovery_tmp  BOOLEAN
);

UPDATE qrtz_fired_triggers
SET
  is_nonconcurrent_tmp = CASE LOWER(is_nonconcurrent) 
                           WHEN '1' THEN TRUE WHEN 'y' THEN TRUE WHEN 't' THEN TRUE
                           WHEN '0' THEN FALSE WHEN 'n' THEN FALSE WHEN 'f' THEN FALSE
                           ELSE NULL 
                         END,
  requests_recovery_tmp = CASE LOWER(requests_recovery) 
                           WHEN '1' THEN TRUE WHEN 'y' THEN TRUE WHEN 't' THEN TRUE
                           WHEN '0' THEN FALSE WHEN 'n' THEN FALSE WHEN 'f' THEN FALSE
                           ELSE NULL 
                         END;

ALTER TABLE qrtz_fired_triggers DROP COLUMN is_nonconcurrent;
ALTER TABLE qrtz_fired_triggers RENAME COLUMN is_nonconcurrent_tmp TO is_nonconcurrent;

ALTER TABLE qrtz_fired_triggers DROP COLUMN requests_recovery;
ALTER TABLE qrtz_fired_triggers RENAME COLUMN requests_recovery_tmp TO requests_recovery;
