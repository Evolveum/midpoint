create table m_acc_cert_campaign (definitionRef_relation nvarchar(157) collate database_default, definitionRef_targetOid nvarchar(36) collate database_default, definitionRef_type int, endTimestamp datetime2, handlerUri nvarchar(255) collate database_default, name_norm nvarchar(255) collate database_default, name_orig nvarchar(255) collate database_default, ownerRef_relation nvarchar(157) collate database_default, ownerRef_targetOid nvarchar(36) collate database_default, ownerRef_type int, stageNumber int, startTimestamp datetime2, state int, oid nvarchar(36) collate database_default not null, primary key (oid));
create table m_acc_cert_case (id int not null, owner_oid nvarchar(36) collate database_default not null, administrativeStatus int, archiveTimestamp datetime2, disableReason nvarchar(255) collate database_default, disableTimestamp datetime2, effectiveStatus int, enableTimestamp datetime2, validFrom datetime2, validTo datetime2, validityChangeTimestamp datetime2, validityStatus int, currentStageOutcome nvarchar(255) collate database_default, fullObject varbinary(MAX), objectRef_relation nvarchar(157) collate database_default, objectRef_targetOid nvarchar(36) collate database_default, objectRef_type int, orgRef_relation nvarchar(157) collate database_default, orgRef_targetOid nvarchar(36) collate database_default, orgRef_type int, outcome nvarchar(255) collate database_default, remediedTimestamp datetime2, reviewDeadline datetime2, reviewRequestedTimestamp datetime2, stageNumber int, targetRef_relation nvarchar(157) collate database_default, targetRef_targetOid nvarchar(36) collate database_default, targetRef_type int, tenantRef_relation nvarchar(157) collate database_default, tenantRef_targetOid nvarchar(36) collate database_default, tenantRef_type int, primary key (id, owner_oid));
create table m_acc_cert_definition (handlerUri nvarchar(255) collate database_default, lastCampaignClosedTimestamp datetime2, lastCampaignStartedTimestamp datetime2, name_norm nvarchar(255) collate database_default, name_orig nvarchar(255) collate database_default, ownerRef_relation nvarchar(157) collate database_default, ownerRef_targetOid nvarchar(36) collate database_default, ownerRef_type int, oid nvarchar(36) collate database_default not null, primary key (oid));
create table m_acc_cert_wi (id int not null, owner_id int not null, owner_owner_oid nvarchar(36) collate database_default not null, closeTimestamp datetime2, outcome nvarchar(255) collate database_default, outputChangeTimestamp datetime2, performerRef_relation nvarchar(157) collate database_default, performerRef_targetOid nvarchar(36) collate database_default, performerRef_type int, stageNumber int, primary key (id, owner_id, owner_owner_oid));
create table m_acc_cert_wi_reference (owner_id int not null, owner_owner_id int not null, owner_owner_owner_oid nvarchar(36) collate database_default not null, relation nvarchar(157) collate database_default not null, targetOid nvarchar(36) collate database_default not null, targetType int, primary key (owner_id, owner_owner_id, owner_owner_owner_oid, relation, targetOid));
create table m_assignment (id int not null, owner_oid nvarchar(36) collate database_default not null, administrativeStatus int, archiveTimestamp datetime2, disableReason nvarchar(255) collate database_default, disableTimestamp datetime2, effectiveStatus int, enableTimestamp datetime2, validFrom datetime2, validTo datetime2, validityChangeTimestamp datetime2, validityStatus int, assignmentOwner int, createChannel nvarchar(255) collate database_default, createTimestamp datetime2, creatorRef_relation nvarchar(157) collate database_default, creatorRef_targetOid nvarchar(36) collate database_default, creatorRef_type int, lifecycleState nvarchar(255) collate database_default, modifierRef_relation nvarchar(157) collate database_default, modifierRef_targetOid nvarchar(36) collate database_default, modifierRef_type int, modifyChannel nvarchar(255) collate database_default, modifyTimestamp datetime2, orderValue int, orgRef_relation nvarchar(157) collate database_default, orgRef_targetOid nvarchar(36) collate database_default, orgRef_type int, resourceRef_relation nvarchar(157) collate database_default, resourceRef_targetOid nvarchar(36) collate database_default, resourceRef_type int, targetRef_relation nvarchar(157) collate database_default, targetRef_targetOid nvarchar(36) collate database_default, targetRef_type int, tenantRef_relation nvarchar(157) collate database_default, tenantRef_targetOid nvarchar(36) collate database_default, tenantRef_type int, extId int, extOid nvarchar(36) collate database_default, primary key (id, owner_oid));
create table m_assignment_ext_boolean (eName nvarchar(157) collate database_default not null, anyContainer_owner_id int not null, anyContainer_owner_owner_oid nvarchar(36) collate database_default not null, booleanValue bit not null, extensionType int, dynamicDef bit, eType nvarchar(157) collate database_default, valueType int, primary key (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, booleanValue));
create table m_assignment_ext_date (eName nvarchar(157) collate database_default not null, anyContainer_owner_id int not null, anyContainer_owner_owner_oid nvarchar(36) collate database_default not null, dateValue datetime2 not null, extensionType int, dynamicDef bit, eType nvarchar(157) collate database_default, valueType int, primary key (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, dateValue));
create table m_assignment_ext_long (eName nvarchar(157) collate database_default not null, anyContainer_owner_id int not null, anyContainer_owner_owner_oid nvarchar(36) collate database_default not null, longValue bigint not null, extensionType int, dynamicDef bit, eType nvarchar(157) collate database_default, valueType int, primary key (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, longValue));
create table m_assignment_ext_poly (eName nvarchar(157) collate database_default not null, anyContainer_owner_id int not null, anyContainer_owner_owner_oid nvarchar(36) collate database_default not null, orig nvarchar(255) collate database_default not null, extensionType int, dynamicDef bit, norm nvarchar(255) collate database_default, eType nvarchar(157) collate database_default, valueType int, primary key (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, orig));
create table m_assignment_ext_reference (eName nvarchar(157) collate database_default not null, anyContainer_owner_id int not null, anyContainer_owner_owner_oid nvarchar(36) collate database_default not null, targetoid nvarchar(36) collate database_default not null, extensionType int, dynamicDef bit, relation nvarchar(157) collate database_default, targetType int, eType nvarchar(157) collate database_default, valueType int, primary key (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, targetoid));
create table m_assignment_ext_string (eName nvarchar(157) collate database_default not null, anyContainer_owner_id int not null, anyContainer_owner_owner_oid nvarchar(36) collate database_default not null, stringValue nvarchar(255) collate database_default not null, extensionType int, dynamicDef bit, eType nvarchar(157) collate database_default, valueType int, primary key (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, stringValue));
create table m_assignment_extension (owner_id int not null, owner_owner_oid nvarchar(36) collate database_default not null, booleansCount smallint, datesCount smallint, longsCount smallint, polysCount smallint, referencesCount smallint, stringsCount smallint, primary key (owner_id, owner_owner_oid));
create table m_assignment_policy_situation (assignment_id int not null, assignment_oid nvarchar(36) collate database_default not null, policySituation nvarchar(255) collate database_default);
create table m_assignment_reference (owner_id int not null, owner_owner_oid nvarchar(36) collate database_default not null, reference_type int not null, relation nvarchar(157) collate database_default not null, targetOid nvarchar(36) collate database_default not null, targetType int, primary key (owner_id, owner_owner_oid, reference_type, relation, targetOid));
create table m_audit_delta (checksum nvarchar(32) collate database_default not null, record_id bigint not null, delta nvarchar(MAX), deltaOid nvarchar(36) collate database_default, deltaType int, fullResult nvarchar(MAX), objectName_norm nvarchar(255) collate database_default, objectName_orig nvarchar(255) collate database_default, resourceName_norm nvarchar(255) collate database_default, resourceName_orig nvarchar(255) collate database_default, resourceOid nvarchar(36) collate database_default, status int, primary key (checksum, record_id));
create table m_audit_event (id bigint identity not null, attorneyName nvarchar(255) collate database_default, attorneyOid nvarchar(36) collate database_default, channel nvarchar(255) collate database_default, eventIdentifier nvarchar(255) collate database_default, eventStage int, eventType int, hostIdentifier nvarchar(255) collate database_default, initiatorName nvarchar(255) collate database_default, initiatorOid nvarchar(36) collate database_default, initiatorType int, message nvarchar(1024) collate database_default, nodeIdentifier nvarchar(255) collate database_default, outcome int, parameter nvarchar(255) collate database_default, remoteHostAddress nvarchar(255) collate database_default, result nvarchar(255) collate database_default, sessionIdentifier nvarchar(255) collate database_default, targetName nvarchar(255) collate database_default, targetOid nvarchar(36) collate database_default, targetOwnerName nvarchar(255) collate database_default, targetOwnerOid nvarchar(36) collate database_default, targetType int, taskIdentifier nvarchar(255) collate database_default, taskOID nvarchar(255) collate database_default, timestampValue datetime2, primary key (id));
create table m_audit_item (changedItemPath nvarchar(900) collate database_default not null, record_id bigint not null, primary key (changedItemPath, record_id));
create table m_audit_prop_value (id bigint identity not null, name nvarchar(255) collate database_default, record_id bigint, value nvarchar(1024) collate database_default, primary key (id));
create table m_audit_ref_value (id bigint identity not null, name nvarchar(255) collate database_default, oid nvarchar(255) collate database_default, record_id bigint, targetName_norm nvarchar(255) collate database_default, targetName_orig nvarchar(255) collate database_default, type nvarchar(255) collate database_default, primary key (id));
create table m_connector_target_system (connector_oid nvarchar(36) collate database_default not null, targetSystemType nvarchar(255) collate database_default);
create table m_focus_photo (owner_oid nvarchar(36) collate database_default not null, photo varbinary(MAX), primary key (owner_oid));
create table m_focus_policy_situation (focus_oid nvarchar(36) collate database_default not null, policySituation nvarchar(255) collate database_default);
create table m_object (oid nvarchar(36) collate database_default not null, booleansCount smallint, createChannel nvarchar(255) collate database_default, createTimestamp datetime2, creatorRef_relation nvarchar(157) collate database_default, creatorRef_targetOid nvarchar(36) collate database_default, creatorRef_type int, datesCount smallint, fullObject varbinary(MAX), lifecycleState nvarchar(255) collate database_default, longsCount smallint, modifierRef_relation nvarchar(157) collate database_default, modifierRef_targetOid nvarchar(36) collate database_default, modifierRef_type int, modifyChannel nvarchar(255) collate database_default, modifyTimestamp datetime2, name_norm nvarchar(255) collate database_default, name_orig nvarchar(255) collate database_default, objectTypeClass int, polysCount smallint, referencesCount smallint, stringsCount smallint, tenantRef_relation nvarchar(157) collate database_default, tenantRef_targetOid nvarchar(36) collate database_default, tenantRef_type int, version int not null, primary key (oid));
create table m_object_ext_boolean (eName nvarchar(157) collate database_default not null, owner_oid nvarchar(36) collate database_default not null, ownerType int not null, booleanValue bit not null, dynamicDef bit, eType nvarchar(157) collate database_default, valueType int, primary key (eName, owner_oid, ownerType, booleanValue));
create table m_object_ext_date (eName nvarchar(157) collate database_default not null, owner_oid nvarchar(36) collate database_default not null, ownerType int not null, dateValue datetime2 not null, dynamicDef bit, eType nvarchar(157) collate database_default, valueType int, primary key (eName, owner_oid, ownerType, dateValue));
create table m_object_ext_long (eName nvarchar(157) collate database_default not null, owner_oid nvarchar(36) collate database_default not null, ownerType int not null, longValue bigint not null, dynamicDef bit, eType nvarchar(157) collate database_default, valueType int, primary key (eName, owner_oid, ownerType, longValue));
create table m_object_ext_poly (eName nvarchar(157) collate database_default not null, owner_oid nvarchar(36) collate database_default not null, ownerType int not null, orig nvarchar(255) collate database_default not null, dynamicDef bit, norm nvarchar(255) collate database_default, eType nvarchar(157) collate database_default, valueType int, primary key (eName, owner_oid, ownerType, orig));
create table m_object_ext_reference (eName nvarchar(157) collate database_default not null, owner_oid nvarchar(36) collate database_default not null, ownerType int not null, targetoid nvarchar(36) collate database_default not null, dynamicDef bit, relation nvarchar(157) collate database_default, targetType int, eType nvarchar(157) collate database_default, valueType int, primary key (eName, owner_oid, ownerType, targetoid));
create table m_object_ext_string (eName nvarchar(157) collate database_default not null, owner_oid nvarchar(36) collate database_default not null, ownerType int not null, stringValue nvarchar(255) collate database_default not null, dynamicDef bit, eType nvarchar(157) collate database_default, valueType int, primary key (eName, owner_oid, ownerType, stringValue));
create table m_object_text_info (owner_oid nvarchar(36) collate database_default not null, text nvarchar(255) collate database_default not null, primary key (owner_oid, text));
create table m_operation_execution (id int not null, owner_oid nvarchar(36) collate database_default not null, initiatorRef_relation nvarchar(157) collate database_default, initiatorRef_targetOid nvarchar(36) collate database_default, initiatorRef_type int, status int, taskRef_relation nvarchar(157) collate database_default, taskRef_targetOid nvarchar(36) collate database_default, taskRef_type int, timestampValue datetime2, primary key (id, owner_oid));
create table m_org_closure (ancestor_oid nvarchar(36) collate database_default not null, descendant_oid nvarchar(36) collate database_default not null, val int, primary key (ancestor_oid, descendant_oid));
create table m_org_org_type (org_oid nvarchar(36) collate database_default not null, orgType nvarchar(255) collate database_default);
create table m_reference (owner_oid nvarchar(36) collate database_default not null, reference_type int not null, relation nvarchar(157) collate database_default not null, targetOid nvarchar(36) collate database_default not null, targetType int, primary key (owner_oid, reference_type, relation, targetOid));
create table m_service_type (service_oid nvarchar(36) collate database_default not null, serviceType nvarchar(255) collate database_default);
create table m_shadow (attemptNumber int, dead bit, exist bit, failedOperationType int, fullSynchronizationTimestamp datetime2, intent nvarchar(255) collate database_default, kind int, name_norm nvarchar(255) collate database_default, name_orig nvarchar(255) collate database_default, objectClass nvarchar(157) collate database_default, pendingOperationCount int, resourceRef_relation nvarchar(157) collate database_default, resourceRef_targetOid nvarchar(36) collate database_default, resourceRef_type int, status int, synchronizationSituation int, synchronizationTimestamp datetime2, oid nvarchar(36) collate database_default not null, primary key (oid));
create table m_task (binding int, canRunOnNode nvarchar(255) collate database_default, category nvarchar(255) collate database_default, completionTimestamp datetime2, executionStatus int, handlerUri nvarchar(255) collate database_default, lastRunFinishTimestamp datetime2, lastRunStartTimestamp datetime2, name_norm nvarchar(255) collate database_default, name_orig nvarchar(255) collate database_default, node nvarchar(255) collate database_default, objectRef_relation nvarchar(157) collate database_default, objectRef_targetOid nvarchar(36) collate database_default, objectRef_type int, ownerRef_relation nvarchar(157) collate database_default, ownerRef_targetOid nvarchar(36) collate database_default, ownerRef_type int, parent nvarchar(255) collate database_default, recurrence int, status int, taskIdentifier nvarchar(255) collate database_default, threadStopAction int, waitingReason int, wfEndTimestamp datetime2, wfObjectRef_relation nvarchar(157) collate database_default, wfObjectRef_targetOid nvarchar(36) collate database_default, wfObjectRef_type int, wfProcessInstanceId nvarchar(255) collate database_default, wfRequesterRef_relation nvarchar(157) collate database_default, wfRequesterRef_targetOid nvarchar(36) collate database_default, wfRequesterRef_type int, wfStartTimestamp datetime2, wfTargetRef_relation nvarchar(157) collate database_default, wfTargetRef_targetOid nvarchar(36) collate database_default, wfTargetRef_type int, oid nvarchar(36) collate database_default not null, primary key (oid));
create table m_task_dependent (task_oid nvarchar(36) collate database_default not null, dependent nvarchar(255) collate database_default);
create table m_user_employee_type (user_oid nvarchar(36) collate database_default not null, employeeType nvarchar(255) collate database_default);
create table m_user_organization (user_oid nvarchar(36) collate database_default not null, norm nvarchar(255) collate database_default, orig nvarchar(255) collate database_default);
create table m_user_organizational_unit (user_oid nvarchar(36) collate database_default not null, norm nvarchar(255) collate database_default, orig nvarchar(255) collate database_default);
create table m_abstract_role (approvalProcess nvarchar(255) collate database_default, autoassign_enabled bit, displayName_norm nvarchar(255) collate database_default, displayName_orig nvarchar(255) collate database_default, identifier nvarchar(255) collate database_default, ownerRef_relation nvarchar(157) collate database_default, ownerRef_targetOid nvarchar(36) collate database_default, ownerRef_type int, requestable bit, riskLevel nvarchar(255) collate database_default, oid nvarchar(36) collate database_default not null, primary key (oid));
create table m_case (name_norm nvarchar(255) collate database_default, name_orig nvarchar(255) collate database_default, oid nvarchar(36) collate database_default not null, primary key (oid));
create table m_connector (connectorBundle nvarchar(255) collate database_default, connectorHostRef_relation nvarchar(157) collate database_default, connectorHostRef_targetOid nvarchar(36) collate database_default, connectorHostRef_type int, connectorType nvarchar(255) collate database_default, connectorVersion nvarchar(255) collate database_default, framework nvarchar(255) collate database_default, name_norm nvarchar(255) collate database_default, name_orig nvarchar(255) collate database_default, oid nvarchar(36) collate database_default not null, primary key (oid));
create table m_connector_host (hostname nvarchar(255) collate database_default, name_norm nvarchar(255) collate database_default, name_orig nvarchar(255) collate database_default, port nvarchar(255) collate database_default, oid nvarchar(36) collate database_default not null, primary key (oid));
create table m_exclusion (id int not null, owner_oid nvarchar(36) collate database_default not null, policy int, targetRef_relation nvarchar(157) collate database_default, targetRef_targetOid nvarchar(36) collate database_default, targetRef_type int, primary key (id, owner_oid));
create table m_focus (administrativeStatus int, archiveTimestamp datetime2, disableReason nvarchar(255) collate database_default, disableTimestamp datetime2, effectiveStatus int, enableTimestamp datetime2, validFrom datetime2, validTo datetime2, validityChangeTimestamp datetime2, validityStatus int, hasPhoto bit default false not null, oid nvarchar(36) collate database_default not null, primary key (oid));
create table m_form (name_norm nvarchar(255) collate database_default, name_orig nvarchar(255) collate database_default, oid nvarchar(36) collate database_default not null, primary key (oid));
create table m_function_library (name_norm nvarchar(255) collate database_default, name_orig nvarchar(255) collate database_default, oid nvarchar(36) collate database_default not null, primary key (oid));
create table m_generic_object (name_norm nvarchar(255) collate database_default, name_orig nvarchar(255) collate database_default, objectType nvarchar(255) collate database_default, oid nvarchar(36) collate database_default not null, primary key (oid));
create table m_lookup_table (name_norm nvarchar(255) collate database_default, name_orig nvarchar(255) collate database_default, oid nvarchar(36) collate database_default not null, primary key (oid));
create table m_lookup_table_row (id int not null, owner_oid nvarchar(36) collate database_default not null, row_key nvarchar(255) collate database_default, label_norm nvarchar(255) collate database_default, label_orig nvarchar(255) collate database_default, lastChangeTimestamp datetime2, row_value nvarchar(255) collate database_default, primary key (id, owner_oid));
create table m_node (name_norm nvarchar(255) collate database_default, name_orig nvarchar(255) collate database_default, nodeIdentifier nvarchar(255) collate database_default, oid nvarchar(36) collate database_default not null, primary key (oid));
create table m_object_template (name_norm nvarchar(255) collate database_default, name_orig nvarchar(255) collate database_default, type int, oid nvarchar(36) collate database_default not null, primary key (oid));
create table m_org (costCenter nvarchar(255) collate database_default, displayOrder int, locality_norm nvarchar(255) collate database_default, locality_orig nvarchar(255) collate database_default, name_norm nvarchar(255) collate database_default, name_orig nvarchar(255) collate database_default, tenant bit, oid nvarchar(36) collate database_default not null, primary key (oid));
create table m_report (export int, name_norm nvarchar(255) collate database_default, name_orig nvarchar(255) collate database_default, orientation int, parent bit, useHibernateSession bit, oid nvarchar(36) collate database_default not null, primary key (oid));
create table m_report_output (name_norm nvarchar(255) collate database_default, name_orig nvarchar(255) collate database_default, reportRef_relation nvarchar(157) collate database_default, reportRef_targetOid nvarchar(36) collate database_default, reportRef_type int, oid nvarchar(36) collate database_default not null, primary key (oid));
create table m_resource (administrativeState int, connectorRef_relation nvarchar(157) collate database_default, connectorRef_targetOid nvarchar(36) collate database_default, connectorRef_type int, name_norm nvarchar(255) collate database_default, name_orig nvarchar(255) collate database_default, o16_lastAvailabilityStatus int, oid nvarchar(36) collate database_default not null, primary key (oid));
create table m_role (name_norm nvarchar(255) collate database_default, name_orig nvarchar(255) collate database_default, roleType nvarchar(255) collate database_default, oid nvarchar(36) collate database_default not null, primary key (oid));
create table m_security_policy (name_norm nvarchar(255) collate database_default, name_orig nvarchar(255) collate database_default, oid nvarchar(36) collate database_default not null, primary key (oid));
create table m_sequence (name_norm nvarchar(255) collate database_default, name_orig nvarchar(255) collate database_default, oid nvarchar(36) collate database_default not null, primary key (oid));
create table m_service (displayOrder int, locality_norm nvarchar(255) collate database_default, locality_orig nvarchar(255) collate database_default, name_norm nvarchar(255) collate database_default, name_orig nvarchar(255) collate database_default, oid nvarchar(36) collate database_default not null, primary key (oid));
create table m_system_configuration (name_norm nvarchar(255) collate database_default, name_orig nvarchar(255) collate database_default, oid nvarchar(36) collate database_default not null, primary key (oid));
create table m_trigger (id int not null, owner_oid nvarchar(36) collate database_default not null, handlerUri nvarchar(255) collate database_default, timestampValue datetime2, primary key (id, owner_oid));
create table m_user (additionalName_norm nvarchar(255) collate database_default, additionalName_orig nvarchar(255) collate database_default, costCenter nvarchar(255) collate database_default, emailAddress nvarchar(255) collate database_default, employeeNumber nvarchar(255) collate database_default, familyName_norm nvarchar(255) collate database_default, familyName_orig nvarchar(255) collate database_default, fullName_norm nvarchar(255) collate database_default, fullName_orig nvarchar(255) collate database_default, givenName_norm nvarchar(255) collate database_default, givenName_orig nvarchar(255) collate database_default, honorificPrefix_norm nvarchar(255) collate database_default, honorificPrefix_orig nvarchar(255) collate database_default, honorificSuffix_norm nvarchar(255) collate database_default, honorificSuffix_orig nvarchar(255) collate database_default, locale nvarchar(255) collate database_default, locality_norm nvarchar(255) collate database_default, locality_orig nvarchar(255) collate database_default, name_norm nvarchar(255) collate database_default, name_orig nvarchar(255) collate database_default, nickName_norm nvarchar(255) collate database_default, nickName_orig nvarchar(255) collate database_default, preferredLanguage nvarchar(255) collate database_default, status int, telephoneNumber nvarchar(255) collate database_default, timezone nvarchar(255) collate database_default, title_norm nvarchar(255) collate database_default, title_orig nvarchar(255) collate database_default, oid nvarchar(36) collate database_default not null, primary key (oid));
create table m_value_policy (name_norm nvarchar(255) collate database_default, name_orig nvarchar(255) collate database_default, oid nvarchar(36) collate database_default not null, primary key (oid));
alter table m_acc_cert_campaign add constraint uc_acc_cert_campaign_name unique (name_norm);
create index iCaseObjectRefTargetOid on m_acc_cert_case (objectRef_targetOid);
create index iCaseTargetRefTargetOid on m_acc_cert_case (targetRef_targetOid);
create index iCaseTenantRefTargetOid on m_acc_cert_case (tenantRef_targetOid);
create index iCaseOrgRefTargetOid on m_acc_cert_case (orgRef_targetOid);
alter table m_acc_cert_definition add constraint uc_acc_cert_definition_name unique (name_norm);
create index iCertWorkItemRefTargetOid on m_acc_cert_wi_reference (targetOid);
create index iAssignmentAdministrative on m_assignment (administrativeStatus);
create index iAssignmentEffective on m_assignment (effectiveStatus);
create index iTargetRefTargetOid on m_assignment (targetRef_targetOid);
create index iTenantRefTargetOid on m_assignment (tenantRef_targetOid);
create index iOrgRefTargetOid on m_assignment (orgRef_targetOid);
create index iResourceRefTargetOid on m_assignment (resourceRef_targetOid);
create index iAExtensionBoolean on m_assignment_ext_boolean (extensionType, eName, booleanValue);
create index iAExtensionDate on m_assignment_ext_date (extensionType, eName, dateValue);
create index iAExtensionLong on m_assignment_ext_long (extensionType, eName, longValue);
create index iAExtensionPolyString on m_assignment_ext_poly (extensionType, eName, orig);
create index iAExtensionReference on m_assignment_ext_reference (extensionType, eName, targetoid);
create index iAExtensionString on m_assignment_ext_string (extensionType, eName, stringValue);
create index iAssignmentReferenceTargetOid on m_assignment_reference (targetOid);
create index iTimestampValue on m_audit_event (timestampValue);
create index iChangedItemPath on m_audit_item (changedItemPath);
create index iAuditPropValRecordId on m_audit_prop_value (record_id);
create index iAuditRefValRecordId on m_audit_ref_value (record_id);
create index iObjectNameOrig on m_object (name_orig);
create index iObjectNameNorm on m_object (name_norm);
create index iObjectTypeClass on m_object (objectTypeClass);
create index iObjectCreateTimestamp on m_object (createTimestamp);
create index iObjectLifecycleState on m_object (lifecycleState);
create index iExtensionBoolean on m_object_ext_boolean (ownerType, eName, booleanValue);
create index iExtensionBooleanDef on m_object_ext_boolean (owner_oid, ownerType);
create index iExtensionDate on m_object_ext_date (ownerType, eName, dateValue);
create index iExtensionDateDef on m_object_ext_date (owner_oid, ownerType);
create index iExtensionLong on m_object_ext_long (ownerType, eName, longValue);
create index iExtensionLongDef on m_object_ext_long (owner_oid, ownerType);
create index iExtensionPolyString on m_object_ext_poly (ownerType, eName, orig);
create index iExtensionPolyStringDef on m_object_ext_poly (owner_oid, ownerType);
create index iExtensionReference on m_object_ext_reference (ownerType, eName, targetoid);
create index iExtensionReferenceDef on m_object_ext_reference (owner_oid, ownerType);
create index iExtensionString on m_object_ext_string (ownerType, eName, stringValue);
create index iExtensionStringDef on m_object_ext_string (owner_oid, ownerType);
create index iOpExecTaskOid on m_operation_execution (taskRef_targetOid);
create index iOpExecInitiatorOid on m_operation_execution (initiatorRef_targetOid);
create index iOpExecStatus on m_operation_execution (status);
create index iOpExecOwnerOid on m_operation_execution (owner_oid);
create index iAncestor on m_org_closure (ancestor_oid);
create index iDescendant on m_org_closure (descendant_oid);
create index iDescendantAncestor on m_org_closure (descendant_oid, ancestor_oid);
create index iReferenceTargetOid on m_reference (targetOid);
create index iShadowResourceRef on m_shadow (resourceRef_targetOid);
create index iShadowDead on m_shadow (dead);
create index iShadowKind on m_shadow (kind);
create index iShadowIntent on m_shadow (intent);
create index iShadowObjectClass on m_shadow (objectClass);
create index iShadowFailedOperationType on m_shadow (failedOperationType);
create index iShadowSyncSituation on m_shadow (synchronizationSituation);
create index iShadowPendingOperationCount on m_shadow (pendingOperationCount);
create index iParent on m_task (parent);
create index iTaskWfProcessInstanceId on m_task (wfProcessInstanceId);
create index iTaskWfStartTimestamp on m_task (wfStartTimestamp);
create index iTaskWfEndTimestamp on m_task (wfEndTimestamp);
create index iTaskWfRequesterOid on m_task (wfRequesterRef_targetOid);
create index iTaskWfObjectOid on m_task (wfObjectRef_targetOid);
create index iTaskWfTargetOid on m_task (wfTargetRef_targetOid);
alter table m_task add constraint uc_task_identifier unique (taskIdentifier);
create index iAbstractRoleIdentifier on m_abstract_role (identifier);
create index iRequestable on m_abstract_role (requestable);
create index iAutoassignEnabled on m_abstract_role (autoassign_enabled);
alter table m_case add constraint uc_case_name unique (name_norm);
alter table m_connector_host add constraint uc_connector_host_name unique (name_norm);
create index iFocusAdministrative on m_focus (administrativeStatus);
create index iFocusEffective on m_focus (effectiveStatus);
alter table m_form add constraint uc_form_name unique (name_norm);
alter table m_function_library add constraint uc_function_library_name unique (name_norm);
alter table m_generic_object add constraint uc_generic_object_name unique (name_norm);
alter table m_lookup_table add constraint uc_lookup_name unique (name_norm);
alter table m_lookup_table_row add constraint uc_row_key unique (owner_oid, row_key);
alter table m_node add constraint uc_node_name unique (name_norm);
alter table m_object_template add constraint uc_object_template_name unique (name_norm);
create index iDisplayOrder on m_org (displayOrder);
alter table m_org add constraint uc_org_name unique (name_norm);
create index iReportParent on m_report (parent);
alter table m_report add constraint uc_report_name unique (name_norm);
alter table m_resource add constraint uc_resource_name unique (name_norm);
alter table m_role add constraint uc_role_name unique (name_norm);
alter table m_security_policy add constraint uc_security_policy_name unique (name_norm);
alter table m_sequence add constraint uc_sequence_name unique (name_norm);
alter table m_system_configuration add constraint uc_system_configuration_name unique (name_norm);
create index iTriggerTimestamp on m_trigger (timestampValue);
create index iEmployeeNumber on m_user (employeeNumber);
create index iFullName on m_user (fullName_orig);
create index iFamilyName on m_user (familyName_orig);
create index iGivenName on m_user (givenName_orig);
create index iLocality on m_user (locality_orig);
alter table m_user add constraint uc_user_name unique (name_norm);
alter table m_value_policy add constraint uc_value_policy_name unique (name_norm);
alter table m_acc_cert_campaign add constraint fk_acc_cert_campaign foreign key (oid) references m_object;
alter table m_acc_cert_case add constraint fk_acc_cert_case_owner foreign key (owner_oid) references m_acc_cert_campaign;
alter table m_acc_cert_definition add constraint fk_acc_cert_definition foreign key (oid) references m_object;
alter table m_acc_cert_wi add constraint fk_acc_cert_wi_owner foreign key (owner_id, owner_owner_oid) references m_acc_cert_case;
alter table m_acc_cert_wi_reference add constraint fk_acc_cert_wi_ref_owner foreign key (owner_id, owner_owner_id, owner_owner_owner_oid) references m_acc_cert_wi;
alter table m_assignment add constraint fk_assignment_owner foreign key (owner_oid) references m_object;
alter table m_assignment_ext_boolean add constraint fk_assignment_ext_boolean foreign key (anyContainer_owner_id, anyContainer_owner_owner_oid) references m_assignment_extension;
alter table m_assignment_ext_date add constraint fk_assignment_ext_date foreign key (anyContainer_owner_id, anyContainer_owner_owner_oid) references m_assignment_extension;
alter table m_assignment_ext_long add constraint fk_assignment_ext_long foreign key (anyContainer_owner_id, anyContainer_owner_owner_oid) references m_assignment_extension;
alter table m_assignment_ext_poly add constraint fk_assignment_ext_poly foreign key (anyContainer_owner_id, anyContainer_owner_owner_oid) references m_assignment_extension;
alter table m_assignment_ext_reference add constraint fk_assignment_ext_reference foreign key (anyContainer_owner_id, anyContainer_owner_owner_oid) references m_assignment_extension;
alter table m_assignment_ext_string add constraint fk_assignment_ext_string foreign key (anyContainer_owner_id, anyContainer_owner_owner_oid) references m_assignment_extension;
alter table m_assignment_policy_situation add constraint fk_assignment_policy_situation foreign key (assignment_id, assignment_oid) references m_assignment;
alter table m_assignment_reference add constraint fk_assignment_reference foreign key (owner_id, owner_owner_oid) references m_assignment;
alter table m_audit_delta add constraint fk_audit_delta foreign key (record_id) references m_audit_event;
alter table m_audit_item add constraint fk_audit_item foreign key (record_id) references m_audit_event;
alter table m_audit_prop_value add constraint fk_audit_prop_value foreign key (record_id) references m_audit_event;
alter table m_audit_ref_value add constraint fk_audit_ref_value foreign key (record_id) references m_audit_event;
alter table m_connector_target_system add constraint fk_connector_target_system foreign key (connector_oid) references m_connector;
alter table m_focus_photo add constraint fk_focus_photo foreign key (owner_oid) references m_focus;
alter table m_focus_policy_situation add constraint fk_focus_policy_situation foreign key (focus_oid) references m_focus;
alter table m_object_ext_boolean add constraint fk_object_ext_boolean foreign key (owner_oid) references m_object;
alter table m_object_ext_date add constraint fk_object_ext_date foreign key (owner_oid) references m_object;
alter table m_object_ext_long add constraint fk_object_ext_long foreign key (owner_oid) references m_object;
alter table m_object_ext_poly add constraint fk_object_ext_poly foreign key (owner_oid) references m_object;
alter table m_object_ext_reference add constraint fk_object_ext_reference foreign key (owner_oid) references m_object;
alter table m_object_ext_string add constraint fk_object_ext_string foreign key (owner_oid) references m_object;
alter table m_object_text_info add constraint fk_object_text_info_owner foreign key (owner_oid) references m_object;
alter table m_operation_execution add constraint fk_op_exec_owner foreign key (owner_oid) references m_object;
alter table m_org_closure add constraint fk_ancestor foreign key (ancestor_oid) references m_object;
alter table m_org_closure add constraint fk_descendant foreign key (descendant_oid) references m_object;
alter table m_org_org_type add constraint fk_org_org_type foreign key (org_oid) references m_org;
alter table m_reference add constraint fk_reference_owner foreign key (owner_oid) references m_object;
alter table m_service_type add constraint fk_service_type foreign key (service_oid) references m_service;
alter table m_shadow add constraint fk_shadow foreign key (oid) references m_object;
alter table m_task add constraint fk_task foreign key (oid) references m_object;
alter table m_task_dependent add constraint fk_task_dependent foreign key (task_oid) references m_task;
alter table m_user_employee_type add constraint fk_user_employee_type foreign key (user_oid) references m_user;
alter table m_user_organization add constraint fk_user_organization foreign key (user_oid) references m_user;
alter table m_user_organizational_unit add constraint fk_user_org_unit foreign key (user_oid) references m_user;
alter table m_abstract_role add constraint fk_abstract_role foreign key (oid) references m_focus;
alter table m_case add constraint fk_case foreign key (oid) references m_object;
alter table m_connector add constraint fk_connector foreign key (oid) references m_object;
alter table m_connector_host add constraint fk_connector_host foreign key (oid) references m_object;
alter table m_exclusion add constraint fk_exclusion_owner foreign key (owner_oid) references m_object;
alter table m_focus add constraint fk_focus foreign key (oid) references m_object;
alter table m_form add constraint fk_form foreign key (oid) references m_object;
alter table m_function_library add constraint fk_function_library foreign key (oid) references m_object;
alter table m_generic_object add constraint fk_generic_object foreign key (oid) references m_object;
alter table m_lookup_table add constraint fk_lookup_table foreign key (oid) references m_object;
alter table m_lookup_table_row add constraint fk_lookup_table_owner foreign key (owner_oid) references m_lookup_table;
alter table m_node add constraint fk_node foreign key (oid) references m_object;
alter table m_object_template add constraint fk_object_template foreign key (oid) references m_object;
alter table m_org add constraint fk_org foreign key (oid) references m_abstract_role;
alter table m_report add constraint fk_report foreign key (oid) references m_object;
alter table m_report_output add constraint fk_report_output foreign key (oid) references m_object;
alter table m_resource add constraint fk_resource foreign key (oid) references m_object;
alter table m_role add constraint fk_role foreign key (oid) references m_abstract_role;
alter table m_security_policy add constraint fk_security_policy foreign key (oid) references m_object;
alter table m_sequence add constraint fk_sequence foreign key (oid) references m_object;
alter table m_service add constraint fk_service foreign key (oid) references m_abstract_role;
alter table m_system_configuration add constraint fk_system_configuration foreign key (oid) references m_object;
alter table m_trigger add constraint fk_trigger_owner foreign key (owner_oid) references m_object;
alter table m_user add constraint fk_user foreign key (oid) references m_focus;
alter table m_value_policy add constraint fk_value_policy foreign key (oid) references m_object;

