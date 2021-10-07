-- Never mix DDL (CREATE/UPDATE/ALTER) with sp_rename and other functions, put GO in between + end.

-- MID-7173
ALTER TABLE m_task ADD schedulingState INT;
ALTER TABLE m_task ADD autoScalingMode INT;
ALTER TABLE m_node ADD operationalState INT;

-- MID-7074
CREATE UNIQUE INDEX uc_connector_type_version_host
  ON m_connector (connectorType, connectorVersion, connectorHostRef_targetOid);

-- WRITE CHANGES ABOVE ^^
GO
UPDATE m_global_metadata SET value = '4.4' WHERE name = 'databaseSchemaVersion';

-- MID-6974
UPDATE QRTZ_JOB_DETAILS SET JOB_CLASS_NAME = 'com.evolveum.midpoint.task.quartzimpl.run.JobExecutor'
    WHERE JOB_CLASS_NAME = 'com.evolveum.midpoint.task.quartzimpl.execution.JobExecutor';

GO
