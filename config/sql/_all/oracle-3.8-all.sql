create table m_acc_cert_campaign (definitionRef_relation varchar2(157 char), definitionRef_targetOid varchar2(36 char), definitionRef_type number(10,0), endTimestamp timestamp, handlerUri varchar2(255 char), name_norm varchar2(255 char), name_orig varchar2(255 char), ownerRef_relation varchar2(157 char), ownerRef_targetOid varchar2(36 char), ownerRef_type number(10,0), stageNumber number(10,0), startTimestamp timestamp, state number(10,0), oid varchar2(36 char) not null, primary key (oid)) initrans 30;
create table m_acc_cert_case (id number(10,0) not null, owner_oid varchar2(36 char) not null, administrativeStatus number(10,0), archiveTimestamp timestamp, disableReason varchar2(255 char), disableTimestamp timestamp, effectiveStatus number(10,0), enableTimestamp timestamp, validFrom timestamp, validTo timestamp, validityChangeTimestamp timestamp, validityStatus number(10,0), currentStageOutcome varchar2(255 char), fullObject blob, objectRef_relation varchar2(157 char), objectRef_targetOid varchar2(36 char), objectRef_type number(10,0), orgRef_relation varchar2(157 char), orgRef_targetOid varchar2(36 char), orgRef_type number(10,0), outcome varchar2(255 char), remediedTimestamp timestamp, reviewDeadline timestamp, reviewRequestedTimestamp timestamp, stageNumber number(10,0), targetRef_relation varchar2(157 char), targetRef_targetOid varchar2(36 char), targetRef_type number(10,0), tenantRef_relation varchar2(157 char), tenantRef_targetOid varchar2(36 char), tenantRef_type number(10,0), primary key (id, owner_oid)) initrans 30;
create table m_acc_cert_definition (handlerUri varchar2(255 char), lastCampaignClosedTimestamp timestamp, lastCampaignStartedTimestamp timestamp, name_norm varchar2(255 char), name_orig varchar2(255 char), ownerRef_relation varchar2(157 char), ownerRef_targetOid varchar2(36 char), ownerRef_type number(10,0), oid varchar2(36 char) not null, primary key (oid)) initrans 30;
create table m_acc_cert_wi (id number(10,0) not null, owner_id number(10,0) not null, owner_owner_oid varchar2(36 char) not null, closeTimestamp timestamp, outcome varchar2(255 char), outputChangeTimestamp timestamp, performerRef_relation varchar2(157 char), performerRef_targetOid varchar2(36 char), performerRef_type number(10,0), stageNumber number(10,0), primary key (id, owner_id, owner_owner_oid)) initrans 30;
create table m_acc_cert_wi_reference (owner_id number(10,0) not null, owner_owner_id number(10,0) not null, owner_owner_owner_oid varchar2(36 char) not null, relation varchar2(157 char) not null, targetOid varchar2(36 char) not null, targetType number(10,0), primary key (owner_id, owner_owner_id, owner_owner_owner_oid, relation, targetOid)) initrans 30;
create table m_assignment (id number(10,0) not null, owner_oid varchar2(36 char) not null, administrativeStatus number(10,0), archiveTimestamp timestamp, disableReason varchar2(255 char), disableTimestamp timestamp, effectiveStatus number(10,0), enableTimestamp timestamp, validFrom timestamp, validTo timestamp, validityChangeTimestamp timestamp, validityStatus number(10,0), assignmentOwner number(10,0), createChannel varchar2(255 char), createTimestamp timestamp, creatorRef_relation varchar2(157 char), creatorRef_targetOid varchar2(36 char), creatorRef_type number(10,0), lifecycleState varchar2(255 char), modifierRef_relation varchar2(157 char), modifierRef_targetOid varchar2(36 char), modifierRef_type number(10,0), modifyChannel varchar2(255 char), modifyTimestamp timestamp, orderValue number(10,0), orgRef_relation varchar2(157 char), orgRef_targetOid varchar2(36 char), orgRef_type number(10,0), resourceRef_relation varchar2(157 char), resourceRef_targetOid varchar2(36 char), resourceRef_type number(10,0), targetRef_relation varchar2(157 char), targetRef_targetOid varchar2(36 char), targetRef_type number(10,0), tenantRef_relation varchar2(157 char), tenantRef_targetOid varchar2(36 char), tenantRef_type number(10,0), extId number(10,0), extOid varchar2(36 char), primary key (id, owner_oid)) initrans 30;
create table m_assignment_ext_boolean (item_id number(10,0) not null, anyContainer_owner_id number(10,0) not null, anyContainer_owner_owner_oid varchar2(36 char) not null, booleanValue number(1,0) not null, primary key ( anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, booleanValue)) initrans 30;
create table m_assignment_ext_date (item_id number(10,0) not null, anyContainer_owner_id number(10,0) not null, anyContainer_owner_owner_oid varchar2(36 char) not null, dateValue timestamp not null, primary key ( anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, dateValue)) initrans 30;
create table m_assignment_ext_long (item_id number(10,0) not null, anyContainer_owner_id number(10,0) not null, anyContainer_owner_owner_oid varchar2(36 char) not null, longValue number(19,0) not null, primary key ( anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, longValue)) initrans 30;
create table m_assignment_ext_poly (item_id number(10,0) not null, anyContainer_owner_id number(10,0) not null, anyContainer_owner_owner_oid varchar2(36 char) not null, orig varchar2(255 char) not null, norm varchar2(255 char), primary key ( anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, orig)) initrans 30;
create table m_assignment_ext_reference (item_id number(10,0) not null, anyContainer_owner_id number(10,0) not null, anyContainer_owner_owner_oid varchar2(36 char) not null, targetoid varchar2(36 char) not null, relation varchar2(157 char), targetType number(10,0), primary key ( anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, targetoid)) initrans 30;
create table m_assignment_ext_string (item_id number(10,0) not null, anyContainer_owner_id number(10,0) not null, anyContainer_owner_owner_oid varchar2(36 char) not null, stringValue varchar2(255 char) not null, primary key ( anyContainer_owner_owner_oid, anyContainer_owner_id, item_id, stringValue)) initrans 30;
create table m_assignment_extension (owner_id number(10,0) not null, owner_owner_oid varchar2(36 char) not null, booleansCount number(5,0), datesCount number(5,0), longsCount number(5,0), polysCount number(5,0), referencesCount number(5,0), stringsCount number(5,0), primary key (owner_id, owner_owner_oid)) initrans 30;
create table m_assignment_policy_situation (assignment_id number(10,0) not null, assignment_oid varchar2(36 char) not null, policySituation varchar2(255 char)) initrans 30;
create table m_assignment_reference (owner_id number(10,0) not null, owner_owner_oid varchar2(36 char) not null, reference_type number(10,0) not null, relation varchar2(157 char) not null, targetOid varchar2(36 char) not null, targetType number(10,0), primary key (owner_id, owner_owner_oid, reference_type, relation, targetOid)) initrans 30;
create table m_audit_delta (checksum varchar2(32 char) not null, record_id number(19,0) not null, delta blob, deltaOid varchar2(36 char), deltaType number(10,0), fullResult blob, objectName_norm varchar2(255 char), objectName_orig varchar2(255 char), resourceName_norm varchar2(255 char), resourceName_orig varchar2(255 char), resourceOid varchar2(36 char), status number(10,0), primary key (checksum, record_id)) initrans 30;
create table m_audit_event (id number(19,0) generated as identity, attorneyName varchar2(255 char), attorneyOid varchar2(36 char), channel varchar2(255 char), eventIdentifier varchar2(255 char), eventStage number(10,0), eventType number(10,0), hostIdentifier varchar2(255 char), initiatorName varchar2(255 char), initiatorOid varchar2(36 char), initiatorType number(10,0), message varchar2(1024 char), nodeIdentifier varchar2(255 char), outcome number(10,0), parameter varchar2(255 char), remoteHostAddress varchar2(255 char), result varchar2(255 char), sessionIdentifier varchar2(255 char), targetName varchar2(255 char), targetOid varchar2(36 char), targetOwnerName varchar2(255 char), targetOwnerOid varchar2(36 char), targetType number(10,0), taskIdentifier varchar2(255 char), taskOID varchar2(255 char), timestampValue timestamp, primary key (id)) initrans 30;
create table m_audit_item (changedItemPath varchar2(900 char) not null, record_id number(19,0) not null, primary key (changedItemPath, record_id)) initrans 30;
create table m_audit_prop_value (id number(19,0) generated as identity, name varchar2(255 char), record_id number(19,0), value varchar2(1024 char), primary key (id)) initrans 30;
create table m_audit_ref_value (id number(19,0) generated as identity, name varchar2(255 char), oid varchar2(255 char), record_id number(19,0), targetName_norm varchar2(255 char), targetName_orig varchar2(255 char), type varchar2(255 char), primary key (id)) initrans 30;
create table m_connector_target_system (connector_oid varchar2(36 char) not null, targetSystemType varchar2(255 char)) initrans 30;
create table m_ext_item (id number(10,0) generated as identity, kind number(10,0), itemName varchar2(157 char), itemType varchar2(157 char), primary key (id)) initrans 30;
create table m_focus_photo (owner_oid varchar2(36 char) not null, photo blob, primary key (owner_oid)) initrans 30;
create table m_focus_policy_situation (focus_oid varchar2(36 char) not null, policySituation varchar2(255 char)) initrans 30;
create table m_object (oid varchar2(36 char) not null, booleansCount number(5,0), createChannel varchar2(255 char), createTimestamp timestamp, creatorRef_relation varchar2(157 char), creatorRef_targetOid varchar2(36 char), creatorRef_type number(10,0), datesCount number(5,0), fullObject blob, lifecycleState varchar2(255 char), longsCount number(5,0), modifierRef_relation varchar2(157 char), modifierRef_targetOid varchar2(36 char), modifierRef_type number(10,0), modifyChannel varchar2(255 char), modifyTimestamp timestamp, name_norm varchar2(255 char), name_orig varchar2(255 char), objectTypeClass number(10,0), polysCount number(5,0), referencesCount number(5,0), stringsCount number(5,0), tenantRef_relation varchar2(157 char), tenantRef_targetOid varchar2(36 char), tenantRef_type number(10,0), version number(10,0) not null, primary key (oid)) initrans 30;
create table m_object_ext_boolean (item_id number(10,0) not null, owner_oid varchar2(36 char) not null, ownerType number(10,0) not null, booleanValue number(1,0) not null, primary key (owner_oid, ownerType, item_id, booleanValue)) initrans 30;
create table m_object_ext_date (item_id number(10,0) not null, owner_oid varchar2(36 char) not null, ownerType number(10,0) not null, dateValue timestamp not null, primary key (owner_oid, ownerType, item_id, dateValue)) initrans 30;
create table m_object_ext_long (item_id number(10,0) not null, owner_oid varchar2(36 char) not null, ownerType number(10,0) not null, longValue number(19,0) not null, primary key (owner_oid, ownerType, item_id, longValue)) initrans 30;
create table m_object_ext_poly (item_id number(10,0) not null, owner_oid varchar2(36 char) not null, ownerType number(10,0) not null, orig varchar2(255 char) not null, norm varchar2(255 char), primary key (owner_oid, ownerType, item_id, orig)) initrans 30;
create table m_object_ext_reference (item_id number(10,0) not null, owner_oid varchar2(36 char) not null, ownerType number(10,0) not null, targetoid varchar2(36 char) not null, relation varchar2(157 char), targetType number(10,0), primary key (owner_oid, ownerType, item_id, targetoid)) initrans 30;
create table m_object_ext_string (item_id number(10,0) not null, owner_oid varchar2(36 char) not null, ownerType number(10,0) not null, stringValue varchar2(255 char) not null, primary key (owner_oid, ownerType, item_id, stringValue)) initrans 30;
create table m_object_text_info (owner_oid varchar2(36 char) not null, text varchar2(255 char) not null, primary key (owner_oid, text)) initrans 30;
create table m_operation_execution (id number(10,0) not null, owner_oid varchar2(36 char) not null, initiatorRef_relation varchar2(157 char), initiatorRef_targetOid varchar2(36 char), initiatorRef_type number(10,0), status number(10,0), taskRef_relation varchar2(157 char), taskRef_targetOid varchar2(36 char), taskRef_type number(10,0), timestampValue timestamp, primary key (id, owner_oid)) initrans 30;
create table m_org_closure (ancestor_oid varchar2(36 char) not null, descendant_oid varchar2(36 char) not null, val number(10,0), primary key (ancestor_oid, descendant_oid)) initrans 30;
create table m_org_org_type (org_oid varchar2(36 char) not null, orgType varchar2(255 char)) initrans 30;
create table m_reference (owner_oid varchar2(36 char) not null, reference_type number(10,0) not null, relation varchar2(157 char) not null, targetOid varchar2(36 char) not null, targetType number(10,0), primary key (owner_oid, reference_type, relation, targetOid)) initrans 30;
create table m_service_type (service_oid varchar2(36 char) not null, serviceType varchar2(255 char)) initrans 30;
create table m_shadow (attemptNumber number(10,0), dead number(1,0), exist number(1,0), failedOperationType number(10,0), fullSynchronizationTimestamp timestamp, intent varchar2(255 char), kind number(10,0), name_norm varchar2(255 char), name_orig varchar2(255 char), objectClass varchar2(157 char), pendingOperationCount number(10,0), resourceRef_relation varchar2(157 char), resourceRef_targetOid varchar2(36 char), resourceRef_type number(10,0), status number(10,0), synchronizationSituation number(10,0), synchronizationTimestamp timestamp, oid varchar2(36 char) not null, primary key (oid)) initrans 30;
create table m_task (binding number(10,0), canRunOnNode varchar2(255 char), category varchar2(255 char), completionTimestamp timestamp, executionStatus number(10,0), handlerUri varchar2(255 char), lastRunFinishTimestamp timestamp, lastRunStartTimestamp timestamp, name_norm varchar2(255 char), name_orig varchar2(255 char), node varchar2(255 char), objectRef_relation varchar2(157 char), objectRef_targetOid varchar2(36 char), objectRef_type number(10,0), ownerRef_relation varchar2(157 char), ownerRef_targetOid varchar2(36 char), ownerRef_type number(10,0), parent varchar2(255 char), recurrence number(10,0), status number(10,0), taskIdentifier varchar2(255 char), threadStopAction number(10,0), waitingReason number(10,0), wfEndTimestamp timestamp, wfObjectRef_relation varchar2(157 char), wfObjectRef_targetOid varchar2(36 char), wfObjectRef_type number(10,0), wfProcessInstanceId varchar2(255 char), wfRequesterRef_relation varchar2(157 char), wfRequesterRef_targetOid varchar2(36 char), wfRequesterRef_type number(10,0), wfStartTimestamp timestamp, wfTargetRef_relation varchar2(157 char), wfTargetRef_targetOid varchar2(36 char), wfTargetRef_type number(10,0), oid varchar2(36 char) not null, primary key (oid)) initrans 30;
create table m_task_dependent (task_oid varchar2(36 char) not null, dependent varchar2(255 char)) initrans 30;
create table m_user_employee_type (user_oid varchar2(36 char) not null, employeeType varchar2(255 char)) initrans 30;
create table m_user_organization (user_oid varchar2(36 char) not null, norm varchar2(255 char), orig varchar2(255 char)) initrans 30;
create table m_user_organizational_unit (user_oid varchar2(36 char) not null, norm varchar2(255 char), orig varchar2(255 char)) initrans 30;
create table m_abstract_role (approvalProcess varchar2(255 char), autoassign_enabled number(1,0), displayName_norm varchar2(255 char), displayName_orig varchar2(255 char), identifier varchar2(255 char), ownerRef_relation varchar2(157 char), ownerRef_targetOid varchar2(36 char), ownerRef_type number(10,0), requestable number(1,0), riskLevel varchar2(255 char), oid varchar2(36 char) not null, primary key (oid)) initrans 30;
create table m_case (name_norm varchar2(255 char), name_orig varchar2(255 char), oid varchar2(36 char) not null, primary key (oid)) initrans 30;
create table m_connector (connectorBundle varchar2(255 char), connectorHostRef_relation varchar2(157 char), connectorHostRef_targetOid varchar2(36 char), connectorHostRef_type number(10,0), connectorType varchar2(255 char), connectorVersion varchar2(255 char), framework varchar2(255 char), name_norm varchar2(255 char), name_orig varchar2(255 char), oid varchar2(36 char) not null, primary key (oid)) initrans 30;
create table m_connector_host (hostname varchar2(255 char), name_norm varchar2(255 char), name_orig varchar2(255 char), port varchar2(255 char), oid varchar2(36 char) not null, primary key (oid)) initrans 30;
create table m_focus (administrativeStatus number(10,0), archiveTimestamp timestamp, disableReason varchar2(255 char), disableTimestamp timestamp, effectiveStatus number(10,0), enableTimestamp timestamp, validFrom timestamp, validTo timestamp, validityChangeTimestamp timestamp, validityStatus number(10,0), hasPhoto number(1,0) default false not null, oid varchar2(36 char) not null, primary key (oid)) initrans 30;
create table m_form (name_norm varchar2(255 char), name_orig varchar2(255 char), oid varchar2(36 char) not null, primary key (oid)) initrans 30;
create table m_function_library (name_norm varchar2(255 char), name_orig varchar2(255 char), oid varchar2(36 char) not null, primary key (oid)) initrans 30;
create table m_generic_object (name_norm varchar2(255 char), name_orig varchar2(255 char), objectType varchar2(255 char), oid varchar2(36 char) not null, primary key (oid)) initrans 30;
create table m_lookup_table (name_norm varchar2(255 char), name_orig varchar2(255 char), oid varchar2(36 char) not null, primary key (oid)) initrans 30;
create table m_lookup_table_row (id number(10,0) not null, owner_oid varchar2(36 char) not null, row_key varchar2(255 char), label_norm varchar2(255 char), label_orig varchar2(255 char), lastChangeTimestamp timestamp, row_value varchar2(255 char), primary key (id, owner_oid)) initrans 30;
create table m_node (name_norm varchar2(255 char), name_orig varchar2(255 char), nodeIdentifier varchar2(255 char), oid varchar2(36 char) not null, primary key (oid)) initrans 30;
create table m_object_template (name_norm varchar2(255 char), name_orig varchar2(255 char), type number(10,0), oid varchar2(36 char) not null, primary key (oid)) initrans 30;
create table m_org (costCenter varchar2(255 char), displayOrder number(10,0), locality_norm varchar2(255 char), locality_orig varchar2(255 char), name_norm varchar2(255 char), name_orig varchar2(255 char), tenant number(1,0), oid varchar2(36 char) not null, primary key (oid)) initrans 30;
create table m_report (export number(10,0), name_norm varchar2(255 char), name_orig varchar2(255 char), orientation number(10,0), parent number(1,0), useHibernateSession number(1,0), oid varchar2(36 char) not null, primary key (oid)) initrans 30;
create table m_report_output (name_norm varchar2(255 char), name_orig varchar2(255 char), reportRef_relation varchar2(157 char), reportRef_targetOid varchar2(36 char), reportRef_type number(10,0), oid varchar2(36 char) not null, primary key (oid)) initrans 30;
create table m_resource (administrativeState number(10,0), connectorRef_relation varchar2(157 char), connectorRef_targetOid varchar2(36 char), connectorRef_type number(10,0), name_norm varchar2(255 char), name_orig varchar2(255 char), o16_lastAvailabilityStatus number(10,0), oid varchar2(36 char) not null, primary key (oid)) initrans 30;
create table m_role (name_norm varchar2(255 char), name_orig varchar2(255 char), roleType varchar2(255 char), oid varchar2(36 char) not null, primary key (oid)) initrans 30;
create table m_security_policy (name_norm varchar2(255 char), name_orig varchar2(255 char), oid varchar2(36 char) not null, primary key (oid)) initrans 30;
create table m_sequence (name_norm varchar2(255 char), name_orig varchar2(255 char), oid varchar2(36 char) not null, primary key (oid)) initrans 30;
create table m_service (displayOrder number(10,0), locality_norm varchar2(255 char), locality_orig varchar2(255 char), name_norm varchar2(255 char), name_orig varchar2(255 char), oid varchar2(36 char) not null, primary key (oid)) initrans 30;
create table m_system_configuration (name_norm varchar2(255 char), name_orig varchar2(255 char), oid varchar2(36 char) not null, primary key (oid)) initrans 30;
create table m_trigger (id number(10,0) not null, owner_oid varchar2(36 char) not null, handlerUri varchar2(255 char), timestampValue timestamp, primary key (id, owner_oid)) initrans 30;
create table m_user (additionalName_norm varchar2(255 char), additionalName_orig varchar2(255 char), costCenter varchar2(255 char), emailAddress varchar2(255 char), employeeNumber varchar2(255 char), familyName_norm varchar2(255 char), familyName_orig varchar2(255 char), fullName_norm varchar2(255 char), fullName_orig varchar2(255 char), givenName_norm varchar2(255 char), givenName_orig varchar2(255 char), honorificPrefix_norm varchar2(255 char), honorificPrefix_orig varchar2(255 char), honorificSuffix_norm varchar2(255 char), honorificSuffix_orig varchar2(255 char), locale varchar2(255 char), locality_norm varchar2(255 char), locality_orig varchar2(255 char), name_norm varchar2(255 char), name_orig varchar2(255 char), nickName_norm varchar2(255 char), nickName_orig varchar2(255 char), preferredLanguage varchar2(255 char), status number(10,0), telephoneNumber varchar2(255 char), timezone varchar2(255 char), title_norm varchar2(255 char), title_orig varchar2(255 char), oid varchar2(36 char) not null, primary key (oid)) initrans 30;
create table m_value_policy (name_norm varchar2(255 char), name_orig varchar2(255 char), oid varchar2(36 char) not null, primary key (oid)) initrans 30;
alter table m_acc_cert_campaign add constraint uc_acc_cert_campaign_name unique (name_norm);
create index iCaseObjectRefTargetOid on m_acc_cert_case (objectRef_targetOid) initrans 30;
create index iCaseTargetRefTargetOid on m_acc_cert_case (targetRef_targetOid) initrans 30;
create index iCaseTenantRefTargetOid on m_acc_cert_case (tenantRef_targetOid) initrans 30;
create index iCaseOrgRefTargetOid on m_acc_cert_case (orgRef_targetOid) initrans 30;
alter table m_acc_cert_definition add constraint uc_acc_cert_definition_name unique (name_norm);
create index iCertWorkItemRefTargetOid on m_acc_cert_wi_reference (targetOid) initrans 30;
create index iAssignmentAdministrative on m_assignment (administrativeStatus) initrans 30;
create index iAssignmentEffective on m_assignment (effectiveStatus) initrans 30;
create index iTargetRefTargetOid on m_assignment (targetRef_targetOid) initrans 30;
create index iTenantRefTargetOid on m_assignment (tenantRef_targetOid) initrans 30;
create index iOrgRefTargetOid on m_assignment (orgRef_targetOid) initrans 30;
create index iResourceRefTargetOid on m_assignment (resourceRef_targetOid) initrans 30;
create index iAExtensionBoolean on m_assignment_ext_boolean (booleanValue) initrans 30;
create index iAExtensionDate on m_assignment_ext_date (dateValue) initrans 30;
create index iAExtensionLong on m_assignment_ext_long (longValue) initrans 30;
create index iAExtensionPolyString on m_assignment_ext_poly (orig) initrans 30;
create index iAExtensionReference on m_assignment_ext_reference (targetoid) initrans 30;
create index iAExtensionString on m_assignment_ext_string (stringValue) initrans 30;
create index iAssignmentReferenceTargetOid on m_assignment_reference (targetOid) initrans 30;
create index iTimestampValue on m_audit_event (timestampValue) initrans 30;
create index iChangedItemPath on m_audit_item (changedItemPath) initrans 30;
create index iAuditPropValRecordId on m_audit_prop_value (record_id) initrans 30;
create index iAuditRefValRecordId on m_audit_ref_value (record_id) initrans 30;
create index iObjectNameOrig on m_object (name_orig) initrans 30;
create index iObjectNameNorm on m_object (name_norm) initrans 30;
create index iObjectTypeClass on m_object (objectTypeClass) initrans 30;
create index iObjectCreateTimestamp on m_object (createTimestamp) initrans 30;
create index iObjectLifecycleState on m_object (lifecycleState) initrans 30;
create index iExtensionBoolean on m_object_ext_boolean (booleanValue) initrans 30;
create index iExtensionDate on m_object_ext_date (dateValue) initrans 30;
create index iExtensionLong on m_object_ext_long (longValue) initrans 30;
create index iExtensionPolyString on m_object_ext_poly (orig) initrans 30;
create index iExtensionReference on m_object_ext_reference (targetoid) initrans 30;
create index iExtensionString on m_object_ext_string (stringValue) initrans 30;
create index iOpExecTaskOid on m_operation_execution (taskRef_targetOid) initrans 30;
create index iOpExecInitiatorOid on m_operation_execution (initiatorRef_targetOid) initrans 30;
create index iOpExecStatus on m_operation_execution (status) initrans 30;
create index iOpExecOwnerOid on m_operation_execution (owner_oid) initrans 30;
create index iAncestor on m_org_closure (ancestor_oid) initrans 30;
create index iDescendant on m_org_closure (descendant_oid) initrans 30;
create index iDescendantAncestor on m_org_closure (descendant_oid, ancestor_oid) initrans 30;
create index iReferenceTargetOid on m_reference (targetOid) initrans 30;
create index iShadowResourceRef on m_shadow (resourceRef_targetOid) initrans 30;
create index iShadowDead on m_shadow (dead) initrans 30;
create index iShadowKind on m_shadow (kind) initrans 30;
create index iShadowIntent on m_shadow (intent) initrans 30;
create index iShadowObjectClass on m_shadow (objectClass) initrans 30;
create index iShadowFailedOperationType on m_shadow (failedOperationType) initrans 30;
create index iShadowSyncSituation on m_shadow (synchronizationSituation) initrans 30;
create index iShadowPendingOperationCount on m_shadow (pendingOperationCount) initrans 30;
create index iParent on m_task (parent) initrans 30;
create index iTaskWfProcessInstanceId on m_task (wfProcessInstanceId) initrans 30;
create index iTaskWfStartTimestamp on m_task (wfStartTimestamp) initrans 30;
create index iTaskWfEndTimestamp on m_task (wfEndTimestamp) initrans 30;
create index iTaskWfRequesterOid on m_task (wfRequesterRef_targetOid) initrans 30;
create index iTaskWfObjectOid on m_task (wfObjectRef_targetOid) initrans 30;
create index iTaskWfTargetOid on m_task (wfTargetRef_targetOid) initrans 30;
alter table m_task add constraint uc_task_identifier unique (taskIdentifier);
create index iAbstractRoleIdentifier on m_abstract_role (identifier) initrans 30;
create index iRequestable on m_abstract_role (requestable) initrans 30;
create index iAutoassignEnabled on m_abstract_role (autoassign_enabled) initrans 30;
alter table m_case add constraint uc_case_name unique (name_norm);
alter table m_connector_host add constraint uc_connector_host_name unique (name_norm);
create index iFocusAdministrative on m_focus (administrativeStatus) initrans 30;
create index iFocusEffective on m_focus (effectiveStatus) initrans 30;
alter table m_form add constraint uc_form_name unique (name_norm);
alter table m_function_library add constraint uc_function_library_name unique (name_norm);
alter table m_generic_object add constraint uc_generic_object_name unique (name_norm);
alter table m_lookup_table add constraint uc_lookup_name unique (name_norm);
alter table m_lookup_table_row add constraint uc_row_key unique (owner_oid, row_key);
alter table m_node add constraint uc_node_name unique (name_norm);
alter table m_object_template add constraint uc_object_template_name unique (name_norm);
create index iDisplayOrder on m_org (displayOrder) initrans 30;
alter table m_org add constraint uc_org_name unique (name_norm);
create index iReportParent on m_report (parent) initrans 30;
alter table m_report add constraint uc_report_name unique (name_norm);
alter table m_resource add constraint uc_resource_name unique (name_norm);
alter table m_role add constraint uc_role_name unique (name_norm);
alter table m_security_policy add constraint uc_security_policy_name unique (name_norm);
alter table m_sequence add constraint uc_sequence_name unique (name_norm);
alter table m_system_configuration add constraint uc_system_configuration_name unique (name_norm);
create index iTriggerTimestamp on m_trigger (timestampValue) initrans 30;
create index iEmployeeNumber on m_user (employeeNumber) initrans 30;
create index iFullName on m_user (fullName_orig) initrans 30;
create index iFamilyName on m_user (familyName_orig) initrans 30;
create index iGivenName on m_user (givenName_orig) initrans 30;
create index iLocality on m_user (locality_orig) initrans 30;
alter table m_user add constraint uc_user_name unique (name_norm);
alter table m_value_policy add constraint uc_value_policy_name unique (name_norm);
alter table m_acc_cert_campaign add constraint fk_acc_cert_campaign foreign key (oid) references m_object;
alter table m_acc_cert_case add constraint fk_acc_cert_case_owner foreign key (owner_oid) references m_acc_cert_campaign;
alter table m_acc_cert_definition add constraint fk_acc_cert_definition foreign key (oid) references m_object;
alter table m_acc_cert_wi add constraint fk_acc_cert_wi_owner foreign key (owner_id, owner_owner_oid) references m_acc_cert_case;
alter table m_acc_cert_wi_reference add constraint fk_acc_cert_wi_ref_owner foreign key (owner_id, owner_owner_id, owner_owner_owner_oid) references m_acc_cert_wi;
alter table m_assignment add constraint fk_assignment_owner foreign key (owner_oid) references m_object;
alter table m_assignment_ext_boolean add constraint fk_a_ext_boolean_owner foreign key (anyContainer_owner_id, anyContainer_owner_owner_oid) references m_assignment_extension;
alter table m_assignment_ext_boolean add constraint fk_a_ext_boolean_item foreign key (item_id) references m_ext_item;
alter table m_assignment_ext_date add constraint fk_a_ext_date_owner foreign key (anyContainer_owner_id, anyContainer_owner_owner_oid) references m_assignment_extension;
alter table m_assignment_ext_date add constraint fk_a_ext_date_item foreign key (item_id) references m_ext_item;
alter table m_assignment_ext_long add constraint fk_a_ext_long_owner foreign key (anyContainer_owner_id, anyContainer_owner_owner_oid) references m_assignment_extension;
alter table m_assignment_ext_long add constraint fk_a_ext_long_item foreign key (item_id) references m_ext_item;
alter table m_assignment_ext_poly add constraint fk_a_ext_poly_owner foreign key (anyContainer_owner_id, anyContainer_owner_owner_oid) references m_assignment_extension;
alter table m_assignment_ext_poly add constraint fk_a_ext_poly_item foreign key (item_id) references m_ext_item;
alter table m_assignment_ext_reference add constraint fk_a_ext_reference_owner foreign key (anyContainer_owner_id, anyContainer_owner_owner_oid) references m_assignment_extension;
alter table m_assignment_ext_reference add constraint fk_a_ext_boolean_reference foreign key (item_id) references m_ext_item;
alter table m_assignment_ext_string add constraint fk_a_ext_string_owner foreign key (anyContainer_owner_id, anyContainer_owner_owner_oid) references m_assignment_extension;
alter table m_assignment_ext_string add constraint fk_a_ext_string_item foreign key (item_id) references m_ext_item;
alter table m_assignment_policy_situation add constraint fk_assignment_policy_situation foreign key (assignment_id, assignment_oid) references m_assignment;
alter table m_assignment_reference add constraint fk_assignment_reference foreign key (owner_id, owner_owner_oid) references m_assignment;
alter table m_audit_delta add constraint fk_audit_delta foreign key (record_id) references m_audit_event;
alter table m_audit_item add constraint fk_audit_item foreign key (record_id) references m_audit_event;
alter table m_audit_prop_value add constraint fk_audit_prop_value foreign key (record_id) references m_audit_event;
alter table m_audit_ref_value add constraint fk_audit_ref_value foreign key (record_id) references m_audit_event;
alter table m_connector_target_system add constraint fk_connector_target_system foreign key (connector_oid) references m_connector;
alter table m_focus_photo add constraint fk_focus_photo foreign key (owner_oid) references m_focus;
alter table m_focus_policy_situation add constraint fk_focus_policy_situation foreign key (focus_oid) references m_focus;
alter table m_object_ext_boolean add constraint fk_o_ext_boolean_owner foreign key (owner_oid) references m_object;
alter table m_object_ext_boolean add constraint fk_o_ext_boolean_item foreign key (item_id) references m_ext_item;
alter table m_object_ext_date add constraint fk_o_ext_date_owner foreign key (owner_oid) references m_object;
alter table m_object_ext_date add constraint fk_o_ext_date_item foreign key (item_id) references m_ext_item;
alter table m_object_ext_long add constraint fk_object_ext_long foreign key (owner_oid) references m_object;
alter table m_object_ext_long add constraint fk_o_ext_long_item foreign key (item_id) references m_ext_item;
alter table m_object_ext_poly add constraint fk_o_ext_poly_owner foreign key (owner_oid) references m_object;
alter table m_object_ext_poly add constraint fk_o_ext_poly_item foreign key (item_id) references m_ext_item;
alter table m_object_ext_reference add constraint fk_o_ext_reference_owner foreign key (owner_oid) references m_object;
alter table m_object_ext_reference add constraint fk_o_ext_reference_item foreign key (item_id) references m_ext_item;
alter table m_object_ext_string add constraint fk_object_ext_string foreign key (owner_oid) references m_object;
alter table m_object_ext_string add constraint fk_o_ext_string_item foreign key (item_id) references m_ext_item;
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

