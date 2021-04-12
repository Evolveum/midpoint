-- MID-6417
ALTER TABLE m_operation_execution ADD recordType NUMBER(10, 0);

-- MID-3669
ALTER TABLE m_focus ADD lockoutStatus NUMBER(10, 0);

-- WRITE CHANGES ABOVE ^^
UPDATE m_global_metadata SET value = '4.3' WHERE name = 'databaseSchemaVersion';

-- MID-6974
UPDATE qrtz_job_details SET job_class_name = 'com.evolveum.midpoint.task.quartzimpl.run.JobExecutor'
    WHERE job_class_name = 'com.evolveum.midpoint.task.quartzimpl.execution.JobExecutor';

COMMIT;
