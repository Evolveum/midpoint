-- Never mix DDL (CREATE/UPDATE/ALTER) with sp_rename and other functions, put GO in between + end.

-- MID-6417
ALTER TABLE m_operation_execution ADD recordType INT;

-- MID-3669
ALTER TABLE m_focus ADD lockoutStatus INT;

-- WRITE CHANGES ABOVE ^^
GO
UPDATE m_global_metadata SET value = '4.3' WHERE name = 'databaseSchemaVersion';

-- MID-6974
UPDATE QRTZ_JOB_DETAILS SET JOB_CLASS_NAME = 'com.evolveum.midpoint.task.quartzimpl.run.JobExecutor'
    WHERE JOB_CLASS_NAME = 'com.evolveum.midpoint.task.quartzimpl.execution.JobExecutor';

GO
