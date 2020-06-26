-- 2020-05-29 09:20

CREATE INDEX iOpExecTimestampValue
  ON m_operation_execution (timestampValue);

BEGIN TRANSACTION
UPDATE m_global_metadata SET value = '4.2' WHERE name = 'databaseSchemaVersion';
COMMIT;

-- 2020-06-25 11:35

UPDATE m_acc_cert_campaign SET definitionRef_type = convert(INT, definitionRef_targetType) where definitionRef_targetType is not null;
UPDATE m_acc_cert_campaign SET ownerRef_type = convert(INT, ownerRef_targetType) where ownerRef_targetType is not null;
UPDATE m_acc_cert_definition SET ownerRef_type = convert(INT, ownerRef_targetType) where ownerRef_targetType is not null;
UPDATE m_connector SET connectorHostRef_type = convert(INT, connectorHostRef_targetType) where connectorHostRef_targetType is not null;
UPDATE m_object SET creatorRef_type = convert(INT, creatorRef_targetType) where creatorRef_targetType is not null;
UPDATE m_object SET modifierRef_type = convert(INT, modifierRef_targetType) where modifierRef_targetType is not null;
UPDATE m_object SET tenantRef_type = convert(INT, tenantRef_targetType) where tenantRef_targetType is not null;
UPDATE m_report_output SET reportRef_type = convert(INT, reportRef_targetType) where reportRef_targetType is not null;
UPDATE m_resource SET connectorRef_type = convert(INT, connectorRef_targetType) where connectorRef_targetType is not null;
UPDATE m_shadow SET resourceRef_type = convert(INT, resourceRef_targetType) where resourceRef_targetType is not null;
UPDATE m_acc_cert_case SET objectRef_type = convert(INT, objectRef_targetType) where objectRef_targetType is not null;
UPDATE m_acc_cert_case SET orgRef_type = convert(INT, orgRef_targetType) where orgRef_targetType is not null;
UPDATE m_acc_cert_case SET targetRef_type = convert(INT, targetRef_targetType) where targetRef_targetType is not null;
UPDATE m_acc_cert_case SET tenantRef_type = convert(INT, tenantRef_targetType) where tenantRef_targetType is not null;
UPDATE m_acc_cert_wi SET performerRef_type = convert(INT, performerRef_targetType) where performerRef_targetType is not null;
UPDATE m_assignment SET creatorRef_type = convert(INT, creatorRef_targetType) where creatorRef_targetType is not null;
UPDATE m_assignment SET modifierRef_type = convert(INT, modifierRef_targetType) where modifierRef_targetType is not null;
UPDATE m_assignment SET orgRef_type = convert(INT, orgRef_targetType) where orgRef_targetType is not null;
UPDATE m_assignment SET resourceRef_type = convert(INT, resourceRef_targetType) where resourceRef_targetType is not null;
UPDATE m_assignment SET targetRef_type = convert(INT, targetRef_targetType) where targetRef_targetType is not null;
UPDATE m_assignment SET tenantRef_type = convert(INT, tenantRef_targetType) where tenantRef_targetType is not null;
UPDATE m_case_wi SET originalAssigneeRef_type = convert(INT, originalAssigneeRef_targetType) where originalAssigneeRef_targetType is not null;
UPDATE m_case_wi SET performerRef_type = convert(INT, performerRef_targetType) where performerRef_targetType is not null;
UPDATE m_operation_execution SET initiatorRef_type = convert(INT, initiatorRef_targetType) where initiatorRef_targetType is not null;
UPDATE m_operation_execution SET taskRef_type = convert(INT, taskRef_targetType) where taskRef_targetType is not null;
UPDATE m_task SET objectRef_type = convert(INT, objectRef_targetType) where objectRef_targetType is not null;
UPDATE m_task SET ownerRef_type = convert(INT, ownerRef_targetType) where ownerRef_targetType is not null;
UPDATE m_abstract_role SET ownerRef_type = convert(INT, ownerRef_targetType) where ownerRef_targetType is not null;
UPDATE m_case SET objectRef_type = convert(INT, objectRef_targetType) where objectRef_targetType is not null;
UPDATE m_case SET parentRef_type = convert(INT, parentRef_targetType) where parentRef_targetType is not null;
UPDATE m_case SET requestorRef_type = convert(INT, requestorRef_targetType) where requestorRef_targetType is not null;
UPDATE m_case SET targetRef_type = convert(INT, targetRef_targetType) where targetRef_targetType is not null;

GO
