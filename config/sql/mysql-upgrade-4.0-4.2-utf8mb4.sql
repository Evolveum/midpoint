-- 2020-05-29 09:20

CREATE INDEX iOpExecTimestampValue
  ON m_operation_execution (timestampValue);

UPDATE m_global_metadata SET value = '4.2' WHERE name = 'databaseSchemaVersion';

-- 2020-06-25 11:35

ALTER TABLE m_acc_cert_campaign CHANGE definitionRef_type definitionRef_targetType INTEGER;
ALTER TABLE m_acc_cert_campaign CHANGE ownerRef_type ownerRef_targetType INTEGER;
ALTER TABLE m_acc_cert_definition CHANGE ownerRef_type ownerRef_targetType INTEGER;
ALTER TABLE m_connector CHANGE connectorHostRef_type connectorHostRef_targetType INTEGER;
ALTER TABLE m_object CHANGE creatorRef_type creatorRef_targetType INTEGER;
ALTER TABLE m_object CHANGE modifierRef_type modifierRef_targetType INTEGER;
ALTER TABLE m_object CHANGE tenantRef_type tenantRef_targetType INTEGER;
ALTER TABLE m_report_output CHANGE reportRef_type reportRef_targetType INTEGER;
ALTER TABLE m_resource CHANGE connectorRef_type connectorRef_targetType INTEGER;
ALTER TABLE m_shadow CHANGE resourceRef_type resourceRef_targetType INTEGER;
ALTER TABLE m_acc_cert_case CHANGE objectRef_type objectRef_targetType INTEGER;
ALTER TABLE m_acc_cert_case CHANGE orgRef_type orgRef_targetType INTEGER;
ALTER TABLE m_acc_cert_case CHANGE targetRef_type targetRef_targetType INTEGER;
ALTER TABLE m_acc_cert_case CHANGE tenantRef_type tenantRef_targetType INTEGER;
ALTER TABLE m_acc_cert_wi CHANGE performerRef_type performerRef_targetType INTEGER;
ALTER TABLE m_assignment CHANGE creatorRef_type creatorRef_targetType INTEGER;
ALTER TABLE m_assignment CHANGE modifierRef_type modifierRef_targetType INTEGER;
ALTER TABLE m_assignment CHANGE orgRef_type orgRef_targetType INTEGER;
ALTER TABLE m_assignment CHANGE resourceRef_type resourceRef_targetType INTEGER;
ALTER TABLE m_assignment CHANGE targetRef_type targetRef_targetType INTEGER;
ALTER TABLE m_assignment CHANGE tenantRef_type tenantRef_targetType INTEGER;
ALTER TABLE m_case_wi CHANGE originalAssigneeRef_type originalAssigneeRef_targetType INTEGER;
ALTER TABLE m_case_wi CHANGE performerRef_type performerRef_targetType INTEGER;
ALTER TABLE m_operation_execution CHANGE initiatorRef_type initiatorRef_targetType INTEGER;
ALTER TABLE m_operation_execution CHANGE taskRef_type taskRef_targetType INTEGER;
ALTER TABLE m_task CHANGE objectRef_type objectRef_targetType INTEGER;
ALTER TABLE m_task CHANGE ownerRef_type ownerRef_targetType INTEGER;
ALTER TABLE m_abstract_role CHANGE ownerRef_type ownerRef_targetType INTEGER;
ALTER TABLE m_case CHANGE objectRef_type objectRef_targetType INTEGER;
ALTER TABLE m_case CHANGE parentRef_type parentRef_targetType INTEGER;
ALTER TABLE m_case CHANGE requestorRef_type requestorRef_targetType INTEGER;
ALTER TABLE m_case CHANGE targetRef_type targetRef_targetType INTEGER;

-- 2020-08-19 10:55

ALTER TABLE m_focus ADD COLUMN passwordCreateTimestamp DATETIME(6);
ALTER TABLE m_focus ADD COLUMN passwordModifyTimestamp DATETIME(6);

-- MID-6037
ALTER TABLE m_service ADD CONSTRAINT uc_service_name UNIQUE (name_norm);

-- MID-6232
CREATE INDEX iAuditEventRecordEStageTOid
  ON m_audit_event (eventStage, targetOid);

-- policySituation belong to M_OBJECT
ALTER TABLE m_focus_policy_situation DROP CONSTRAINT fk_focus_policy_situation;
ALTER TABLE m_focus_policy_situation RENAME TO m_object_policy_situation;
ALTER TABLE m_object_policy_situation CHANGE COLUMN focus_oid object_oid VARCHAR(36) CHARSET utf8 COLLATE utf8_bin NOT NULL;
ALTER TABLE m_object_policy_situation
  ADD CONSTRAINT fk_object_policy_situation FOREIGN KEY (object_oid) REFERENCES m_object (oid);

COMMIT;
