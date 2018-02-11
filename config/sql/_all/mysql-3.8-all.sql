-- remove iAncestor and iDescendant index, they are the same as FK for that fields

create table m_acc_cert_campaign (definitionRef_relation varchar(157), definitionRef_targetOid varchar(36), definitionRef_type integer, endTimestamp datetime(6), handlerUri varchar(255), name_norm varchar(255), name_orig varchar(255), ownerRef_relation varchar(157), ownerRef_targetOid varchar(36), ownerRef_type integer, stageNumber integer, startTimestamp datetime(6), state integer, oid varchar(36) not null, primary key (oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_acc_cert_case (id integer not null, owner_oid varchar(36) not null, administrativeStatus integer, archiveTimestamp datetime(6), disableReason varchar(255), disableTimestamp datetime(6), effectiveStatus integer, enableTimestamp datetime(6), validFrom datetime(6), validTo datetime(6), validityChangeTimestamp datetime(6), validityStatus integer, currentStageOutcome varchar(255), fullObject longblob, objectRef_relation varchar(157), objectRef_targetOid varchar(36), objectRef_type integer, orgRef_relation varchar(157), orgRef_targetOid varchar(36), orgRef_type integer, outcome varchar(255), remediedTimestamp datetime(6), reviewDeadline datetime(6), reviewRequestedTimestamp datetime(6), stageNumber integer, targetRef_relation varchar(157), targetRef_targetOid varchar(36), targetRef_type integer, tenantRef_relation varchar(157), tenantRef_targetOid varchar(36), tenantRef_type integer, primary key (id, owner_oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_acc_cert_definition (handlerUri varchar(255), lastCampaignClosedTimestamp datetime(6), lastCampaignStartedTimestamp datetime(6), name_norm varchar(255), name_orig varchar(255), ownerRef_relation varchar(157), ownerRef_targetOid varchar(36), ownerRef_type integer, oid varchar(36) not null, primary key (oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_acc_cert_wi (id integer not null, owner_id integer not null, owner_owner_oid varchar(36) not null, closeTimestamp datetime(6), outcome varchar(255), outputChangeTimestamp datetime(6), performerRef_relation varchar(157), performerRef_targetOid varchar(36), performerRef_type integer, stageNumber integer, primary key (id, owner_id, owner_owner_oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_acc_cert_wi_reference (owner_id integer not null, owner_owner_id integer not null, owner_owner_owner_oid varchar(36) not null, relation varchar(157) not null, targetOid varchar(36) not null, targetType integer, primary key (owner_id, owner_owner_id, owner_owner_owner_oid, relation, targetOid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_assignment (id integer not null, owner_oid varchar(36) not null, administrativeStatus integer, archiveTimestamp datetime(6), disableReason varchar(255), disableTimestamp datetime(6), effectiveStatus integer, enableTimestamp datetime(6), validFrom datetime(6), validTo datetime(6), validityChangeTimestamp datetime(6), validityStatus integer, assignmentOwner integer, createChannel varchar(255), createTimestamp datetime(6), creatorRef_relation varchar(157), creatorRef_targetOid varchar(36), creatorRef_type integer, lifecycleState varchar(255), modifierRef_relation varchar(157), modifierRef_targetOid varchar(36), modifierRef_type integer, modifyChannel varchar(255), modifyTimestamp datetime(6), orderValue integer, orgRef_relation varchar(157), orgRef_targetOid varchar(36), orgRef_type integer, resourceRef_relation varchar(157), resourceRef_targetOid varchar(36), resourceRef_type integer, targetRef_relation varchar(157), targetRef_targetOid varchar(36), targetRef_type integer, tenantRef_relation varchar(157), tenantRef_targetOid varchar(36), tenantRef_type integer, extId integer, extOid varchar(36), primary key (id, owner_oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_assignment_ext_boolean (item_id integer not null, anyContainer_owner_id integer not null, anyContainer_owner_owner_oid varchar(36) not null, booleanValue bit not null, primary key (item_id, anyContainer_owner_id, anyContainer_owner_owner_oid, booleanValue)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_assignment_ext_date (item_id integer not null, anyContainer_owner_id integer not null, anyContainer_owner_owner_oid varchar(36) not null, dateValue datetime(6) not null, primary key (item_id, anyContainer_owner_id, anyContainer_owner_owner_oid, dateValue)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_assignment_ext_long (item_id integer not null, anyContainer_owner_id integer not null, anyContainer_owner_owner_oid varchar(36) not null, longValue bigint not null, primary key (item_id, anyContainer_owner_id, anyContainer_owner_owner_oid, longValue)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_assignment_ext_poly (item_id integer not null, anyContainer_owner_id integer not null, anyContainer_owner_owner_oid varchar(36) not null, orig varchar(255) not null, norm varchar(255), primary key (item_id, anyContainer_owner_id, anyContainer_owner_owner_oid, orig)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_assignment_ext_reference (item_id integer not null, anyContainer_owner_id integer not null, anyContainer_owner_owner_oid varchar(36) not null, targetoid varchar(36) not null, relation varchar(157), targetType integer, primary key (item_id, anyContainer_owner_id, anyContainer_owner_owner_oid, targetoid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_assignment_ext_string (item_id integer not null, anyContainer_owner_id integer not null, anyContainer_owner_owner_oid varchar(36) not null, stringValue varchar(255) not null, primary key (item_id, anyContainer_owner_id, anyContainer_owner_owner_oid, stringValue)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_assignment_extension (owner_id integer not null, owner_owner_oid varchar(36) not null, booleansCount smallint, datesCount smallint, longsCount smallint, polysCount smallint, referencesCount smallint, stringsCount smallint, primary key (owner_id, owner_owner_oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_assignment_policy_situation (assignment_id integer not null, assignment_oid varchar(36) not null, policySituation varchar(255)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_assignment_reference (owner_id integer not null, owner_owner_oid varchar(36) not null, reference_type integer not null, relation varchar(157) not null, targetOid varchar(36) not null, targetType integer, primary key (owner_id, owner_owner_oid, reference_type, relation, targetOid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_audit_delta (checksum varchar(32) not null, record_id bigint not null, delta longblob, deltaOid varchar(36), deltaType integer, fullResult longblob, objectName_norm varchar(255), objectName_orig varchar(255), resourceName_norm varchar(255), resourceName_orig varchar(255), resourceOid varchar(36), status integer, primary key (checksum, record_id)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_audit_event (id bigint not null auto_increment, attorneyName varchar(255), attorneyOid varchar(36), channel varchar(255), eventIdentifier varchar(255), eventStage integer, eventType integer, hostIdentifier varchar(255), initiatorName varchar(255), initiatorOid varchar(36), initiatorType integer, message varchar(1024), nodeIdentifier varchar(255), outcome integer, parameter varchar(255), remoteHostAddress varchar(255), result varchar(255), sessionIdentifier varchar(255), targetName varchar(255), targetOid varchar(36), targetOwnerName varchar(255), targetOwnerOid varchar(36), targetType integer, taskIdentifier varchar(255), taskOID varchar(255), timestampValue datetime(6), primary key (id)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_audit_item (changedItemPath varchar(900) not null, record_id bigint not null, primary key (changedItemPath, record_id)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_audit_prop_value (id bigint not null auto_increment, name varchar(255), record_id bigint, value varchar(1024), primary key (id)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_audit_ref_value (id bigint not null auto_increment, name varchar(255), oid varchar(255), record_id bigint, targetName_norm varchar(255), targetName_orig varchar(255), type varchar(255), primary key (id)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_connector_target_system (connector_oid varchar(36) not null, targetSystemType varchar(255)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_ext_item (id integer not null auto_increment, kind integer, itemName varchar(157), itemType varchar(157), primary key (id)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_focus_photo (owner_oid varchar(36) not null, photo longblob, primary key (owner_oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_focus_policy_situation (focus_oid varchar(36) not null, policySituation varchar(255)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_object (oid varchar(36) not null, booleansCount smallint, createChannel varchar(255), createTimestamp datetime(6), creatorRef_relation varchar(157), creatorRef_targetOid varchar(36), creatorRef_type integer, datesCount smallint, fullObject longblob, lifecycleState varchar(255), longsCount smallint, modifierRef_relation varchar(157), modifierRef_targetOid varchar(36), modifierRef_type integer, modifyChannel varchar(255), modifyTimestamp datetime(6), name_norm varchar(255), name_orig varchar(255), objectTypeClass integer, polysCount smallint, referencesCount smallint, stringsCount smallint, tenantRef_relation varchar(157), tenantRef_targetOid varchar(36), tenantRef_type integer, version integer not null, primary key (oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_object_ext_boolean (item_id integer not null, owner_oid varchar(36) not null, ownerType integer not null, booleanValue bit not null, primary key (item_id, owner_oid, ownerType, booleanValue)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_object_ext_date (item_id integer not null, owner_oid varchar(36) not null, ownerType integer not null, dateValue datetime(6) not null, primary key (item_id, owner_oid, ownerType, dateValue)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_object_ext_long (item_id integer not null, owner_oid varchar(36) not null, ownerType integer not null, longValue bigint not null, primary key (item_id, owner_oid, ownerType, longValue)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_object_ext_poly (item_id integer not null, owner_oid varchar(36) not null, ownerType integer not null, orig varchar(255) not null, norm varchar(255), primary key (item_id, owner_oid, ownerType, orig)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_object_ext_reference (item_id integer not null, owner_oid varchar(36) not null, ownerType integer not null, targetoid varchar(36) not null, relation varchar(157), targetType integer, primary key (item_id, owner_oid, ownerType, targetoid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_object_ext_string (item_id integer not null, owner_oid varchar(36) not null, ownerType integer not null, stringValue varchar(255) not null, primary key (item_id, owner_oid, ownerType, stringValue)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_object_text_info (owner_oid varchar(36) not null, text varchar(255) not null, primary key (owner_oid, text)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_operation_execution (id integer not null, owner_oid varchar(36) not null, initiatorRef_relation varchar(157), initiatorRef_targetOid varchar(36), initiatorRef_type integer, status integer, taskRef_relation varchar(157), taskRef_targetOid varchar(36), taskRef_type integer, timestampValue datetime(6), primary key (id, owner_oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_org_closure (ancestor_oid varchar(36) not null, descendant_oid varchar(36) not null, val integer, primary key (ancestor_oid, descendant_oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_org_org_type (org_oid varchar(36) not null, orgType varchar(255)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_reference (owner_oid varchar(36) not null, reference_type integer not null, relation varchar(157) not null, targetOid varchar(36) not null, targetType integer, primary key (owner_oid, reference_type, relation, targetOid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_service_type (service_oid varchar(36) not null, serviceType varchar(255)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_shadow (attemptNumber integer, dead bit, exist bit, failedOperationType integer, fullSynchronizationTimestamp datetime(6), intent varchar(255), kind integer, name_norm varchar(255), name_orig varchar(255), objectClass varchar(157), pendingOperationCount integer, resourceRef_relation varchar(157), resourceRef_targetOid varchar(36), resourceRef_type integer, status integer, synchronizationSituation integer, synchronizationTimestamp datetime(6), oid varchar(36) not null, primary key (oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_task (binding integer, canRunOnNode varchar(255), category varchar(255), completionTimestamp datetime(6), executionStatus integer, handlerUri varchar(255), lastRunFinishTimestamp datetime(6), lastRunStartTimestamp datetime(6), name_norm varchar(255), name_orig varchar(255), node varchar(255), objectRef_relation varchar(157), objectRef_targetOid varchar(36), objectRef_type integer, ownerRef_relation varchar(157), ownerRef_targetOid varchar(36), ownerRef_type integer, parent varchar(255), recurrence integer, status integer, taskIdentifier varchar(255), threadStopAction integer, waitingReason integer, wfEndTimestamp datetime(6), wfObjectRef_relation varchar(157), wfObjectRef_targetOid varchar(36), wfObjectRef_type integer, wfProcessInstanceId varchar(255), wfRequesterRef_relation varchar(157), wfRequesterRef_targetOid varchar(36), wfRequesterRef_type integer, wfStartTimestamp datetime(6), wfTargetRef_relation varchar(157), wfTargetRef_targetOid varchar(36), wfTargetRef_type integer, oid varchar(36) not null, primary key (oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_task_dependent (task_oid varchar(36) not null, dependent varchar(255)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_user_employee_type (user_oid varchar(36) not null, employeeType varchar(255)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_user_organization (user_oid varchar(36) not null, norm varchar(255), orig varchar(255)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_user_organizational_unit (user_oid varchar(36) not null, norm varchar(255), orig varchar(255)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_abstract_role (approvalProcess varchar(255), autoassign_enabled bit, displayName_norm varchar(255), displayName_orig varchar(255), identifier varchar(255), ownerRef_relation varchar(157), ownerRef_targetOid varchar(36), ownerRef_type integer, requestable bit, riskLevel varchar(255), oid varchar(36) not null, primary key (oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_case (name_norm varchar(255), name_orig varchar(255), oid varchar(36) not null, primary key (oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_connector (connectorBundle varchar(255), connectorHostRef_relation varchar(157), connectorHostRef_targetOid varchar(36), connectorHostRef_type integer, connectorType varchar(255), connectorVersion varchar(255), framework varchar(255), name_norm varchar(255), name_orig varchar(255), oid varchar(36) not null, primary key (oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_connector_host (hostname varchar(255), name_norm varchar(255), name_orig varchar(255), port varchar(255), oid varchar(36) not null, primary key (oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_focus (administrativeStatus integer, archiveTimestamp datetime(6), disableReason varchar(255), disableTimestamp datetime(6), effectiveStatus integer, enableTimestamp datetime(6), validFrom datetime(6), validTo datetime(6), validityChangeTimestamp datetime(6), validityStatus integer, hasPhoto bit default false not null, oid varchar(36) not null, primary key (oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_form (name_norm varchar(255), name_orig varchar(255), oid varchar(36) not null, primary key (oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_function_library (name_norm varchar(255), name_orig varchar(255), oid varchar(36) not null, primary key (oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_generic_object (name_norm varchar(255), name_orig varchar(255), objectType varchar(255), oid varchar(36) not null, primary key (oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_lookup_table (name_norm varchar(255), name_orig varchar(255), oid varchar(36) not null, primary key (oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_lookup_table_row (id integer not null, owner_oid varchar(36) not null, row_key varchar(255), label_norm varchar(255), label_orig varchar(255), lastChangeTimestamp datetime(6), row_value varchar(255), primary key (id, owner_oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_node (name_norm varchar(255), name_orig varchar(255), nodeIdentifier varchar(255), oid varchar(36) not null, primary key (oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_object_template (name_norm varchar(255), name_orig varchar(255), type integer, oid varchar(36) not null, primary key (oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_org (costCenter varchar(255), displayOrder integer, locality_norm varchar(255), locality_orig varchar(255), name_norm varchar(255), name_orig varchar(255), tenant bit, oid varchar(36) not null, primary key (oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_report (export integer, name_norm varchar(255), name_orig varchar(255), orientation integer, parent bit, useHibernateSession bit, oid varchar(36) not null, primary key (oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_report_output (name_norm varchar(255), name_orig varchar(255), reportRef_relation varchar(157), reportRef_targetOid varchar(36), reportRef_type integer, oid varchar(36) not null, primary key (oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_resource (administrativeState integer, connectorRef_relation varchar(157), connectorRef_targetOid varchar(36), connectorRef_type integer, name_norm varchar(255), name_orig varchar(255), o16_lastAvailabilityStatus integer, oid varchar(36) not null, primary key (oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_role (name_norm varchar(255), name_orig varchar(255), roleType varchar(255), oid varchar(36) not null, primary key (oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_security_policy (name_norm varchar(255), name_orig varchar(255), oid varchar(36) not null, primary key (oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_sequence (name_norm varchar(255), name_orig varchar(255), oid varchar(36) not null, primary key (oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_service (displayOrder integer, locality_norm varchar(255), locality_orig varchar(255), name_norm varchar(255), name_orig varchar(255), oid varchar(36) not null, primary key (oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_system_configuration (name_norm varchar(255), name_orig varchar(255), oid varchar(36) not null, primary key (oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_trigger (id integer not null, owner_oid varchar(36) not null, handlerUri varchar(255), timestampValue datetime(6), primary key (id, owner_oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_user (additionalName_norm varchar(255), additionalName_orig varchar(255), costCenter varchar(255), emailAddress varchar(255), employeeNumber varchar(255), familyName_norm varchar(255), familyName_orig varchar(255), fullName_norm varchar(255), fullName_orig varchar(255), givenName_norm varchar(255), givenName_orig varchar(255), honorificPrefix_norm varchar(255), honorificPrefix_orig varchar(255), honorificSuffix_norm varchar(255), honorificSuffix_orig varchar(255), locale varchar(255), locality_norm varchar(255), locality_orig varchar(255), name_norm varchar(255), name_orig varchar(255), nickName_norm varchar(255), nickName_orig varchar(255), preferredLanguage varchar(255), status integer, telephoneNumber varchar(255), timezone varchar(255), title_norm varchar(255), title_orig varchar(255), oid varchar(36) not null, primary key (oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
create table m_value_policy (name_norm varchar(255), name_orig varchar(255), oid varchar(36) not null, primary key (oid)) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin engine=InnoDB;
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
create index iOpExecTaskOid on m_operation_execution (taskRef_targetOid);
create index iOpExecInitiatorOid on m_operation_execution (initiatorRef_targetOid);
create index iOpExecStatus on m_operation_execution (status);
create index iOpExecOwnerOid on m_operation_execution (owner_oid);
-- create index iAncestor on m_org_closure (ancestor_oid);
-- create index iDescendant on m_org_closure (descendant_oid);
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
alter table m_acc_cert_campaign add constraint fk_acc_cert_campaign foreign key (oid) references m_object (oid);
alter table m_acc_cert_case add constraint fk_acc_cert_case_owner foreign key (owner_oid) references m_acc_cert_campaign (oid);
alter table m_acc_cert_definition add constraint fk_acc_cert_definition foreign key (oid) references m_object (oid);
alter table m_acc_cert_wi add constraint fk_acc_cert_wi_owner foreign key (owner_id, owner_owner_oid) references m_acc_cert_case (id, owner_oid);
alter table m_acc_cert_wi_reference add constraint fk_acc_cert_wi_ref_owner foreign key (owner_id, owner_owner_id, owner_owner_owner_oid) references m_acc_cert_wi (id, owner_id, owner_owner_oid);
alter table m_assignment add constraint fk_assignment_owner foreign key (owner_oid) references m_object (oid);
alter table m_assignment_ext_boolean add constraint fk_a_ext_boolean_owner foreign key (anyContainer_owner_id, anyContainer_owner_owner_oid) references m_assignment_extension (owner_id, owner_owner_oid);
alter table m_assignment_ext_boolean add constraint fk_a_ext_boolean_item foreign key (item_id) references m_ext_item (id);
alter table m_assignment_ext_date add constraint fk_a_ext_date_owner foreign key (anyContainer_owner_id, anyContainer_owner_owner_oid) references m_assignment_extension (owner_id, owner_owner_oid);
alter table m_assignment_ext_date add constraint fk_a_ext_date_item foreign key (item_id) references m_ext_item (id);
alter table m_assignment_ext_long add constraint fk_a_ext_long_owner foreign key (anyContainer_owner_id, anyContainer_owner_owner_oid) references m_assignment_extension (owner_id, owner_owner_oid);
alter table m_assignment_ext_long add constraint fk_a_ext_long_item foreign key (item_id) references m_ext_item (id);
alter table m_assignment_ext_poly add constraint fk_a_ext_poly_owner foreign key (anyContainer_owner_id, anyContainer_owner_owner_oid) references m_assignment_extension (owner_id, owner_owner_oid);
alter table m_assignment_ext_poly add constraint fk_a_ext_poly_item foreign key (item_id) references m_ext_item (id);
alter table m_assignment_ext_reference add constraint fk_a_ext_reference_owner foreign key (anyContainer_owner_id, anyContainer_owner_owner_oid) references m_assignment_extension (owner_id, owner_owner_oid);
alter table m_assignment_ext_reference add constraint fk_a_ext_boolean_reference foreign key (item_id) references m_ext_item (id);
alter table m_assignment_ext_string add constraint fk_a_ext_string_owner foreign key (anyContainer_owner_id, anyContainer_owner_owner_oid) references m_assignment_extension (owner_id, owner_owner_oid);
alter table m_assignment_ext_string add constraint fk_a_ext_string_item foreign key (item_id) references m_ext_item (id);
alter table m_assignment_policy_situation add constraint fk_assignment_policy_situation foreign key (assignment_id, assignment_oid) references m_assignment (id, owner_oid);
alter table m_assignment_reference add constraint fk_assignment_reference foreign key (owner_id, owner_owner_oid) references m_assignment (id, owner_oid);
alter table m_audit_delta add constraint fk_audit_delta foreign key (record_id) references m_audit_event (id);
alter table m_audit_item add constraint fk_audit_item foreign key (record_id) references m_audit_event (id);
alter table m_audit_prop_value add constraint fk_audit_prop_value foreign key (record_id) references m_audit_event (id);
alter table m_audit_ref_value add constraint fk_audit_ref_value foreign key (record_id) references m_audit_event (id);
alter table m_connector_target_system add constraint fk_connector_target_system foreign key (connector_oid) references m_connector (oid);
alter table m_focus_photo add constraint fk_focus_photo foreign key (owner_oid) references m_focus (oid);
alter table m_focus_policy_situation add constraint fk_focus_policy_situation foreign key (focus_oid) references m_focus (oid);
alter table m_object_ext_boolean add constraint fk_o_ext_boolean_owner foreign key (owner_oid) references m_object (oid);
alter table m_object_ext_boolean add constraint fk_o_ext_boolean_item foreign key (item_id) references m_ext_item (id);
alter table m_object_ext_date add constraint fk_o_ext_date_owner foreign key (owner_oid) references m_object (oid);
alter table m_object_ext_date add constraint fk_o_ext_date_item foreign key (item_id) references m_ext_item (id);
alter table m_object_ext_long add constraint fk_object_ext_long foreign key (owner_oid) references m_object (oid);
alter table m_object_ext_long add constraint fk_o_ext_long_item foreign key (item_id) references m_ext_item (id);
alter table m_object_ext_poly add constraint fk_o_ext_poly_owner foreign key (owner_oid) references m_object (oid);
alter table m_object_ext_poly add constraint fk_o_ext_poly_item foreign key (item_id) references m_ext_item (id);
alter table m_object_ext_reference add constraint fk_o_ext_reference_owner foreign key (owner_oid) references m_object (oid);
alter table m_object_ext_reference add constraint fk_o_ext_reference_item foreign key (item_id) references m_ext_item (id);
alter table m_object_ext_string add constraint fk_object_ext_string foreign key (owner_oid) references m_object (oid);
alter table m_object_ext_string add constraint fk_o_ext_string_item foreign key (item_id) references m_ext_item (id);
alter table m_object_text_info add constraint fk_object_text_info_owner foreign key (owner_oid) references m_object (oid);
alter table m_operation_execution add constraint fk_op_exec_owner foreign key (owner_oid) references m_object (oid);
alter table m_org_closure add constraint fk_ancestor foreign key (ancestor_oid) references m_object (oid);
alter table m_org_closure add constraint fk_descendant foreign key (descendant_oid) references m_object (oid);
alter table m_org_org_type add constraint fk_org_org_type foreign key (org_oid) references m_org (oid);
alter table m_reference add constraint fk_reference_owner foreign key (owner_oid) references m_object (oid);
alter table m_service_type add constraint fk_service_type foreign key (service_oid) references m_service (oid);
alter table m_shadow add constraint fk_shadow foreign key (oid) references m_object (oid);
alter table m_task add constraint fk_task foreign key (oid) references m_object (oid);
alter table m_task_dependent add constraint fk_task_dependent foreign key (task_oid) references m_task (oid);
alter table m_user_employee_type add constraint fk_user_employee_type foreign key (user_oid) references m_user (oid);
alter table m_user_organization add constraint fk_user_organization foreign key (user_oid) references m_user (oid);
alter table m_user_organizational_unit add constraint fk_user_org_unit foreign key (user_oid) references m_user (oid);
alter table m_abstract_role add constraint fk_abstract_role foreign key (oid) references m_focus (oid);
alter table m_case add constraint fk_case foreign key (oid) references m_object (oid);
alter table m_connector add constraint fk_connector foreign key (oid) references m_object (oid);
alter table m_connector_host add constraint fk_connector_host foreign key (oid) references m_object (oid);
alter table m_focus add constraint fk_focus foreign key (oid) references m_object (oid);
alter table m_form add constraint fk_form foreign key (oid) references m_object (oid);
alter table m_function_library add constraint fk_function_library foreign key (oid) references m_object (oid);
alter table m_generic_object add constraint fk_generic_object foreign key (oid) references m_object (oid);
alter table m_lookup_table add constraint fk_lookup_table foreign key (oid) references m_object (oid);
alter table m_lookup_table_row add constraint fk_lookup_table_owner foreign key (owner_oid) references m_lookup_table (oid);
alter table m_node add constraint fk_node foreign key (oid) references m_object (oid);
alter table m_object_template add constraint fk_object_template foreign key (oid) references m_object (oid);
alter table m_org add constraint fk_org foreign key (oid) references m_abstract_role (oid);
alter table m_report add constraint fk_report foreign key (oid) references m_object (oid);
alter table m_report_output add constraint fk_report_output foreign key (oid) references m_object (oid);
alter table m_resource add constraint fk_resource foreign key (oid) references m_object (oid);
alter table m_role add constraint fk_role foreign key (oid) references m_abstract_role (oid);
alter table m_security_policy add constraint fk_security_policy foreign key (oid) references m_object (oid);
alter table m_sequence add constraint fk_sequence foreign key (oid) references m_object (oid);
alter table m_service add constraint fk_service foreign key (oid) references m_abstract_role (oid);
alter table m_system_configuration add constraint fk_system_configuration foreign key (oid) references m_object (oid);
alter table m_trigger add constraint fk_trigger_owner foreign key (owner_oid) references m_object (oid);
alter table m_user add constraint fk_user foreign key (oid) references m_focus (oid);
alter table m_value_policy add constraint fk_value_policy foreign key (oid) references m_object (oid);


# By: Ron Cordell - roncordell
#  I didn't see this anywhere, so I thought I'd post it here. This is the script from Quartz to create the tables in a MySQL database, modified to use INNODB instead of MYISAM.

DROP TABLE IF EXISTS QRTZ_FIRED_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_PAUSED_TRIGGER_GRPS;
DROP TABLE IF EXISTS QRTZ_SCHEDULER_STATE;
DROP TABLE IF EXISTS QRTZ_LOCKS;
DROP TABLE IF EXISTS QRTZ_SIMPLE_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_SIMPROP_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_CRON_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_BLOB_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_JOB_DETAILS;
DROP TABLE IF EXISTS QRTZ_CALENDARS;

CREATE TABLE QRTZ_JOB_DETAILS(
SCHED_NAME VARCHAR(120) NOT NULL,
JOB_NAME VARCHAR(200) NOT NULL,
JOB_GROUP VARCHAR(200) NOT NULL,
DESCRIPTION VARCHAR(250) NULL,
JOB_CLASS_NAME VARCHAR(250) NOT NULL,
IS_DURABLE VARCHAR(1) NOT NULL,
IS_NONCONCURRENT VARCHAR(1) NOT NULL,
IS_UPDATE_DATA VARCHAR(1) NOT NULL,
REQUESTS_RECOVERY VARCHAR(1) NOT NULL,
JOB_DATA BLOB NULL,
PRIMARY KEY (SCHED_NAME,JOB_NAME,JOB_GROUP))
ENGINE=InnoDB;

CREATE TABLE QRTZ_TRIGGERS (
SCHED_NAME VARCHAR(120) NOT NULL,
TRIGGER_NAME VARCHAR(200) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
JOB_NAME VARCHAR(200) NOT NULL,
JOB_GROUP VARCHAR(200) NOT NULL,
DESCRIPTION VARCHAR(250) NULL,
NEXT_FIRE_TIME BIGINT(13) NULL,
PREV_FIRE_TIME BIGINT(13) NULL,
PRIORITY INTEGER NULL,
EXECUTION_GROUP VARCHAR(200) NULL,
TRIGGER_STATE VARCHAR(16) NOT NULL,
TRIGGER_TYPE VARCHAR(8) NOT NULL,
START_TIME BIGINT(13) NOT NULL,
END_TIME BIGINT(13) NULL,
CALENDAR_NAME VARCHAR(200) NULL,
MISFIRE_INSTR SMALLINT(2) NULL,
JOB_DATA BLOB NULL,
PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
FOREIGN KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
REFERENCES QRTZ_JOB_DETAILS(SCHED_NAME,JOB_NAME,JOB_GROUP))
ENGINE=InnoDB;

CREATE TABLE QRTZ_SIMPLE_TRIGGERS (
SCHED_NAME VARCHAR(120) NOT NULL,
TRIGGER_NAME VARCHAR(200) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
REPEAT_COUNT BIGINT(7) NOT NULL,
REPEAT_INTERVAL BIGINT(12) NOT NULL,
TIMES_TRIGGERED BIGINT(10) NOT NULL,
PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
ENGINE=InnoDB;

CREATE TABLE QRTZ_CRON_TRIGGERS (
SCHED_NAME VARCHAR(120) NOT NULL,
TRIGGER_NAME VARCHAR(200) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
CRON_EXPRESSION VARCHAR(120) NOT NULL,
TIME_ZONE_ID VARCHAR(80),
PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
ENGINE=InnoDB;

CREATE TABLE QRTZ_SIMPROP_TRIGGERS
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    STR_PROP_1 VARCHAR(512) NULL,
    STR_PROP_2 VARCHAR(512) NULL,
    STR_PROP_3 VARCHAR(512) NULL,
    INT_PROP_1 INT NULL,
    INT_PROP_2 INT NULL,
    LONG_PROP_1 BIGINT NULL,
    LONG_PROP_2 BIGINT NULL,
    DEC_PROP_1 NUMERIC(13,4) NULL,
    DEC_PROP_2 NUMERIC(13,4) NULL,
    BOOL_PROP_1 VARCHAR(1) NULL,
    BOOL_PROP_2 VARCHAR(1) NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
    REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
ENGINE=InnoDB;

CREATE TABLE QRTZ_BLOB_TRIGGERS (
SCHED_NAME VARCHAR(120) NOT NULL,
TRIGGER_NAME VARCHAR(200) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
BLOB_DATA BLOB NULL,
PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
INDEX (SCHED_NAME,TRIGGER_NAME, TRIGGER_GROUP),
FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
ENGINE=InnoDB;

CREATE TABLE QRTZ_CALENDARS (
SCHED_NAME VARCHAR(120) NOT NULL,
CALENDAR_NAME VARCHAR(200) NOT NULL,
CALENDAR BLOB NOT NULL,
PRIMARY KEY (SCHED_NAME,CALENDAR_NAME))
ENGINE=InnoDB;

CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS (
SCHED_NAME VARCHAR(120) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
PRIMARY KEY (SCHED_NAME,TRIGGER_GROUP))
ENGINE=InnoDB;

CREATE TABLE QRTZ_FIRED_TRIGGERS (
SCHED_NAME VARCHAR(120) NOT NULL,
ENTRY_ID VARCHAR(95) NOT NULL,
TRIGGER_NAME VARCHAR(200) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
INSTANCE_NAME VARCHAR(200) NOT NULL,
FIRED_TIME BIGINT(13) NOT NULL,
SCHED_TIME BIGINT(13) NOT NULL,
PRIORITY INTEGER NOT NULL,
EXECUTION_GROUP VARCHAR(200) NULL,
STATE VARCHAR(16) NOT NULL,
JOB_NAME VARCHAR(200) NULL,
JOB_GROUP VARCHAR(200) NULL,
IS_NONCONCURRENT VARCHAR(1) NULL,
REQUESTS_RECOVERY VARCHAR(1) NULL,
PRIMARY KEY (SCHED_NAME,ENTRY_ID))
ENGINE=InnoDB;

CREATE TABLE QRTZ_SCHEDULER_STATE (
SCHED_NAME VARCHAR(120) NOT NULL,
INSTANCE_NAME VARCHAR(200) NOT NULL,
LAST_CHECKIN_TIME BIGINT(13) NOT NULL,
CHECKIN_INTERVAL BIGINT(13) NOT NULL,
PRIMARY KEY (SCHED_NAME,INSTANCE_NAME))
ENGINE=InnoDB;

CREATE TABLE QRTZ_LOCKS (
SCHED_NAME VARCHAR(120) NOT NULL,
LOCK_NAME VARCHAR(40) NOT NULL,
PRIMARY KEY (SCHED_NAME,LOCK_NAME))
ENGINE=InnoDB;

CREATE INDEX IDX_QRTZ_J_REQ_RECOVERY ON QRTZ_JOB_DETAILS(SCHED_NAME,REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_J_GRP ON QRTZ_JOB_DETAILS(SCHED_NAME,JOB_GROUP);

CREATE INDEX IDX_QRTZ_T_J ON QRTZ_TRIGGERS(SCHED_NAME,JOB_NAME,JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_JG ON QRTZ_TRIGGERS(SCHED_NAME,JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_C ON QRTZ_TRIGGERS(SCHED_NAME,CALENDAR_NAME);
CREATE INDEX IDX_QRTZ_T_G ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_T_STATE ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_STATE ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP,TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_G_STATE ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_GROUP,TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NEXT_FIRE_TIME ON QRTZ_TRIGGERS(SCHED_NAME,NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_STATE,NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_MISFIRE ON QRTZ_TRIGGERS(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE ON QRTZ_TRIGGERS(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE_GRP ON QRTZ_TRIGGERS(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_GROUP,TRIGGER_STATE);

CREATE INDEX IDX_QRTZ_FT_TRIG_INST_NAME ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,INSTANCE_NAME);
CREATE INDEX IDX_QRTZ_FT_INST_JOB_REQ_RCVRY ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,INSTANCE_NAME,REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_FT_J_G ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,JOB_NAME,JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_JG ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_T_G ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_FT_TG ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,TRIGGER_GROUP);

create table ACT_GE_PROPERTY (
    NAME_ varchar(64),
    VALUE_ varchar(300),
    REV_ integer,
    primary key (NAME_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

insert into ACT_GE_PROPERTY
values ('schema.version', '5.22.0.0', 1);

insert into ACT_GE_PROPERTY
values ('schema.history', 'create(5.22.0.0)', 1);

insert into ACT_GE_PROPERTY
values ('next.dbid', '1', 1);

create table ACT_GE_BYTEARRAY (
    ID_ varchar(64),
    REV_ integer,
    NAME_ varchar(255),
    DEPLOYMENT_ID_ varchar(64),
    BYTES_ LONGBLOB,
    GENERATED_ TINYINT,
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_RE_DEPLOYMENT (
    ID_ varchar(64),
    NAME_ varchar(255),
    CATEGORY_ varchar(255),
    TENANT_ID_ varchar(255) default '',
    DEPLOY_TIME_ timestamp(3) NULL,
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_RE_MODEL (
    ID_ varchar(64) not null,
    REV_ integer,
    NAME_ varchar(255),
    KEY_ varchar(255),
    CATEGORY_ varchar(255),
    CREATE_TIME_ timestamp(3) null,
    LAST_UPDATE_TIME_ timestamp(3) null,
    VERSION_ integer,
    META_INFO_ varchar(4000),
    DEPLOYMENT_ID_ varchar(64),
    EDITOR_SOURCE_VALUE_ID_ varchar(64),
    EDITOR_SOURCE_EXTRA_VALUE_ID_ varchar(64),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_RU_EXECUTION (
    ID_ varchar(64),
    REV_ integer,
    PROC_INST_ID_ varchar(64),
    BUSINESS_KEY_ varchar(255),
    PARENT_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    SUPER_EXEC_ varchar(64),
    ACT_ID_ varchar(255),
    IS_ACTIVE_ TINYINT,
    IS_CONCURRENT_ TINYINT,
    IS_SCOPE_ TINYINT,
    IS_EVENT_SCOPE_ TINYINT,
    SUSPENSION_STATE_ integer,
    CACHED_ENT_STATE_ integer,
    TENANT_ID_ varchar(255) default '',
    NAME_ varchar(255),
    LOCK_TIME_ timestamp(3) NULL,
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_RU_JOB (
    ID_ varchar(64) NOT NULL,
    REV_ integer,
    TYPE_ varchar(255) NOT NULL,
    LOCK_EXP_TIME_ timestamp(3) NULL,
    LOCK_OWNER_ varchar(255),
    EXCLUSIVE_ boolean,
    EXECUTION_ID_ varchar(64),
    PROCESS_INSTANCE_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    RETRIES_ integer,
    EXCEPTION_STACK_ID_ varchar(64),
    EXCEPTION_MSG_ varchar(4000),
    DUEDATE_ timestamp(3) NULL,
    REPEAT_ varchar(255),
    HANDLER_TYPE_ varchar(255),
    HANDLER_CFG_ varchar(4000),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_RE_PROCDEF (
    ID_ varchar(64) not null,
    REV_ integer,
    CATEGORY_ varchar(255),
    NAME_ varchar(255),
    KEY_ varchar(255) not null,
    VERSION_ integer not null,
    DEPLOYMENT_ID_ varchar(64),
    RESOURCE_NAME_ varchar(4000),
    DGRM_RESOURCE_NAME_ varchar(4000),
    DESCRIPTION_ varchar(4000),
    HAS_START_FORM_KEY_ TINYINT,
    HAS_GRAPHICAL_NOTATION_ TINYINT,
    SUSPENSION_STATE_ integer,
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_RU_TASK (
    ID_ varchar(64),
    REV_ integer,
    EXECUTION_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    NAME_ varchar(255),
    PARENT_TASK_ID_ varchar(64),
    DESCRIPTION_ varchar(4000),
    TASK_DEF_KEY_ varchar(255),
    OWNER_ varchar(255),
    ASSIGNEE_ varchar(255),
    DELEGATION_ varchar(64),
    PRIORITY_ integer,
    CREATE_TIME_ timestamp(3) NULL,
    DUE_DATE_ datetime(3),
    CATEGORY_ varchar(255),
    SUSPENSION_STATE_ integer,
    TENANT_ID_ varchar(255) default '',
    FORM_KEY_ varchar(255),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_RU_IDENTITYLINK (
    ID_ varchar(64),
    REV_ integer,
    GROUP_ID_ varchar(255),
    TYPE_ varchar(255),
    USER_ID_ varchar(255),
    TASK_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_RU_VARIABLE (
    ID_ varchar(64) not null,
    REV_ integer,
    TYPE_ varchar(255) not null,
    NAME_ varchar(255) not null,
    EXECUTION_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    TASK_ID_ varchar(64),
    BYTEARRAY_ID_ varchar(64),
    DOUBLE_ double,
    LONG_ bigint,
    TEXT_ varchar(4000),
    TEXT2_ varchar(4000),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_RU_EVENT_SUBSCR (
    ID_ varchar(64) not null,
    REV_ integer,
    EVENT_TYPE_ varchar(255) not null,
    EVENT_NAME_ varchar(255),
    EXECUTION_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    ACTIVITY_ID_ varchar(64),
    CONFIGURATION_ varchar(255),
    CREATED_ timestamp(3) not null DEFAULT CURRENT_TIMESTAMP(3),
    PROC_DEF_ID_ varchar(64),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_EVT_LOG (
    LOG_NR_ bigint auto_increment,
    TYPE_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    EXECUTION_ID_ varchar(64),
    TASK_ID_ varchar(64),
    TIME_STAMP_ timestamp(3) not null,
    USER_ID_ varchar(255),
    DATA_ LONGBLOB,
    LOCK_OWNER_ varchar(255),
    LOCK_TIME_ timestamp(3) null,
    IS_PROCESSED_ tinyint default 0,
    primary key (LOG_NR_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_PROCDEF_INFO (
	ID_ varchar(64) not null,
    PROC_DEF_ID_ varchar(64) not null,
    REV_ integer,
    INFO_JSON_ID_ varchar(64),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create index ACT_IDX_EXEC_BUSKEY on ACT_RU_EXECUTION(BUSINESS_KEY_);
create index ACT_IDX_TASK_CREATE on ACT_RU_TASK(CREATE_TIME_);
create index ACT_IDX_IDENT_LNK_USER on ACT_RU_IDENTITYLINK(USER_ID_);
create index ACT_IDX_IDENT_LNK_GROUP on ACT_RU_IDENTITYLINK(GROUP_ID_);
create index ACT_IDX_EVENT_SUBSCR_CONFIG_ on ACT_RU_EVENT_SUBSCR(CONFIGURATION_);
create index ACT_IDX_VARIABLE_TASK_ID on ACT_RU_VARIABLE(TASK_ID_);
create index ACT_IDX_ATHRZ_PROCEDEF on ACT_RU_IDENTITYLINK(PROC_DEF_ID_);
create index ACT_IDX_INFO_PROCDEF on ACT_PROCDEF_INFO(PROC_DEF_ID_);

alter table ACT_GE_BYTEARRAY
    add constraint ACT_FK_BYTEARR_DEPL
    foreign key (DEPLOYMENT_ID_)
    references ACT_RE_DEPLOYMENT (ID_);

alter table ACT_RE_PROCDEF
    add constraint ACT_UNIQ_PROCDEF
    unique (KEY_,VERSION_, TENANT_ID_);

alter table ACT_RU_EXECUTION
    add constraint ACT_FK_EXE_PROCINST
    foreign key (PROC_INST_ID_)
    references ACT_RU_EXECUTION (ID_) on delete cascade on update cascade;

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
    references ACT_RE_PROCDEF(ID_);

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
    ID_ varchar(64) not null,
    PROC_INST_ID_ varchar(64) not null,
    BUSINESS_KEY_ varchar(255),
    PROC_DEF_ID_ varchar(64) not null,
    START_TIME_ datetime(3) not null,
    END_TIME_ datetime(3),
    DURATION_ bigint,
    START_USER_ID_ varchar(255),
    START_ACT_ID_ varchar(255),
    END_ACT_ID_ varchar(255),
    SUPER_PROCESS_INSTANCE_ID_ varchar(64),
    DELETE_REASON_ varchar(4000),
    TENANT_ID_ varchar(255) default '',
    NAME_ varchar(255),
    primary key (ID_),
    unique (PROC_INST_ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_HI_ACTINST (
    ID_ varchar(64) not null,
    PROC_DEF_ID_ varchar(64) not null,
    PROC_INST_ID_ varchar(64) not null,
    EXECUTION_ID_ varchar(64) not null,
    ACT_ID_ varchar(255) not null,
    TASK_ID_ varchar(64),
    CALL_PROC_INST_ID_ varchar(64),
    ACT_NAME_ varchar(255),
    ACT_TYPE_ varchar(255) not null,
    ASSIGNEE_ varchar(255),
    START_TIME_ datetime(3) not null,
    END_TIME_ datetime(3),
    DURATION_ bigint,
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_HI_TASKINST (
    ID_ varchar(64) not null,
    PROC_DEF_ID_ varchar(64),
    TASK_DEF_KEY_ varchar(255),
    PROC_INST_ID_ varchar(64),
    EXECUTION_ID_ varchar(64),
    NAME_ varchar(255),
    PARENT_TASK_ID_ varchar(64),
    DESCRIPTION_ varchar(4000),
    OWNER_ varchar(255),
    ASSIGNEE_ varchar(255),
    START_TIME_ datetime(3) not null,
    CLAIM_TIME_ datetime(3),
    END_TIME_ datetime(3),
    DURATION_ bigint,
    DELETE_REASON_ varchar(4000),
    PRIORITY_ integer,
    DUE_DATE_ datetime(3),
    FORM_KEY_ varchar(255),
    CATEGORY_ varchar(255),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_HI_VARINST (
    ID_ varchar(64) not null,
    PROC_INST_ID_ varchar(64),
    EXECUTION_ID_ varchar(64),
    TASK_ID_ varchar(64),
    NAME_ varchar(255) not null,
    VAR_TYPE_ varchar(100),
    REV_ integer,
    BYTEARRAY_ID_ varchar(64),
    DOUBLE_ double,
    LONG_ bigint,
    TEXT_ varchar(4000),
    TEXT2_ varchar(4000),
    CREATE_TIME_ datetime(3),
    LAST_UPDATED_TIME_ datetime(3),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_HI_DETAIL (
    ID_ varchar(64) not null,
    TYPE_ varchar(255) not null,
    PROC_INST_ID_ varchar(64),
    EXECUTION_ID_ varchar(64),
    TASK_ID_ varchar(64),
    ACT_INST_ID_ varchar(64),
    NAME_ varchar(255) not null,
    VAR_TYPE_ varchar(255),
    REV_ integer,
    TIME_ datetime(3) not null,
    BYTEARRAY_ID_ varchar(64),
    DOUBLE_ double,
    LONG_ bigint,
    TEXT_ varchar(4000),
    TEXT2_ varchar(4000),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_HI_COMMENT (
    ID_ varchar(64) not null,
    TYPE_ varchar(255),
    TIME_ datetime(3) not null,
    USER_ID_ varchar(255),
    TASK_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    ACTION_ varchar(255),
    MESSAGE_ varchar(4000),
    FULL_MSG_ LONGBLOB,
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_HI_ATTACHMENT (
    ID_ varchar(64) not null,
    REV_ integer,
    USER_ID_ varchar(255),
    NAME_ varchar(255),
    DESCRIPTION_ varchar(4000),
    TYPE_ varchar(255),
    TASK_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    URL_ varchar(4000),
    CONTENT_ID_ varchar(64),
    TIME_ datetime(3),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_HI_IDENTITYLINK (
    ID_ varchar(64),
    GROUP_ID_ varchar(255),
    TYPE_ varchar(255),
    USER_ID_ varchar(255),
    TASK_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;


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
    ID_ varchar(64),
    REV_ integer,
    NAME_ varchar(255),
    TYPE_ varchar(255),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_ID_MEMBERSHIP (
    USER_ID_ varchar(64),
    GROUP_ID_ varchar(64),
    primary key (USER_ID_, GROUP_ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_ID_USER (
    ID_ varchar(64),
    REV_ integer,
    FIRST_ varchar(255),
    LAST_ varchar(255),
    EMAIL_ varchar(255),
    PWD_ varchar(255),
    PICTURE_ID_ varchar(64),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_ID_INFO (
    ID_ varchar(64),
    REV_ integer,
    USER_ID_ varchar(64),
    TYPE_ varchar(64),
    KEY_ varchar(255),
    VALUE_ varchar(255),
    PASSWORD_ LONGBLOB,
    PARENT_ID_ varchar(255),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

alter table ACT_ID_MEMBERSHIP
    add constraint ACT_FK_MEMB_GROUP
    foreign key (GROUP_ID_)
    references ACT_ID_GROUP (ID_);

alter table ACT_ID_MEMBERSHIP
    add constraint ACT_FK_MEMB_USER
    foreign key (USER_ID_)
    references ACT_ID_USER (ID_);

commit;