--# thanks to George Papastamatopoulos for submitting this ... and Marko Lahma for
--# updating it.
--#
--# In your Quartz properties file, you'll need to set
--# org.quartz.jobStore.driverDelegateClass = org.quartz.impl.jdbcjobstore.MSSQLDelegate
--#
--# you shouse enter your DB instance's name on the next line in place of "enter_db_name_here"
--#
--#
--# From a helpful (but anonymous) Quartz user:
--#
--# Regarding this error message:
--#
--#     [Microsoft][SQLServer 2000 Driver for JDBC]Can't start a cloned connection while in manual transaction mode.
--#
--#
--#     I added "SelectMethod=cursor;" to my Connection URL in the config file.
--#     It Seems to work, hopefully no side effects.
--#
--#		example:
--#		"jdbc:microsoft:sqlserver://dbmachine:1433;SelectMethod=cursor";
--#
--# Another user has pointed out that you will probably need to use the
--# JTDS driver
--#
--#
--# USE [enter_db_name_here]
--# GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[FK_QRTZ_TRIGGERS_QRTZ_JOB_DETAILS]') AND OBJECTPROPERTY(id, N'ISFOREIGNKEY') = 1)
ALTER TABLE [dbo].[QRTZ_TRIGGERS] DROP CONSTRAINT FK_QRTZ_TRIGGERS_QRTZ_JOB_DETAILS;
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[FK_QRTZ_CRON_TRIGGERS_QRTZ_TRIGGERS]') AND OBJECTPROPERTY(id, N'ISFOREIGNKEY') = 1)
ALTER TABLE [dbo].[QRTZ_CRON_TRIGGERS] DROP CONSTRAINT FK_QRTZ_CRON_TRIGGERS_QRTZ_TRIGGERS;
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[FK_QRTZ_SIMPLE_TRIGGERS_QRTZ_TRIGGERS]') AND OBJECTPROPERTY(id, N'ISFOREIGNKEY') = 1)
ALTER TABLE [dbo].[QRTZ_SIMPLE_TRIGGERS] DROP CONSTRAINT FK_QRTZ_SIMPLE_TRIGGERS_QRTZ_TRIGGERS;
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[FK_QRTZ_SIMPROP_TRIGGERS_QRTZ_TRIGGERS]') AND OBJECTPROPERTY(id, N'ISFOREIGNKEY') = 1)
ALTER TABLE [dbo].[QRTZ_SIMPROP_TRIGGERS] DROP CONSTRAINT FK_QRTZ_SIMPROP_TRIGGERS_QRTZ_TRIGGERS;
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[QRTZ_CALENDARS]') AND OBJECTPROPERTY(id, N'ISUSERTABLE') = 1)
DROP TABLE [dbo].[QRTZ_CALENDARS];
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[QRTZ_CRON_TRIGGERS]') AND OBJECTPROPERTY(id, N'ISUSERTABLE') = 1)
DROP TABLE [dbo].[QRTZ_CRON_TRIGGERS];
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[QRTZ_BLOB_TRIGGERS]') AND OBJECTPROPERTY(id, N'ISUSERTABLE') = 1)
DROP TABLE [dbo].[QRTZ_BLOB_TRIGGERS];
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[QRTZ_FIRED_TRIGGERS]') AND OBJECTPROPERTY(id, N'ISUSERTABLE') = 1)
DROP TABLE [dbo].[QRTZ_FIRED_TRIGGERS];
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[QRTZ_PAUSED_TRIGGER_GRPS]') AND OBJECTPROPERTY(id, N'ISUSERTABLE') = 1)
DROP TABLE [dbo].[QRTZ_PAUSED_TRIGGER_GRPS];
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[QRTZ_SCHEDULER_STATE]') AND OBJECTPROPERTY(id, N'ISUSERTABLE') = 1)
DROP TABLE [dbo].[QRTZ_SCHEDULER_STATE];
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[QRTZ_LOCKS]') AND OBJECTPROPERTY(id, N'ISUSERTABLE') = 1)
DROP TABLE [dbo].[QRTZ_LOCKS];
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[QRTZ_JOB_DETAILS]') AND OBJECTPROPERTY(id, N'ISUSERTABLE') = 1)
DROP TABLE [dbo].[QRTZ_JOB_DETAILS];
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[QRTZ_SIMPLE_TRIGGERS]') AND OBJECTPROPERTY(id, N'ISUSERTABLE') = 1)
DROP TABLE [dbo].[QRTZ_SIMPLE_TRIGGERS];
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[QRTZ_SIMPROP_TRIGGERS]') AND OBJECTPROPERTY(id, N'ISUSERTABLE') = 1)
DROP TABLE [dbo].[QRTZ_SIMPROP_TRIGGERS];
-- GO

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[QRTZ_TRIGGERS]') AND OBJECTPROPERTY(id, N'ISUSERTABLE') = 1)
DROP TABLE [dbo].[QRTZ_TRIGGERS];
-- GO