--
-- A hint submitted by a user: Oracle DB MUST be created as "shared" and the
-- job_queue_processes parameter  must be greater than 2
-- However, these settings are pretty much standard after any
-- Oracle install, so most users need not worry about this.
--
-- Many other users (including the primary author of Quartz) have had success
-- running in dedicated mode, so only consider the above as a hint
--


-- there are two semicolons at the end of each of the following lines to work around a bug/feature of ScriptRunner we use
BEGIN
BEGIN   EXECUTE IMMEDIATE 'DROP TABLE qrtz_calendars';           EXCEPTION   WHEN OTHERS THEN      IF SQLCODE != -942 THEN         RAISE;      END IF;     END;
BEGIN   EXECUTE IMMEDIATE 'DROP TABLE qrtz_fired_triggers';      EXCEPTION   WHEN OTHERS THEN      IF SQLCODE != -942 THEN         RAISE;      END IF;     END;
BEGIN   EXECUTE IMMEDIATE 'DROP TABLE qrtz_blob_triggers';       EXCEPTION   WHEN OTHERS THEN      IF SQLCODE != -942 THEN         RAISE;      END IF;     END;
BEGIN   EXECUTE IMMEDIATE 'DROP TABLE qrtz_cron_triggers';       EXCEPTION   WHEN OTHERS THEN      IF SQLCODE != -942 THEN         RAISE;      END IF;     END;
BEGIN   EXECUTE IMMEDIATE 'DROP TABLE qrtz_simple_triggers';     EXCEPTION   WHEN OTHERS THEN      IF SQLCODE != -942 THEN         RAISE;      END IF;     END;
BEGIN   EXECUTE IMMEDIATE 'DROP TABLE qrtz_simprop_triggers';    EXCEPTION   WHEN OTHERS THEN      IF SQLCODE != -942 THEN         RAISE;      END IF;     END;
BEGIN   EXECUTE IMMEDIATE 'DROP TABLE qrtz_triggers';            EXCEPTION   WHEN OTHERS THEN      IF SQLCODE != -942 THEN         RAISE;      END IF;     END;
BEGIN   EXECUTE IMMEDIATE 'DROP TABLE qrtz_job_details';         EXCEPTION   WHEN OTHERS THEN      IF SQLCODE != -942 THEN         RAISE;      END IF;     END;
BEGIN   EXECUTE IMMEDIATE 'DROP TABLE qrtz_paused_trigger_grps'; EXCEPTION   WHEN OTHERS THEN      IF SQLCODE != -942 THEN         RAISE;      END IF;     END;
BEGIN   EXECUTE IMMEDIATE 'DROP TABLE qrtz_locks';               EXCEPTION   WHEN OTHERS THEN      IF SQLCODE != -942 THEN         RAISE;      END IF;     END;
BEGIN   EXECUTE IMMEDIATE 'DROP TABLE qrtz_scheduler_state';     EXCEPTION   WHEN OTHERS THEN      IF SQLCODE != -942 THEN         RAISE;      END IF;     END;
END;
/

