-- 2020-05-29 09:20

CREATE INDEX iOpExecTimestampValue
  ON m_operation_execution (timestampValue);

UPDATE m_global_metadata SET value = '4.2' WHERE name = 'databaseSchemaVersion';

-- 2020-06-25 11:35

ALTER TABLE m_acc_cert_campaign ALTER COLUMN definitionRef_type RENAME TO definitionRef_targetType;
ALTER TABLE m_acc_cert_campaign ALTER COLUMN ownerRef_type RENAME TO ownerRef_targetType;
ALTER TABLE m_acc_cert_definition ALTER COLUMN ownerRef_type RENAME TO ownerRef_targetType;
ALTER TABLE m_connector ALTER COLUMN connectorHostRef_type RENAME TO connectorHostRef_targetType;
ALTER TABLE m_object ALTER COLUMN creatorRef_type RENAME TO creatorRef_targetType;
ALTER TABLE m_object ALTER COLUMN modifierRef_type RENAME TO modifierRef_targetType;
ALTER TABLE m_object ALTER COLUMN tenantRef_type RENAME TO tenantRef_targetType;
ALTER TABLE m_report_output ALTER COLUMN reportRef_type RENAME TO reportRef_targetType;
ALTER TABLE m_resource ALTER COLUMN connectorRef_type RENAME TO connectorRef_targetType;
ALTER TABLE m_shadow ALTER COLUMN resourceRef_type RENAME TO resourceRef_targetType;
ALTER TABLE m_acc_cert_case ALTER COLUMN objectRef_type RENAME TO objectRef_targetType;
ALTER TABLE m_acc_cert_case ALTER COLUMN orgRef_type RENAME TO orgRef_targetType;
ALTER TABLE m_acc_cert_case ALTER COLUMN targetRef_type RENAME TO targetRef_targetType;
ALTER TABLE m_acc_cert_case ALTER COLUMN tenantRef_type RENAME TO tenantRef_targetType;
ALTER TABLE m_acc_cert_wi ALTER COLUMN performerRef_type RENAME TO performerRef_targetType;
ALTER TABLE m_assignment ALTER COLUMN creatorRef_type RENAME TO creatorRef_targetType;
ALTER TABLE m_assignment ALTER COLUMN modifierRef_type RENAME TO modifierRef_targetType;
ALTER TABLE m_assignment ALTER COLUMN orgRef_type RENAME TO orgRef_targetType;
ALTER TABLE m_assignment ALTER COLUMN resourceRef_type RENAME TO resourceRef_targetType;
ALTER TABLE m_assignment ALTER COLUMN targetRef_type RENAME TO targetRef_targetType;
ALTER TABLE m_assignment ALTER COLUMN tenantRef_type RENAME TO tenantRef_targetType;
ALTER TABLE m_case_wi ALTER COLUMN originalAssigneeRef_type RENAME TO originalAssigneeRef_targetType;
ALTER TABLE m_case_wi ALTER COLUMN performerRef_type RENAME TO performerRef_targetType;
ALTER TABLE m_operation_execution ALTER COLUMN initiatorRef_type RENAME TO initiatorRef_targetType;
ALTER TABLE m_operation_execution ALTER COLUMN taskRef_type RENAME TO taskRef_targetType;
ALTER TABLE m_task ALTER COLUMN objectRef_type RENAME TO objectRef_targetType;
ALTER TABLE m_task ALTER COLUMN ownerRef_type RENAME TO ownerRef_targetType;
ALTER TABLE m_abstract_role ALTER COLUMN ownerRef_type RENAME TO ownerRef_targetType;
ALTER TABLE m_case ALTER COLUMN objectRef_type RENAME TO objectRef_targetType;
ALTER TABLE m_case ALTER COLUMN parentRef_type RENAME TO parentRef_targetType;
ALTER TABLE m_case ALTER COLUMN requestorRef_type RENAME TO requestorRef_targetType;
ALTER TABLE m_case ALTER COLUMN targetRef_type RENAME TO targetRef_targetType;

COMMIT;