CREATE TABLE [dbo].[QRTZ_CALENDARS] (
  [SCHED_NAME] [VARCHAR] (120)  NOT NULL ,
  [CALENDAR_NAME] [VARCHAR] (200)  NOT NULL ,
  [CALENDAR] [IMAGE] NOT NULL
) ON [PRIMARY];
-- GO

CREATE TABLE [dbo].[QRTZ_CRON_TRIGGERS] (
  [SCHED_NAME] [VARCHAR] (120)  NOT NULL ,
  [TRIGGER_NAME] [VARCHAR] (200)  NOT NULL ,
  [TRIGGER_GROUP] [VARCHAR] (200)  NOT NULL ,
  [CRON_EXPRESSION] [VARCHAR] (120)  NOT NULL ,
  [TIME_ZONE_ID] [VARCHAR] (80)
) ON [PRIMARY];
-- GO

CREATE TABLE [dbo].[QRTZ_FIRED_TRIGGERS] (
  [SCHED_NAME] [VARCHAR] (120)  NOT NULL ,
  [ENTRY_ID] [VARCHAR] (95)  NOT NULL ,
  [TRIGGER_NAME] [VARCHAR] (200)  NOT NULL ,
  [TRIGGER_GROUP] [VARCHAR] (200)  NOT NULL ,
  [INSTANCE_NAME] [VARCHAR] (200)  NOT NULL ,
  [FIRED_TIME] [BIGINT] NOT NULL ,
  [SCHED_TIME] [BIGINT] NOT NULL ,
  [PRIORITY] [INTEGER] NOT NULL ,
  [EXECUTION_GROUP] [VARCHAR] (200) NULL ,
  [STATE] [VARCHAR] (16)  NOT NULL,
  [JOB_NAME] [VARCHAR] (200)  NULL ,
  [JOB_GROUP] [VARCHAR] (200)  NULL ,
  [IS_NONCONCURRENT] [VARCHAR] (1)  NULL ,
  [REQUESTS_RECOVERY] [VARCHAR] (1)  NULL
) ON [PRIMARY];
-- GO