CREATE TABLE qrtz_job_details
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    JOB_NAME  VARCHAR2(200) NOT NULL,
    JOB_GROUP VARCHAR2(200) NOT NULL,
    DESCRIPTION VARCHAR2(250) NULL,
    JOB_CLASS_NAME   VARCHAR2(250) NOT NULL,
    IS_DURABLE VARCHAR2(1) NOT NULL,
    IS_NONCONCURRENT VARCHAR2(1) NOT NULL,
    IS_UPDATE_DATA VARCHAR2(1) NOT NULL,
    REQUESTS_RECOVERY VARCHAR2(1) NOT NULL,
    JOB_DATA BLOB NULL,
    CONSTRAINT QRTZ_JOB_DETAILS_PK PRIMARY KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
);
CREATE TABLE qrtz_triggers
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    TRIGGER_NAME VARCHAR2(200) NOT NULL,
    TRIGGER_GROUP VARCHAR2(200) NOT NULL,
    JOB_NAME  VARCHAR2(200) NOT NULL,
    JOB_GROUP VARCHAR2(200) NOT NULL,
    DESCRIPTION VARCHAR2(250) NULL,
    NEXT_FIRE_TIME NUMBER(13) NULL,
    PREV_FIRE_TIME NUMBER(13) NULL,
    PRIORITY NUMBER(13) NULL,
    EXECUTION_GROUP VARCHAR2(200) NULL,
    TRIGGER_STATE VARCHAR2(16) NOT NULL,
    TRIGGER_TYPE VARCHAR2(8) NOT NULL,
    START_TIME NUMBER(13) NOT NULL,
    END_TIME NUMBER(13) NULL,
    CALENDAR_NAME VARCHAR2(200) NULL,
    MISFIRE_INSTR NUMBER(2) NULL,
    JOB_DATA BLOB NULL,
    CONSTRAINT QRTZ_TRIGGERS_PK PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    CONSTRAINT QRTZ_TRIGGER_TO_JOBS_FK FOREIGN KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
      REFERENCES QRTZ_JOB_DETAILS(SCHED_NAME,JOB_NAME,JOB_GROUP)
);
CREATE TABLE qrtz_simple_triggers
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    TRIGGER_NAME VARCHAR2(200) NOT NULL,
    TRIGGER_GROUP VARCHAR2(200) NOT NULL,
    REPEAT_COUNT NUMBER(7) NOT NULL,
    REPEAT_INTERVAL NUMBER(12) NOT NULL,
    TIMES_TRIGGERED NUMBER(10) NOT NULL,
    CONSTRAINT QRTZ_SIMPLE_TRIG_PK PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    CONSTRAINT QRTZ_SIMPLE_TRIG_TO_TRIG_FK FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
	REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);
