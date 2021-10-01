-- MID-7173
ALTER TABLE m_task ADD COLUMN schedulingState INT4;
ALTER TABLE m_task ADD COLUMN autoScalingMode INT4;
ALTER TABLE m_node ADD COLUMN operationalState INT4;

-- WRITE CHANGES ABOVE ^^
UPDATE m_global_metadata SET value = '4.4' WHERE name = 'databaseSchemaVersion';

-- MID-6974
UPDATE qrtz_job_details SET job_class_name = 'com.evolveum.midpoint.task.quartzimpl.run.JobExecutor'
    WHERE job_class_name = 'com.evolveum.midpoint.task.quartzimpl.execution.JobExecutor';

COMMIT;