CREATE TABLE [dbo].[QRTZ_PAUSED_TRIGGER_GRPS] (
  [SCHED_NAME] [VARCHAR] (120)  NOT NULL ,
  [TRIGGER_GROUP] [VARCHAR] (200)  NOT NULL
) ON [PRIMARY];
-- GO

CREATE TABLE [dbo].[QRTZ_SCHEDULER_STATE] (
  [SCHED_NAME] [VARCHAR] (120)  NOT NULL ,
  [INSTANCE_NAME] [VARCHAR] (200)  NOT NULL ,
  [LAST_CHECKIN_TIME] [BIGINT] NOT NULL ,
  [CHECKIN_INTERVAL] [BIGINT] NOT NULL
) ON [PRIMARY];
-- GO

CREATE TABLE [dbo].[QRTZ_LOCKS] (
  [SCHED_NAME] [VARCHAR] (120)  NOT NULL ,
  [LOCK_NAME] [VARCHAR] (40)  NOT NULL
) ON [PRIMARY];
-- GO

CREATE TABLE [dbo].[QRTZ_JOB_DETAILS] (
  [SCHED_NAME] [VARCHAR] (120)  NOT NULL ,
  [JOB_NAME] [VARCHAR] (200)  NOT NULL ,
  [JOB_GROUP] [VARCHAR] (200)  NOT NULL ,
  [DESCRIPTION] [VARCHAR] (250) NULL ,
  [JOB_CLASS_NAME] [VARCHAR] (250)  NOT NULL ,
  [IS_DURABLE] [VARCHAR] (1)  NOT NULL ,
  [IS_NONCONCURRENT] [VARCHAR] (1)  NOT NULL ,
  [IS_UPDATE_DATA] [VARCHAR] (1)  NOT NULL ,
  [REQUESTS_RECOVERY] [VARCHAR] (1)  NOT NULL ,
  [JOB_DATA] [IMAGE] NULL
) ON [PRIMARY];
-- GO