CREATE TABLE qrtz_cron_triggers
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    TRIGGER_NAME VARCHAR2(200) NOT NULL,
    TRIGGER_GROUP VARCHAR2(200) NOT NULL,
    CRON_EXPRESSION VARCHAR2(120) NOT NULL,
    TIME_ZONE_ID VARCHAR2(80),
    CONSTRAINT QRTZ_CRON_TRIG_PK PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    CONSTRAINT QRTZ_CRON_TRIG_TO_TRIG_FK FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
      REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);
CREATE TABLE qrtz_simprop_triggers
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    TRIGGER_NAME VARCHAR2(200) NOT NULL,
    TRIGGER_GROUP VARCHAR2(200) NOT NULL,
    STR_PROP_1 VARCHAR2(512) NULL,
    STR_PROP_2 VARCHAR2(512) NULL,
    STR_PROP_3 VARCHAR2(512) NULL,
    INT_PROP_1 NUMBER(10) NULL,
    INT_PROP_2 NUMBER(10) NULL,
    LONG_PROP_1 NUMBER(13) NULL,
    LONG_PROP_2 NUMBER(13) NULL,
    DEC_PROP_1 NUMERIC(13,4) NULL,
    DEC_PROP_2 NUMERIC(13,4) NULL,
    BOOL_PROP_1 VARCHAR2(1) NULL,
    BOOL_PROP_2 VARCHAR2(1) NULL,
    CONSTRAINT QRTZ_SIMPROP_TRIG_PK PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    CONSTRAINT QRTZ_SIMPROP_TRIG_TO_TRIG_FK FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
      REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);
