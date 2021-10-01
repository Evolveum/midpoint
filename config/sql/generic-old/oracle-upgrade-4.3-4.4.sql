-- MID-7173
ALTER TABLE m_task ADD schedulingState NUMBER(10, 0);
ALTER TABLE m_task ADD autoScalingMode NUMBER(10, 0);
ALTER TABLE m_node ADD operationalState NUMBER(10, 0);

-- WRITE CHANGES ABOVE ^^
UPDATE m_global_metadata SET value = '4.4' WHERE name = 'databaseSchemaVersion';

-- MID-6974
UPDATE qrtz_job_details SET job_class_name = 'com.evolveum.midpoint.task.quartzimpl.run.JobExecutor'
    WHERE job_class_name = 'com.evolveum.midpoint.task.quartzimpl.execution.JobExecutor';

COMMIT;