CREATE TABLE [dbo].[QRTZ_SIMPLE_TRIGGERS] (
  [SCHED_NAME] [VARCHAR] (120)  NOT NULL ,
  [TRIGGER_NAME] [VARCHAR] (200)  NOT NULL ,
  [TRIGGER_GROUP] [VARCHAR] (200)  NOT NULL ,
  [REPEAT_COUNT] [BIGINT] NOT NULL ,
  [REPEAT_INTERVAL] [BIGINT] NOT NULL ,
  [TIMES_TRIGGERED] [BIGINT] NOT NULL
) ON [PRIMARY];
-- GO

CREATE TABLE [dbo].[QRTZ_SIMPROP_TRIGGERS] (
  [SCHED_NAME] [VARCHAR] (120)  NOT NULL ,
  [TRIGGER_NAME] [VARCHAR] (200)  NOT NULL ,
  [TRIGGER_GROUP] [VARCHAR] (200)  NOT NULL ,
  [STR_PROP_1] [VARCHAR] (512) NULL,
  [STR_PROP_2] [VARCHAR] (512) NULL,
  [STR_PROP_3] [VARCHAR] (512) NULL,
  [INT_PROP_1] [INT] NULL,
  [INT_PROP_2] [INT] NULL,
  [LONG_PROP_1] [BIGINT] NULL,
  [LONG_PROP_2] [BIGINT] NULL,
  [DEC_PROP_1] [NUMERIC] (13,4) NULL,
  [DEC_PROP_2] [NUMERIC] (13,4) NULL,
  [BOOL_PROP_1] [VARCHAR] (1) NULL,
  [BOOL_PROP_2] [VARCHAR] (1) NULL,
) ON [PRIMARY];
-- GO