CREATE TABLE qrtz_blob_triggers
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    TRIGGER_NAME VARCHAR2(200) NOT NULL,
    TRIGGER_GROUP VARCHAR2(200) NOT NULL,
    BLOB_DATA BLOB NULL,
    CONSTRAINT QRTZ_BLOB_TRIG_PK PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    CONSTRAINT QRTZ_BLOB_TRIG_TO_TRIG_FK FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);
CREATE TABLE qrtz_calendars
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    CALENDAR_NAME  VARCHAR2(200) NOT NULL,
    CALENDAR BLOB NOT NULL,
    CONSTRAINT QRTZ_CALENDARS_PK PRIMARY KEY (SCHED_NAME,CALENDAR_NAME)
);
CREATE TABLE qrtz_paused_trigger_grps
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    TRIGGER_GROUP  VARCHAR2(200) NOT NULL,
    CONSTRAINT QRTZ_PAUSED_TRIG_GRPS_PK PRIMARY KEY (SCHED_NAME,TRIGGER_GROUP)
);
CREATE TABLE qrtz_fired_triggers
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    ENTRY_ID VARCHAR2(95) NOT NULL,
    TRIGGER_NAME VARCHAR2(200) NOT NULL,
    TRIGGER_GROUP VARCHAR2(200) NOT NULL,
    INSTANCE_NAME VARCHAR2(200) NOT NULL,
    FIRED_TIME NUMBER(13) NOT NULL,
    SCHED_TIME NUMBER(13) NOT NULL,
    PRIORITY NUMBER(13) NOT NULL,
    EXECUTION_GROUP VARCHAR2(200) NULL,
    STATE VARCHAR2(16) NOT NULL,
    JOB_NAME VARCHAR2(200) NULL,
    JOB_GROUP VARCHAR2(200) NULL,
    IS_NONCONCURRENT VARCHAR2(1) NULL,
    REQUESTS_RECOVERY VARCHAR2(1) NULL,
    CONSTRAINT QRTZ_FIRED_TRIGGER_PK PRIMARY KEY (SCHED_NAME,ENTRY_ID)
);
CREATE TABLE qrtz_scheduler_state
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    INSTANCE_NAME VARCHAR2(200) NOT NULL,
    LAST_CHECKIN_TIME NUMBER(13) NOT NULL,
    CHECKIN_INTERVAL NUMBER(13) NOT NULL,
    CONSTRAINT QRTZ_SCHEDULER_STATE_PK PRIMARY KEY (SCHED_NAME,INSTANCE_NAME)
);
CREATE TABLE qrtz_locks
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    LOCK_NAME  VARCHAR2(40) NOT NULL,
    CONSTRAINT QRTZ_LOCKS_PK PRIMARY KEY (SCHED_NAME,LOCK_NAME)
);

create index idx_qrtz_j_req_recovery on qrtz_job_details(SCHED_NAME,REQUESTS_RECOVERY);
create index idx_qrtz_j_grp on qrtz_job_details(SCHED_NAME,JOB_GROUP);

create index idx_qrtz_t_j on qrtz_triggers(SCHED_NAME,JOB_NAME,JOB_GROUP);
create index idx_qrtz_t_jg on qrtz_triggers(SCHED_NAME,JOB_GROUP);
create index idx_qrtz_t_c on qrtz_triggers(SCHED_NAME,CALENDAR_NAME);
create index idx_qrtz_t_g on qrtz_triggers(SCHED_NAME,TRIGGER_GROUP);
create index idx_qrtz_t_state on qrtz_triggers(SCHED_NAME,TRIGGER_STATE);
create index idx_qrtz_t_n_state on qrtz_triggers(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP,TRIGGER_STATE);
create index idx_qrtz_t_n_g_state on qrtz_triggers(SCHED_NAME,TRIGGER_GROUP,TRIGGER_STATE);
create index idx_qrtz_t_next_fire_time on qrtz_triggers(SCHED_NAME,NEXT_FIRE_TIME);
create index idx_qrtz_t_nft_st on qrtz_triggers(SCHED_NAME,TRIGGER_STATE,NEXT_FIRE_TIME);
create index idx_qrtz_t_nft_misfire on qrtz_triggers(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME);
create index idx_qrtz_t_nft_st_misfire on qrtz_triggers(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_STATE);
create index idx_qrtz_t_nft_st_misfire_grp on qrtz_triggers(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_GROUP,TRIGGER_STATE);

create index idx_qrtz_ft_trig_inst_name on qrtz_fired_triggers(SCHED_NAME,INSTANCE_NAME);
create index idx_qrtz_ft_inst_job_req_rcvry on qrtz_fired_triggers(SCHED_NAME,INSTANCE_NAME,REQUESTS_RECOVERY);
create index idx_qrtz_ft_j_g on qrtz_fired_triggers(SCHED_NAME,JOB_NAME,JOB_GROUP);
create index idx_qrtz_ft_jg on qrtz_fired_triggers(SCHED_NAME,JOB_GROUP);
create index idx_qrtz_ft_t_g on qrtz_fired_triggers(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP);
create index idx_qrtz_ft_tg on qrtz_fired_triggers(SCHED_NAME,TRIGGER_GROUP);

create table ACT_GE_PROPERTY (
    NAME_ NVARCHAR2(64),
    VALUE_ NVARCHAR2(300),
    REV_ INTEGER,
    primary key (NAME_)
);

insert into ACT_GE_PROPERTY
values ('schema.version', '5.22.0.0', 1);

insert into ACT_GE_PROPERTY
values ('schema.history', 'create(5.22.0.0)', 1);

insert into ACT_GE_PROPERTY
values ('next.dbid', '1', 1);

create table ACT_GE_BYTEARRAY (
    ID_ NVARCHAR2(64),
    REV_ INTEGER,
    NAME_ NVARCHAR2(255),
    DEPLOYMENT_ID_ NVARCHAR2(64),
    BYTES_ BLOB,
    GENERATED_ NUMBER(1,0) CHECK (GENERATED_ IN (1,0)),
    primary key (ID_)
);

create table ACT_RE_DEPLOYMENT (
    ID_ NVARCHAR2(64),
    NAME_ NVARCHAR2(255),
    CATEGORY_ NVARCHAR2(255),
    TENANT_ID_ NVARCHAR2(255) DEFAULT '',
    DEPLOY_TIME_ TIMESTAMP(6),
    primary key (ID_)
);

create table ACT_RE_MODEL (
    ID_ NVARCHAR2(64) not null,
    REV_ INTEGER,
    NAME_ NVARCHAR2(255),
    KEY_ NVARCHAR2(255),
    CATEGORY_ NVARCHAR2(255),
    CREATE_TIME_ TIMESTAMP(6),
    LAST_UPDATE_TIME_ TIMESTAMP(6),
    VERSION_ INTEGER,
    META_INFO_ NVARCHAR2(2000),
    DEPLOYMENT_ID_ NVARCHAR2(64),
    EDITOR_SOURCE_VALUE_ID_ NVARCHAR2(64),
    EDITOR_SOURCE_EXTRA_VALUE_ID_ NVARCHAR2(64),
    TENANT_ID_ NVARCHAR2(255) DEFAULT '',
    primary key (ID_)
);

create table ACT_RU_EXECUTION (
    ID_ NVARCHAR2(64),
    REV_ INTEGER,
    PROC_INST_ID_ NVARCHAR2(64),
    BUSINESS_KEY_ NVARCHAR2(255),
    PARENT_ID_ NVARCHAR2(64),
    PROC_DEF_ID_ NVARCHAR2(64),
    SUPER_EXEC_ NVARCHAR2(64),
    ACT_ID_ NVARCHAR2(255),
    IS_ACTIVE_ NUMBER(1,0) CHECK (IS_ACTIVE_ IN (1,0)),
    IS_CONCURRENT_ NUMBER(1,0) CHECK (IS_CONCURRENT_ IN (1,0)),
    IS_SCOPE_ NUMBER(1,0) CHECK (IS_SCOPE_ IN (1,0)),
    IS_EVENT_SCOPE_ NUMBER(1,0) CHECK (IS_EVENT_SCOPE_ IN (1,0)),
    SUSPENSION_STATE_ INTEGER,
    CACHED_ENT_STATE_ INTEGER,
    TENANT_ID_ NVARCHAR2(255) DEFAULT '',
    NAME_ NVARCHAR2(255),
    LOCK_TIME_ TIMESTAMP(6),
    primary key (ID_)
);

create table ACT_RU_JOB (
    ID_ NVARCHAR2(64) NOT NULL,
    REV_ INTEGER,
    TYPE_ NVARCHAR2(255) NOT NULL,
    LOCK_EXP_TIME_ TIMESTAMP(6),
    LOCK_OWNER_ NVARCHAR2(255),
    EXCLUSIVE_ NUMBER(1,0) CHECK (EXCLUSIVE_ IN (1,0)),
    EXECUTION_ID_ NVARCHAR2(64),
    PROCESS_INSTANCE_ID_ NVARCHAR2(64),
    PROC_DEF_ID_ NVARCHAR2(64),
    RETRIES_ INTEGER,
    EXCEPTION_STACK_ID_ NVARCHAR2(64),
    EXCEPTION_MSG_ NVARCHAR2(2000),
    DUEDATE_ TIMESTAMP(6),
    REPEAT_ NVARCHAR2(255),
    HANDLER_TYPE_ NVARCHAR2(255),
    HANDLER_CFG_ NVARCHAR2(2000),
    TENANT_ID_ NVARCHAR2(255) DEFAULT '',
    primary key (ID_)
);

create table ACT_RE_PROCDEF (
    ID_ NVARCHAR2(64) NOT NULL,
    REV_ INTEGER,
    CATEGORY_ NVARCHAR2(255),
    NAME_ NVARCHAR2(255),
    KEY_ NVARCHAR2(255) NOT NULL,
    VERSION_ INTEGER NOT NULL,
    DEPLOYMENT_ID_ NVARCHAR2(64),
    RESOURCE_NAME_ NVARCHAR2(2000),
    DGRM_RESOURCE_NAME_ varchar(4000),
    DESCRIPTION_ NVARCHAR2(2000),
    HAS_START_FORM_KEY_ NUMBER(1,0) CHECK (HAS_START_FORM_KEY_ IN (1,0)),
    HAS_GRAPHICAL_NOTATION_ NUMBER(1,0) CHECK (HAS_GRAPHICAL_NOTATION_ IN (1,0)),
    SUSPENSION_STATE_ INTEGER,
    TENANT_ID_ NVARCHAR2(255) DEFAULT '',
    primary key (ID_)
);

create table ACT_RU_TASK (
    ID_ NVARCHAR2(64),
    REV_ INTEGER,
    EXECUTION_ID_ NVARCHAR2(64),
    PROC_INST_ID_ NVARCHAR2(64),
    PROC_DEF_ID_ NVARCHAR2(64),
    NAME_ NVARCHAR2(255),
    PARENT_TASK_ID_ NVARCHAR2(64),
    DESCRIPTION_ NVARCHAR2(2000),
    TASK_DEF_KEY_ NVARCHAR2(255),
    OWNER_ NVARCHAR2(255),
    ASSIGNEE_ NVARCHAR2(255),
    DELEGATION_ NVARCHAR2(64),
    PRIORITY_ INTEGER,
    CREATE_TIME_ TIMESTAMP(6),
    DUE_DATE_ TIMESTAMP(6),
    CATEGORY_ NVARCHAR2(255),
    SUSPENSION_STATE_ INTEGER,
    TENANT_ID_ NVARCHAR2(255) DEFAULT '',
    FORM_KEY_ NVARCHAR2(255),
    primary key (ID_)
);

create table ACT_RU_IDENTITYLINK (
    ID_ NVARCHAR2(64),
    REV_ INTEGER,
    GROUP_ID_ NVARCHAR2(255),
    TYPE_ NVARCHAR2(255),
    USER_ID_ NVARCHAR2(255),
    TASK_ID_ NVARCHAR2(64),
    PROC_INST_ID_ NVARCHAR2(64),
    PROC_DEF_ID_ NVARCHAR2(64),
    primary key (ID_)
);

create table ACT_RU_VARIABLE (
    ID_ NVARCHAR2(64) not null,
    REV_ INTEGER,
    TYPE_ NVARCHAR2(255) not null,
    NAME_ NVARCHAR2(255) not null,
    EXECUTION_ID_ NVARCHAR2(64),
    PROC_INST_ID_ NVARCHAR2(64),
    TASK_ID_ NVARCHAR2(64),
    BYTEARRAY_ID_ NVARCHAR2(64),
    DOUBLE_ NUMBER(*,10),
    LONG_ NUMBER(19,0),
    TEXT_ NVARCHAR2(2000),
    TEXT2_ NVARCHAR2(2000),
    primary key (ID_)
);