CREATE TABLE [dbo].[QRTZ_BLOB_TRIGGERS] (
  [SCHED_NAME] [VARCHAR] (120)  NOT NULL ,
  [TRIGGER_NAME] [VARCHAR] (200)  NOT NULL ,
  [TRIGGER_GROUP] [VARCHAR] (200)  NOT NULL ,
  [BLOB_DATA] [IMAGE] NULL
) ON [PRIMARY];
-- GO

CREATE TABLE [dbo].[QRTZ_TRIGGERS] (
  [SCHED_NAME] [VARCHAR] (120)  NOT NULL ,
  [TRIGGER_NAME] [VARCHAR] (200)  NOT NULL ,
  [TRIGGER_GROUP] [VARCHAR] (200)  NOT NULL ,
  [JOB_NAME] [VARCHAR] (200)  NOT NULL ,
  [JOB_GROUP] [VARCHAR] (200)  NOT NULL ,
  [DESCRIPTION] [VARCHAR] (250) NULL ,
  [NEXT_FIRE_TIME] [BIGINT] NULL ,
  [PREV_FIRE_TIME] [BIGINT] NULL ,
  [PRIORITY] [INTEGER] NULL ,
  [EXECUTION_GROUP] [VARCHAR] (200) NULL ,
  [TRIGGER_STATE] [VARCHAR] (16)  NOT NULL ,
  [TRIGGER_TYPE] [VARCHAR] (8)  NOT NULL ,
  [START_TIME] [BIGINT] NOT NULL ,
  [END_TIME] [BIGINT] NULL ,
  [CALENDAR_NAME] [VARCHAR] (200)  NULL ,
  [MISFIRE_INSTR] [SMALLINT] NULL ,
  [JOB_DATA] [IMAGE] NULL
) ON [PRIMARY];
-- GO

ALTER TABLE [dbo].[QRTZ_CALENDARS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_CALENDARS] PRIMARY KEY  CLUSTERED
  (
    [SCHED_NAME],
    [CALENDAR_NAME]
  )  ON [PRIMARY];
-- GO

ALTER TABLE [dbo].[QRTZ_CRON_TRIGGERS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_CRON_TRIGGERS] PRIMARY KEY  CLUSTERED
  (
    [SCHED_NAME],
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  )  ON [PRIMARY];
-- GO

ALTER TABLE [dbo].[QRTZ_FIRED_TRIGGERS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_FIRED_TRIGGERS] PRIMARY KEY  CLUSTERED
  (
    [SCHED_NAME],
    [ENTRY_ID]
  )  ON [PRIMARY];
-- GO

ALTER TABLE [dbo].[QRTZ_PAUSED_TRIGGER_GRPS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_PAUSED_TRIGGER_GRPS] PRIMARY KEY  CLUSTERED
  (
    [SCHED_NAME],
    [TRIGGER_GROUP]
  )  ON [PRIMARY];
-- GO

ALTER TABLE [dbo].[QRTZ_SCHEDULER_STATE] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_SCHEDULER_STATE] PRIMARY KEY  CLUSTERED
  (
    [SCHED_NAME],
    [INSTANCE_NAME]
  )  ON [PRIMARY];
-- GO

ALTER TABLE [dbo].[QRTZ_LOCKS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_LOCKS] PRIMARY KEY  CLUSTERED
  (
    [SCHED_NAME],
    [LOCK_NAME]
  )  ON [PRIMARY];
-- GO

ALTER TABLE [dbo].[QRTZ_JOB_DETAILS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_JOB_DETAILS] PRIMARY KEY  CLUSTERED
  (
    [SCHED_NAME],
    [JOB_NAME],
    [JOB_GROUP]
  )  ON [PRIMARY];
-- GO

ALTER TABLE [dbo].[QRTZ_SIMPLE_TRIGGERS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_SIMPLE_TRIGGERS] PRIMARY KEY  CLUSTERED
  (
    [SCHED_NAME],
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  )  ON [PRIMARY];
-- GO

ALTER TABLE [dbo].[QRTZ_SIMPROP_TRIGGERS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_SIMPROP_TRIGGERS] PRIMARY KEY  CLUSTERED
  (
    [SCHED_NAME],
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  )  ON [PRIMARY];
-- GO

ALTER TABLE [dbo].[QRTZ_TRIGGERS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_TRIGGERS] PRIMARY KEY  CLUSTERED
  (
    [SCHED_NAME],
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  )  ON [PRIMARY];
-- GO

ALTER TABLE [dbo].[QRTZ_CRON_TRIGGERS] ADD
  CONSTRAINT [FK_QRTZ_CRON_TRIGGERS_QRTZ_TRIGGERS] FOREIGN KEY
  (
    [SCHED_NAME],
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) REFERENCES [dbo].[QRTZ_TRIGGERS] (
    [SCHED_NAME],
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) ON DELETE CASCADE;
-- GO

ALTER TABLE [dbo].[QRTZ_SIMPLE_TRIGGERS] ADD
  CONSTRAINT [FK_QRTZ_SIMPLE_TRIGGERS_QRTZ_TRIGGERS] FOREIGN KEY
  (
    [SCHED_NAME],
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) REFERENCES [dbo].[QRTZ_TRIGGERS] (
    [SCHED_NAME],
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) ON DELETE CASCADE;
-- GO

ALTER TABLE [dbo].[QRTZ_SIMPROP_TRIGGERS] ADD
  CONSTRAINT [FK_QRTZ_SIMPROP_TRIGGERS_QRTZ_TRIGGERS] FOREIGN KEY
  (
    [SCHED_NAME],
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) REFERENCES [dbo].[QRTZ_TRIGGERS] (
    [SCHED_NAME],
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) ON DELETE CASCADE;
-- GO

ALTER TABLE [dbo].[QRTZ_TRIGGERS] ADD
  CONSTRAINT [FK_QRTZ_TRIGGERS_QRTZ_JOB_DETAILS] FOREIGN KEY
  (
    [SCHED_NAME],
    [JOB_NAME],
    [JOB_GROUP]
  ) REFERENCES [dbo].[QRTZ_JOB_DETAILS] (
    [SCHED_NAME],
    [JOB_NAME],
    [JOB_GROUP]
  );
-- GO;

create table ACT_GE_PROPERTY (
    NAME_ nvarchar(64),
    VALUE_ nvarchar(300),
    REV_ int,
    primary key (NAME_)
);

insert into ACT_GE_PROPERTY
values ('schema.version', '5.22.0.0', 1);

insert into ACT_GE_PROPERTY
values ('schema.history', 'create(5.22.0.0)', 1);

insert into ACT_GE_PROPERTY
values ('next.dbid', '1', 1);

create table ACT_GE_BYTEARRAY (
    ID_ nvarchar(64),
    REV_ int,
    NAME_ nvarchar(255),
    DEPLOYMENT_ID_ nvarchar(64),
    BYTES_  varbinary(max),
    GENERATED_ tinyint,
    primary key (ID_)
);

create table ACT_RE_DEPLOYMENT (
    ID_ nvarchar(64),
    NAME_ nvarchar(255),
    CATEGORY_ nvarchar(255),
    TENANT_ID_ nvarchar(255) default '',
    DEPLOY_TIME_ datetime,
    primary key (ID_)
);

create table ACT_RE_MODEL (
    ID_ nvarchar(64) not null,
    REV_ int,
    NAME_ nvarchar(255),
    KEY_ nvarchar(255),
    CATEGORY_ nvarchar(255),
    CREATE_TIME_ datetime,
    LAST_UPDATE_TIME_ datetime,
    VERSION_ int,
    META_INFO_ nvarchar(4000),
    DEPLOYMENT_ID_ nvarchar(64),
    EDITOR_SOURCE_VALUE_ID_ nvarchar(64),
    EDITOR_SOURCE_EXTRA_VALUE_ID_ nvarchar(64),
    TENANT_ID_ nvarchar(255) default '',
    primary key (ID_)
);

create table ACT_RU_EXECUTION (
    ID_ nvarchar(64),
    REV_ int,
    PROC_INST_ID_ nvarchar(64),
    BUSINESS_KEY_ nvarchar(255),
    PARENT_ID_ nvarchar(64),
    PROC_DEF_ID_ nvarchar(64),
    SUPER_EXEC_ nvarchar(64),
    ACT_ID_ nvarchar(255),
    IS_ACTIVE_ tinyint,
    IS_CONCURRENT_ tinyint,
    IS_SCOPE_ tinyint,
    IS_EVENT_SCOPE_ tinyint,
    SUSPENSION_STATE_ tinyint,
    CACHED_ENT_STATE_ int,
    TENANT_ID_ nvarchar(255) default '',
    NAME_ nvarchar(255),
    LOCK_TIME_ datetime,
    primary key (ID_)
);

create table ACT_RU_JOB (
    ID_ nvarchar(64) NOT NULL,
  	REV_ int,
    TYPE_ nvarchar(255) NOT NULL,
    LOCK_EXP_TIME_ datetime,
    LOCK_OWNER_ nvarchar(255),
    EXCLUSIVE_ bit,
    EXECUTION_ID_ nvarchar(64),
    PROCESS_INSTANCE_ID_ nvarchar(64),
    PROC_DEF_ID_ nvarchar(64),
    RETRIES_ int,
    EXCEPTION_STACK_ID_ nvarchar(64),
    EXCEPTION_MSG_ nvarchar(4000),
    DUEDATE_ datetime NULL,
    REPEAT_ nvarchar(255),
    HANDLER_TYPE_ nvarchar(255),
    HANDLER_CFG_ nvarchar(4000),
    TENANT_ID_ nvarchar(255) default '',
    primary key (ID_)
);