create table ACT_RU_EVENT_SUBSCR (
    ID_ NVARCHAR2(64) not null,
    REV_ integer,
    EVENT_TYPE_ NVARCHAR2(255) not null,
    EVENT_NAME_ NVARCHAR2(255),
    EXECUTION_ID_ NVARCHAR2(64),
    PROC_INST_ID_ NVARCHAR2(64),
    ACTIVITY_ID_ NVARCHAR2(64),
    CONFIGURATION_ NVARCHAR2(255),
    CREATED_ TIMESTAMP(6) not null,
    PROC_DEF_ID_ NVARCHAR2(64),
    TENANT_ID_ NVARCHAR2(255) DEFAULT '',
    primary key (ID_)
);

create table ACT_EVT_LOG (
    LOG_NR_ NUMBER(19),
    TYPE_ NVARCHAR2(64),
    PROC_DEF_ID_ NVARCHAR2(64),
    PROC_INST_ID_ NVARCHAR2(64),
    EXECUTION_ID_ NVARCHAR2(64),
    TASK_ID_ NVARCHAR2(64),
    TIME_STAMP_ TIMESTAMP(6) not null,
    USER_ID_ NVARCHAR2(255),
    DATA_ BLOB,
    LOCK_OWNER_ NVARCHAR2(255),
    LOCK_TIME_ TIMESTAMP(6) null,
    IS_PROCESSED_ NUMBER(3) default 0,
    primary key (LOG_NR_)
);

create sequence act_evt_log_seq;

create table ACT_PROCDEF_INFO (
	ID_ NVARCHAR2(64) not null,
    PROC_DEF_ID_ NVARCHAR2(64) not null,
    REV_ integer,
    INFO_JSON_ID_ NVARCHAR2(64),
    primary key (ID_)
);

create index ACT_IDX_EXEC_BUSKEY on ACT_RU_EXECUTION(BUSINESS_KEY_);
create index ACT_IDX_TASK_CREATE on ACT_RU_TASK(CREATE_TIME_);
create index ACT_IDX_IDENT_LNK_USER on ACT_RU_IDENTITYLINK(USER_ID_);
create index ACT_IDX_IDENT_LNK_GROUP on ACT_RU_IDENTITYLINK(GROUP_ID_);
create index ACT_IDX_EVENT_SUBSCR_CONFIG_ on ACT_RU_EVENT_SUBSCR(CONFIGURATION_);
create index ACT_IDX_VARIABLE_TASK_ID on ACT_RU_VARIABLE(TASK_ID_);

create index ACT_IDX_BYTEAR_DEPL on ACT_GE_BYTEARRAY(DEPLOYMENT_ID_);
alter table ACT_GE_BYTEARRAY
    add constraint ACT_FK_BYTEARR_DEPL
    foreign key (DEPLOYMENT_ID_)
    references ACT_RE_DEPLOYMENT (ID_);

alter table ACT_RE_PROCDEF
    add constraint ACT_UNIQ_PROCDEF
    unique (KEY_,VERSION_, TENANT_ID_);

create index ACT_IDX_EXE_PROCINST on ACT_RU_EXECUTION(PROC_INST_ID_);
alter table ACT_RU_EXECUTION
    add constraint ACT_FK_EXE_PROCINST
    foreign key (PROC_INST_ID_)
    references ACT_RU_EXECUTION (ID_);

create index ACT_IDX_EXE_PARENT on ACT_RU_EXECUTION(PARENT_ID_);
alter table ACT_RU_EXECUTION
    add constraint ACT_FK_EXE_PARENT
    foreign key (PARENT_ID_)
    references ACT_RU_EXECUTION (ID_);

create index ACT_IDX_EXE_SUPER on ACT_RU_EXECUTION(SUPER_EXEC_);
alter table ACT_RU_EXECUTION
    add constraint ACT_FK_EXE_SUPER
    foreign key (SUPER_EXEC_)
    references ACT_RU_EXECUTION (ID_);

create index ACT_IDX_EXE_PROCDEF on ACT_RU_EXECUTION(PROC_DEF_ID_);
alter table ACT_RU_EXECUTION
    add constraint ACT_FK_EXE_PROCDEF
    foreign key (PROC_DEF_ID_)
    references ACT_RE_PROCDEF (ID_);

create index ACT_IDX_TSKASS_TASK on ACT_RU_IDENTITYLINK(TASK_ID_);
alter table ACT_RU_IDENTITYLINK
    add constraint ACT_FK_TSKASS_TASK
    foreign key (TASK_ID_)
    references ACT_RU_TASK (ID_);

create index ACT_IDX_ATHRZ_PROCEDEF  on ACT_RU_IDENTITYLINK(PROC_DEF_ID_);
alter table ACT_RU_IDENTITYLINK
    add constraint ACT_FK_ATHRZ_PROCEDEF
    foreign key (PROC_DEF_ID_)
    references ACT_RE_PROCDEF (ID_);

create index ACT_IDX_IDL_PROCINST on ACT_RU_IDENTITYLINK(PROC_INST_ID_);
alter table ACT_RU_IDENTITYLINK
    add constraint ACT_FK_IDL_PROCINST
    foreign key (PROC_INST_ID_)
    references ACT_RU_EXECUTION (ID_);

create index ACT_IDX_TASK_EXEC on ACT_RU_TASK(EXECUTION_ID_);
alter table ACT_RU_TASK
    add constraint ACT_FK_TASK_EXE
    foreign key (EXECUTION_ID_)
    references ACT_RU_EXECUTION (ID_);

create index ACT_IDX_TASK_PROCINST on ACT_RU_TASK(PROC_INST_ID_);
alter table ACT_RU_TASK
    add constraint ACT_FK_TASK_PROCINST
    foreign key (PROC_INST_ID_)
    references ACT_RU_EXECUTION (ID_);

create index ACT_IDX_TASK_PROCDEF on ACT_RU_TASK(PROC_DEF_ID_);
alter table ACT_RU_TASK
  add constraint ACT_FK_TASK_PROCDEF
  foreign key (PROC_DEF_ID_)
  references ACT_RE_PROCDEF (ID_);

create index ACT_IDX_VAR_EXE on ACT_RU_VARIABLE(EXECUTION_ID_);
alter table ACT_RU_VARIABLE
    add constraint ACT_FK_VAR_EXE
    foreign key (EXECUTION_ID_)
    references ACT_RU_EXECUTION (ID_);

create index ACT_IDX_VAR_PROCINST on ACT_RU_VARIABLE(PROC_INST_ID_);
alter table ACT_RU_VARIABLE
    add constraint ACT_FK_VAR_PROCINST
    foreign key (PROC_INST_ID_)
    references ACT_RU_EXECUTION(ID_);

create index ACT_IDX_VAR_BYTEARRAY on ACT_RU_VARIABLE(BYTEARRAY_ID_);
alter table ACT_RU_VARIABLE
    add constraint ACT_FK_VAR_BYTEARRAY
    foreign key (BYTEARRAY_ID_)
    references ACT_GE_BYTEARRAY (ID_);

create index ACT_IDX_JOB_EXCEPTION on ACT_RU_JOB(EXCEPTION_STACK_ID_);
alter table ACT_RU_JOB
    add constraint ACT_FK_JOB_EXCEPTION
    foreign key (EXCEPTION_STACK_ID_)
    references ACT_GE_BYTEARRAY (ID_);

create index ACT_IDX_EVENT_SUBSCR on ACT_RU_EVENT_SUBSCR(EXECUTION_ID_);
alter table ACT_RU_EVENT_SUBSCR
    add constraint ACT_FK_EVENT_EXEC
    foreign key (EXECUTION_ID_)
    references ACT_RU_EXECUTION(ID_);

create index ACT_IDX_MODEL_SOURCE on ACT_RE_MODEL(EDITOR_SOURCE_VALUE_ID_);
alter table ACT_RE_MODEL
    add constraint ACT_FK_MODEL_SOURCE
    foreign key (EDITOR_SOURCE_VALUE_ID_)
    references ACT_GE_BYTEARRAY (ID_);

create index ACT_IDX_MODEL_SOURCE_EXTRA on ACT_RE_MODEL(EDITOR_SOURCE_EXTRA_VALUE_ID_);
alter table ACT_RE_MODEL
    add constraint ACT_FK_MODEL_SOURCE_EXTRA
    foreign key (EDITOR_SOURCE_EXTRA_VALUE_ID_)
    references ACT_GE_BYTEARRAY (ID_);

create index ACT_IDX_MODEL_DEPLOYMENT on ACT_RE_MODEL(DEPLOYMENT_ID_);
alter table ACT_RE_MODEL
    add constraint ACT_FK_MODEL_DEPLOYMENT
    foreign key (DEPLOYMENT_ID_)
    references ACT_RE_DEPLOYMENT (ID_);

create index ACT_IDX_PROCDEF_INFO_JSON on ACT_PROCDEF_INFO(INFO_JSON_ID_);
alter table ACT_PROCDEF_INFO
    add constraint ACT_FK_INFO_JSON_BA
    foreign key (INFO_JSON_ID_)
    references ACT_GE_BYTEARRAY (ID_);

create index ACT_IDX_PROCDEF_INFO_PROC on ACT_PROCDEF_INFO(PROC_DEF_ID_);
alter table ACT_PROCDEF_INFO
    add constraint ACT_FK_INFO_PROCDEF
    foreign key (PROC_DEF_ID_)
    references ACT_RE_PROCDEF (ID_);