create table ACT_RE_PROCDEF (
    ID_ nvarchar(64) not null,
    REV_ int,
    CATEGORY_ nvarchar(255),
    NAME_ nvarchar(255),
    KEY_ nvarchar(255) not null,
    VERSION_ int not null,
    DEPLOYMENT_ID_ nvarchar(64),
    RESOURCE_NAME_ nvarchar(4000),
    DGRM_RESOURCE_NAME_ nvarchar(4000),
    DESCRIPTION_ nvarchar(4000),
    HAS_START_FORM_KEY_ tinyint,
    HAS_GRAPHICAL_NOTATION_ tinyint,
    SUSPENSION_STATE_ tinyint,
    TENANT_ID_ nvarchar(255) default '',
    primary key (ID_)
);

create table ACT_RU_TASK (
    ID_ nvarchar(64),
    REV_ int,
    EXECUTION_ID_ nvarchar(64),
    PROC_INST_ID_ nvarchar(64),
    PROC_DEF_ID_ nvarchar(64),
    NAME_ nvarchar(255),
    PARENT_TASK_ID_ nvarchar(64),
    DESCRIPTION_ nvarchar(4000),
    TASK_DEF_KEY_ nvarchar(255),
    OWNER_ nvarchar(255),
    ASSIGNEE_ nvarchar(255),
    DELEGATION_ nvarchar(64),
    PRIORITY_ int,
    CREATE_TIME_ datetime,
    DUE_DATE_ datetime,
    CATEGORY_ nvarchar(255),
    SUSPENSION_STATE_ int,
    TENANT_ID_ nvarchar(255) default '',
    FORM_KEY_ nvarchar(255),
    primary key (ID_)
);

create table ACT_RU_IDENTITYLINK (
    ID_ nvarchar(64),
    REV_ int,
    GROUP_ID_ nvarchar(255),
    TYPE_ nvarchar(255),
    USER_ID_ nvarchar(255),
    TASK_ID_ nvarchar(64),
    PROC_INST_ID_ nvarchar(64),
    PROC_DEF_ID_ nvarchar(64),
    primary key (ID_)
);

create table ACT_RU_VARIABLE (
    ID_ nvarchar(64) not null,
    REV_ int,
    TYPE_ nvarchar(255) not null,
    NAME_ nvarchar(255) not null,
    EXECUTION_ID_ nvarchar(64),
    PROC_INST_ID_ nvarchar(64),
    TASK_ID_ nvarchar(64),
    BYTEARRAY_ID_ nvarchar(64),
    DOUBLE_ double precision,
    LONG_ numeric(19,0),
    TEXT_ nvarchar(4000),
    TEXT2_ nvarchar(4000),
    primary key (ID_)
);

create table ACT_RU_EVENT_SUBSCR (
    ID_ nvarchar(64) not null,
    REV_ int,
    EVENT_TYPE_ nvarchar(255) not null,
    EVENT_NAME_ nvarchar(255),
    EXECUTION_ID_ nvarchar(64),
    PROC_INST_ID_ nvarchar(64),
    ACTIVITY_ID_ nvarchar(64),
    CONFIGURATION_ nvarchar(255),
    CREATED_ datetime not null,
    PROC_DEF_ID_ nvarchar(64),
    TENANT_ID_ nvarchar(255) default '',
    primary key (ID_)
);

create table ACT_EVT_LOG (
    LOG_NR_ numeric(19,0) IDENTITY(1,1),
    TYPE_ nvarchar(64),
    PROC_DEF_ID_ nvarchar(64),
    PROC_INST_ID_ nvarchar(64),
    EXECUTION_ID_ nvarchar(64),
    TASK_ID_ nvarchar(64),
    TIME_STAMP_ datetime not null,
    USER_ID_ nvarchar(255),
    DATA_ varbinary(max),
    LOCK_OWNER_ nvarchar(255),
    LOCK_TIME_ datetime null,
    IS_PROCESSED_ tinyint default 0,
    primary key (LOG_NR_)
);

create table ACT_PROCDEF_INFO (
	ID_ nvarchar(64) not null,
    PROC_DEF_ID_ nvarchar(64) not null,
    REV_ int,
    INFO_JSON_ID_ nvarchar(64),
    primary key (ID_)
);

create index ACT_IDX_EXEC_BUSKEY on ACT_RU_EXECUTION(BUSINESS_KEY_);
create index ACT_IDX_TASK_CREATE on ACT_RU_TASK(CREATE_TIME_);
create index ACT_IDX_IDENT_LNK_USER on ACT_RU_IDENTITYLINK(USER_ID_);
create index ACT_IDX_IDENT_LNK_GROUP on ACT_RU_IDENTITYLINK(GROUP_ID_);
create index ACT_IDX_EVENT_SUBSCR_CONFIG_ on ACT_RU_EVENT_SUBSCR(CONFIGURATION_);
create index ACT_IDX_VARIABLE_TASK_ID on ACT_RU_VARIABLE(TASK_ID_);
create index ACT_IDX_ATHRZ_PROCEDEF on ACT_RU_IDENTITYLINK(PROC_DEF_ID_);
create index ACT_IDX_EXECUTION_PROC on ACT_RU_EXECUTION(PROC_DEF_ID_);
create index ACT_IDX_EXECUTION_PARENT on ACT_RU_EXECUTION(PARENT_ID_);
create index ACT_IDX_EXECUTION_SUPER on ACT_RU_EXECUTION(SUPER_EXEC_);
create index ACT_IDX_EXECUTION_IDANDREV on ACT_RU_EXECUTION(ID_, REV_);
create index ACT_IDX_VARIABLE_BA on ACT_RU_VARIABLE(BYTEARRAY_ID_);
create index ACT_IDX_VARIABLE_EXEC on ACT_RU_VARIABLE(EXECUTION_ID_);
create index ACT_IDX_VARIABLE_PROCINST on ACT_RU_VARIABLE(PROC_INST_ID_);
create index ACT_IDX_IDENT_LNK_TASK on ACT_RU_IDENTITYLINK(TASK_ID_);
create index ACT_IDX_IDENT_LNK_PROCINST on ACT_RU_IDENTITYLINK(PROC_INST_ID_);
create index ACT_IDX_TASK_EXEC on ACT_RU_TASK(EXECUTION_ID_);
create index ACT_IDX_TASK_PROCINST on ACT_RU_TASK(PROC_INST_ID_);
create index ACT_IDX_EXEC_PROC_INST_ID on ACT_RU_EXECUTION(PROC_INST_ID_);
create index ACT_IDX_TASK_PROC_DEF_ID on ACT_RU_TASK(PROC_DEF_ID_);
create index ACT_IDX_EVENT_SUBSCR_EXEC_ID on ACT_RU_EVENT_SUBSCR(EXECUTION_ID_);
create index ACT_IDX_JOB_EXCEPTION_STACK_ID on ACT_RU_JOB(EXCEPTION_STACK_ID_);
create index ACT_IDX_INFO_PROCDEF on ACT_PROCDEF_INFO(PROC_DEF_ID_);

alter table ACT_GE_BYTEARRAY
    add constraint ACT_FK_BYTEARR_DEPL
    foreign key (DEPLOYMENT_ID_)
    references ACT_RE_DEPLOYMENT (ID_);

alter table ACT_RE_PROCDEF
    add constraint ACT_UNIQ_PROCDEF
    unique (KEY_,VERSION_, TENANT_ID_);

alter table ACT_RU_EXECUTION
    add constraint ACT_FK_EXE_PARENT
    foreign key (PARENT_ID_)
    references ACT_RU_EXECUTION (ID_);

alter table ACT_RU_EXECUTION
    add constraint ACT_FK_EXE_SUPER
    foreign key (SUPER_EXEC_)
    references ACT_RU_EXECUTION (ID_);

alter table ACT_RU_EXECUTION
    add constraint ACT_FK_EXE_PROCDEF
    foreign key (PROC_DEF_ID_)
    references ACT_RE_PROCDEF (ID_);

alter table ACT_RU_IDENTITYLINK
    add constraint ACT_FK_TSKASS_TASK
    foreign key (TASK_ID_)
    references ACT_RU_TASK (ID_);

alter table ACT_RU_IDENTITYLINK
    add constraint ACT_FK_ATHRZ_PROCEDEF
    foreign key (PROC_DEF_ID_)
    references ACT_RE_PROCDEF (ID_);

alter table ACT_RU_IDENTITYLINK
    add constraint ACT_FK_IDL_PROCINST
    foreign key (PROC_INST_ID_)
    references ACT_RU_EXECUTION (ID_);

alter table ACT_RU_TASK
    add constraint ACT_FK_TASK_EXE
    foreign key (EXECUTION_ID_)
    references ACT_RU_EXECUTION (ID_);

alter table ACT_RU_TASK
    add constraint ACT_FK_TASK_PROCINST
    foreign key (PROC_INST_ID_)
    references ACT_RU_EXECUTION (ID_);

alter table ACT_RU_TASK
  	add constraint ACT_FK_TASK_PROCDEF
  	foreign key (PROC_DEF_ID_)
  	references ACT_RE_PROCDEF (ID_);

alter table ACT_RU_VARIABLE
    add constraint ACT_FK_VAR_EXE
    foreign key (EXECUTION_ID_)
    references ACT_RU_EXECUTION (ID_);

alter table ACT_RU_VARIABLE
    add constraint ACT_FK_VAR_PROCINST
    foreign key (PROC_INST_ID_)
    references ACT_RU_EXECUTION(ID_);