alter table ACT_PROCDEF_INFO
    add constraint ACT_UNIQ_INFO_PROCDEF
    unique (PROC_DEF_ID_);

create table ACT_HI_PROCINST (
    ID_ NVARCHAR2(64) not null,
    PROC_INST_ID_ NVARCHAR2(64) not null,
    BUSINESS_KEY_ NVARCHAR2(255),
    PROC_DEF_ID_ NVARCHAR2(64) not null,
    START_TIME_ TIMESTAMP(6) not null,
    END_TIME_ TIMESTAMP(6),
    DURATION_ NUMBER(19,0),
    START_USER_ID_ NVARCHAR2(255),
    START_ACT_ID_ NVARCHAR2(255),
    END_ACT_ID_ NVARCHAR2(255),
    SUPER_PROCESS_INSTANCE_ID_ NVARCHAR2(64),
    DELETE_REASON_ NVARCHAR2(2000),
    TENANT_ID_ NVARCHAR2(255) default '',
    NAME_ NVARCHAR2(255),
    primary key (ID_),
    unique (PROC_INST_ID_)
);

create table ACT_HI_ACTINST (
    ID_ NVARCHAR2(64) not null,
    PROC_DEF_ID_ NVARCHAR2(64) not null,
    PROC_INST_ID_ NVARCHAR2(64) not null,
    EXECUTION_ID_ NVARCHAR2(64) not null,
    ACT_ID_ NVARCHAR2(255) not null,
    TASK_ID_ NVARCHAR2(64),
    CALL_PROC_INST_ID_ NVARCHAR2(64),
    ACT_NAME_ NVARCHAR2(255),
    ACT_TYPE_ NVARCHAR2(255) not null,
    ASSIGNEE_ NVARCHAR2(255),
    START_TIME_ TIMESTAMP(6) not null,
    END_TIME_ TIMESTAMP(6),
    DURATION_ NUMBER(19,0),
    TENANT_ID_ NVARCHAR2(255) default '',
    primary key (ID_)
);

create table ACT_HI_TASKINST (
    ID_ NVARCHAR2(64) not null,
    PROC_DEF_ID_ NVARCHAR2(64),
    TASK_DEF_KEY_ NVARCHAR2(255),
    PROC_INST_ID_ NVARCHAR2(64),
    EXECUTION_ID_ NVARCHAR2(64),
    PARENT_TASK_ID_ NVARCHAR2(64),
    NAME_ NVARCHAR2(255),
    DESCRIPTION_ NVARCHAR2(2000),
    OWNER_ NVARCHAR2(255),
    ASSIGNEE_ NVARCHAR2(255),
    START_TIME_ TIMESTAMP(6) not null,
    CLAIM_TIME_ TIMESTAMP(6),
    END_TIME_ TIMESTAMP(6),
    DURATION_ NUMBER(19,0),
    DELETE_REASON_ NVARCHAR2(2000),
    PRIORITY_ INTEGER,
    DUE_DATE_ TIMESTAMP(6),
    FORM_KEY_ NVARCHAR2(255),
    CATEGORY_ NVARCHAR2(255),
    TENANT_ID_ NVARCHAR2(255) default '',
    primary key (ID_)
);

create table ACT_HI_VARINST (
    ID_ NVARCHAR2(64) not null,
    PROC_INST_ID_ NVARCHAR2(64),
    EXECUTION_ID_ NVARCHAR2(64),
    TASK_ID_ NVARCHAR2(64),
    NAME_ NVARCHAR2(255) not null,
    VAR_TYPE_ NVARCHAR2(100),
    REV_ INTEGER,
    BYTEARRAY_ID_ NVARCHAR2(64),
    DOUBLE_ NUMBER(*,10),
    LONG_ NUMBER(19,0),
    TEXT_ NVARCHAR2(2000),
    TEXT2_ NVARCHAR2(2000),
    CREATE_TIME_ TIMESTAMP(6),
    LAST_UPDATED_TIME_ TIMESTAMP(6),
    primary key (ID_)
);

create table ACT_HI_DETAIL (
    ID_ NVARCHAR2(64) not null,
    TYPE_ NVARCHAR2(255) not null,
    PROC_INST_ID_ NVARCHAR2(64),
    EXECUTION_ID_ NVARCHAR2(64),
    TASK_ID_ NVARCHAR2(64),
    ACT_INST_ID_ NVARCHAR2(64),
    NAME_ NVARCHAR2(255) not null,
    VAR_TYPE_ NVARCHAR2(64),
    REV_ INTEGER,
    TIME_ TIMESTAMP(6) not null,
    BYTEARRAY_ID_ NVARCHAR2(64),
    DOUBLE_ NUMBER(*,10),
    LONG_ NUMBER(19,0),
    TEXT_ NVARCHAR2(2000),
    TEXT2_ NVARCHAR2(2000),
    primary key (ID_)
);

create table ACT_HI_COMMENT (
    ID_ NVARCHAR2(64) not null,
    TYPE_ NVARCHAR2(255),
    TIME_ TIMESTAMP(6) not null,
    USER_ID_ NVARCHAR2(255),
    TASK_ID_ NVARCHAR2(64),
    PROC_INST_ID_ NVARCHAR2(64),
    ACTION_ NVARCHAR2(255),
    MESSAGE_ NVARCHAR2(2000),
    FULL_MSG_ BLOB,
    primary key (ID_)
);

create table ACT_HI_ATTACHMENT (
    ID_ NVARCHAR2(64) not null,
    REV_ INTEGER,
    USER_ID_ NVARCHAR2(255),
    NAME_ NVARCHAR2(255),
    DESCRIPTION_ NVARCHAR2(2000),
    TYPE_ NVARCHAR2(255),
    TASK_ID_ NVARCHAR2(64),
    PROC_INST_ID_ NVARCHAR2(64),
    URL_ NVARCHAR2(2000),
    CONTENT_ID_ NVARCHAR2(64),
    TIME_ TIMESTAMP(6),
    primary key (ID_)
);

create table ACT_HI_IDENTITYLINK (
    ID_ NVARCHAR2(64),
    GROUP_ID_ NVARCHAR2(255),
    TYPE_ NVARCHAR2(255),
    USER_ID_ NVARCHAR2(255),
    TASK_ID_ NVARCHAR2(64),
    PROC_INST_ID_ NVARCHAR2(64),
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
create index ACT_IDX_HI_IDENT_LNK_USER on ACT_HI_IDENTITYLINK(USER_ID_);
create index ACT_IDX_HI_IDENT_LNK_TASK on ACT_HI_IDENTITYLINK(TASK_ID_);
create index ACT_IDX_HI_IDENT_LNK_PROCINST on ACT_HI_IDENTITYLINK(PROC_INST_ID_);

create index ACT_IDX_HI_ACT_INST_PROCINST on ACT_HI_ACTINST(PROC_INST_ID_, ACT_ID_);
create index ACT_IDX_HI_ACT_INST_EXEC on ACT_HI_ACTINST(EXECUTION_ID_, ACT_ID_);
create index ACT_IDX_HI_TASK_INST_PROCINST on ACT_HI_TASKINST(PROC_INST_ID_);

create table ACT_ID_GROUP (
    ID_ NVARCHAR2(64),
    REV_ INTEGER,
    NAME_ NVARCHAR2(255),
    TYPE_ NVARCHAR2(255),
    primary key (ID_)
);

create table ACT_ID_MEMBERSHIP (
    USER_ID_ NVARCHAR2(64),
    GROUP_ID_ NVARCHAR2(64),
    primary key (USER_ID_, GROUP_ID_)
);

create table ACT_ID_USER (
    ID_ NVARCHAR2(64),
    REV_ INTEGER,
    FIRST_ NVARCHAR2(255),
    LAST_ NVARCHAR2(255),
    EMAIL_ NVARCHAR2(255),
    PWD_ NVARCHAR2(255),
    PICTURE_ID_ NVARCHAR2(64),
    primary key (ID_)
);

create table ACT_ID_INFO (
    ID_ NVARCHAR2(64),
    REV_ INTEGER,
    USER_ID_ NVARCHAR2(64),
    TYPE_ NVARCHAR2(64),
    KEY_ NVARCHAR2(255),
    VALUE_ NVARCHAR2(255),
    PASSWORD_ BLOB,
    PARENT_ID_ NVARCHAR2(255),
    primary key (ID_)
);

create index ACT_IDX_MEMB_GROUP on ACT_ID_MEMBERSHIP(GROUP_ID_);
alter table ACT_ID_MEMBERSHIP
    add constraint ACT_FK_MEMB_GROUP
    foreign key (GROUP_ID_)
    references ACT_ID_GROUP (ID_);

create index ACT_IDX_MEMB_USER on ACT_ID_MEMBERSHIP(USER_ID_);
alter table ACT_ID_MEMBERSHIP
    add constraint ACT_FK_MEMB_USER
    foreign key (USER_ID_)
    references ACT_ID_USER (ID_);

commit;