alter table ACT_RU_VARIABLE
    add constraint ACT_FK_VAR_BYTEARRAY
    foreign key (BYTEARRAY_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_JOB
    add constraint ACT_FK_JOB_EXCEPTION
    foreign key (EXCEPTION_STACK_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_EVENT_SUBSCR
    add constraint ACT_FK_EVENT_EXEC
    foreign key (EXECUTION_ID_)
    references ACT_RU_EXECUTION(ID_);

alter table ACT_RE_MODEL
    add constraint ACT_FK_MODEL_SOURCE
    foreign key (EDITOR_SOURCE_VALUE_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RE_MODEL
    add constraint ACT_FK_MODEL_SOURCE_EXTRA
    foreign key (EDITOR_SOURCE_EXTRA_VALUE_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RE_MODEL
    add constraint ACT_FK_MODEL_DEPLOYMENT
    foreign key (DEPLOYMENT_ID_)
    references ACT_RE_DEPLOYMENT (ID_);

alter table ACT_PROCDEF_INFO
    add constraint ACT_FK_INFO_JSON_BA
    foreign key (INFO_JSON_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_PROCDEF_INFO
    add constraint ACT_FK_INFO_PROCDEF
    foreign key (PROC_DEF_ID_)
    references ACT_RE_PROCDEF (ID_);

alter table ACT_PROCDEF_INFO
    add constraint ACT_UNIQ_INFO_PROCDEF
    unique (PROC_DEF_ID_);

create table ACT_HI_PROCINST (
    ID_ nvarchar(64) not null,
    PROC_INST_ID_ nvarchar(64) not null,
    BUSINESS_KEY_ nvarchar(255),
    PROC_DEF_ID_ nvarchar(64) not null,
    START_TIME_ datetime not null,
    END_TIME_ datetime,
    DURATION_ numeric(19,0),
    START_USER_ID_ nvarchar(255),
    START_ACT_ID_ nvarchar(255),
    END_ACT_ID_ nvarchar(255),
    SUPER_PROCESS_INSTANCE_ID_ nvarchar(64),
    DELETE_REASON_ nvarchar(4000),
    TENANT_ID_ nvarchar(255) default '',
    NAME_ nvarchar(255),
    primary key (ID_),
    unique (PROC_INST_ID_)
);

create table ACT_HI_ACTINST (
    ID_ nvarchar(64) not null,
    PROC_DEF_ID_ nvarchar(64) not null,
    PROC_INST_ID_ nvarchar(64) not null,
    EXECUTION_ID_ nvarchar(64) not null,
    ACT_ID_ nvarchar(255) not null,
    TASK_ID_ nvarchar(64),
    CALL_PROC_INST_ID_ nvarchar(64),
    ACT_NAME_ nvarchar(255),
    ACT_TYPE_ nvarchar(255) not null,
    ASSIGNEE_ nvarchar(255),
    START_TIME_ datetime not null,
    END_TIME_ datetime,
    DURATION_ numeric(19,0),
    TENANT_ID_ nvarchar(255) default '',
    primary key (ID_)
);

create table ACT_HI_TASKINST (
    ID_ nvarchar(64) not null,
    PROC_DEF_ID_ nvarchar(64),
    TASK_DEF_KEY_ nvarchar(255),
    PROC_INST_ID_ nvarchar(64),
    EXECUTION_ID_ nvarchar(64),
    NAME_ nvarchar(255),
    PARENT_TASK_ID_ nvarchar(64),
    DESCRIPTION_ nvarchar(4000),
    OWNER_ nvarchar(255),
    ASSIGNEE_ nvarchar(255),
    START_TIME_ datetime not null,
    CLAIM_TIME_ datetime,
    END_TIME_ datetime,
    DURATION_ numeric(19,0),
    DELETE_REASON_ nvarchar(4000),
    PRIORITY_ int,
    DUE_DATE_ datetime,
    FORM_KEY_ nvarchar(255),
    CATEGORY_ nvarchar(255),
    TENANT_ID_ nvarchar(255) default '',
    primary key (ID_)
);

create table ACT_HI_VARINST (
    ID_ nvarchar(64) not null,
    PROC_INST_ID_ nvarchar(64),
    EXECUTION_ID_ nvarchar(64),
    TASK_ID_ nvarchar(64),
    NAME_ nvarchar(255) not null,
    VAR_TYPE_ nvarchar(100),
    REV_ int,
    BYTEARRAY_ID_ nvarchar(64),
    DOUBLE_ double precision,
    LONG_ numeric(19,0),
    TEXT_ nvarchar(4000),
    TEXT2_ nvarchar(4000),
    CREATE_TIME_ datetime,
    LAST_UPDATED_TIME_ datetime,
    primary key (ID_)
);

create table ACT_HI_DETAIL (
    ID_ nvarchar(64) not null,
    TYPE_ nvarchar(255) not null,
    PROC_INST_ID_ nvarchar(64),
    EXECUTION_ID_ nvarchar(64),
    TASK_ID_ nvarchar(64),
    ACT_INST_ID_ nvarchar(64),
    NAME_ nvarchar(255) not null,
    VAR_TYPE_ nvarchar(255),
    REV_ int,
    TIME_ datetime not null,
    BYTEARRAY_ID_ nvarchar(64),
    DOUBLE_ double precision,
    LONG_ numeric(19,0),
    TEXT_ nvarchar(4000),
    TEXT2_ nvarchar(4000),
    primary key (ID_)
);

create table ACT_HI_COMMENT (
    ID_ nvarchar(64) not null,
    TYPE_ nvarchar(255),
    TIME_ datetime not null,
    USER_ID_ nvarchar(255),
    TASK_ID_ nvarchar(64),
    PROC_INST_ID_ nvarchar(64),
    ACTION_ nvarchar(255),
    MESSAGE_ nvarchar(4000),
    FULL_MSG_ varbinary(max),
    primary key (ID_)
);

create table ACT_HI_ATTACHMENT (
    ID_ nvarchar(64) not null,
    REV_ integer,
    USER_ID_ nvarchar(255),
    NAME_ nvarchar(255),
    DESCRIPTION_ nvarchar(4000),
    TYPE_ nvarchar(255),
    TASK_ID_ nvarchar(64),
    PROC_INST_ID_ nvarchar(64),
    URL_ nvarchar(4000),
    CONTENT_ID_ nvarchar(64),
    TIME_ datetime,
    primary key (ID_)
);

create table ACT_HI_IDENTITYLINK (
    ID_ nvarchar(64),
    GROUP_ID_ nvarchar(255),
    TYPE_ nvarchar(255),
    USER_ID_ nvarchar(255),
    TASK_ID_ nvarchar(64),
    PROC_INST_ID_ nvarchar(64),
    primary key (ID_)
);


create index ACT_IDX_HI_PRO_INST_END on ACT_HI_PROCINST(END_TIME_);
create index ACT_IDX_HI_PRO_I_BUSKEY on ACT_HI_PROCINST(BUSINESS_KEY_);
create index ACT_IDX_HI_ACT_INST_START on ACT_HI_ACTINST(START_TIME_);
create index ACT_IDX_HI_ACT_INST_END on ACT_HI_ACTINST(END_TIME_);
create index ACT_IDX_HI_DETAIL_PROC_INST on ACT_HI_DETAIL(PROC_INST_ID_);
create index ACT_IDX_HI_DETAIL_ACT_INST on ACT_HI_DETAIL(ACT_INST_ID_);
create index ACT_IDX_HI_DETAIL_TIME on ACT_HI_DETAIL(TIME_);
create index ACT_IDX_HI_DETAIL_NAME on ACT_HI_DETAIL(NAME_);
create index ACT_IDX_HI_DETAIL_TASK_ID on ACT_HI_DETAIL(TASK_ID_);
create index ACT_IDX_HI_PROCVAR_PROC_INST on ACT_HI_VARINST(PROC_INST_ID_);
create index ACT_IDX_HI_PROCVAR_NAME_TYPE on ACT_HI_VARINST(NAME_, VAR_TYPE_);
create index ACT_IDX_HI_PROCVAR_TASK_ID on ACT_HI_VARINST(TASK_ID_);
create index ACT_IDX_HI_ACT_INST_PROCINST on ACT_HI_ACTINST(PROC_INST_ID_, ACT_ID_);
create index ACT_IDX_HI_ACT_INST_EXEC on ACT_HI_ACTINST(EXECUTION_ID_, ACT_ID_);
create index ACT_IDX_HI_IDENT_LNK_USER on ACT_HI_IDENTITYLINK(USER_ID_);
create index ACT_IDX_HI_IDENT_LNK_TASK on ACT_HI_IDENTITYLINK(TASK_ID_);
create index ACT_IDX_HI_IDENT_LNK_PROCINST on ACT_HI_IDENTITYLINK(PROC_INST_ID_);
create index ACT_IDX_HI_TASK_INST_PROCINST on ACT_HI_TASKINST(PROC_INST_ID_);

create table ACT_ID_GROUP (
    ID_ nvarchar(64),
    REV_ int,
    NAME_ nvarchar(255),
    TYPE_ nvarchar(255),
    primary key (ID_)
);

create table ACT_ID_MEMBERSHIP (
    USER_ID_ nvarchar(64),
    GROUP_ID_ nvarchar(64),
    primary key (USER_ID_, GROUP_ID_)
);

create table ACT_ID_USER (
    ID_ nvarchar(64),
    REV_ int,
    FIRST_ nvarchar(255),
    LAST_ nvarchar(255),
    EMAIL_ nvarchar(255),
    PWD_ nvarchar(255),
    PICTURE_ID_ nvarchar(64),
    primary key (ID_)
);

create table ACT_ID_INFO (
    ID_ nvarchar(64),
    REV_ int,
    USER_ID_ nvarchar(64),
    TYPE_ nvarchar(64),
    KEY_ nvarchar(255),
    VALUE_ nvarchar(255),
    PASSWORD_ varbinary(max),
    PARENT_ID_ nvarchar(255),
    primary key (ID_)
);

alter table ACT_ID_MEMBERSHIP
    add constraint ACT_FK_MEMB_GROUP
    foreign key (GROUP_ID_)
    references ACT_ID_GROUP (ID_);

alter table ACT_ID_MEMBERSHIP
    add constraint ACT_FK_MEMB_USER
    foreign key (USER_ID_)
    references ACT_ID_USER (ID_);
