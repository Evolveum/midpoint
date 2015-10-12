
    create table m_abstract_role (
        approvalProcess varchar(255),
        requestable bit,
        oid varchar(36) not null,
        primary key (oid)
    ) ENGINE=InnoDB;

    create table m_acc_cert_campaign (
        definitionRef_relation varchar(157),
        definitionRef_targetOid varchar(36),
        definitionRef_type integer,
        name_norm varchar(255),
        name_orig varchar(255),
        oid varchar(36) not null,
        primary key (oid)
    ) ENGINE=InnoDB;

    create table m_acc_cert_definition (
        name_norm varchar(255),
        name_orig varchar(255),
        oid varchar(36) not null,
        primary key (oid)
    ) ENGINE=InnoDB;

    create table m_assignment (
        id integer not null,
        owner_oid varchar(36) not null,
        administrativeStatus integer,
        archiveTimestamp datetime,
        disableReason varchar(255),
        disableTimestamp datetime,
        effectiveStatus integer,
        enableTimestamp datetime,
        validFrom datetime,
        validTo datetime,
        validityChangeTimestamp datetime,
        validityStatus integer,
        assignmentOwner integer,
        createChannel varchar(255),
        createTimestamp datetime,
        creatorRef_relation varchar(157),
        creatorRef_targetOid varchar(36),
        creatorRef_type integer,
        modifierRef_relation varchar(157),
        modifierRef_targetOid varchar(36),
        modifierRef_type integer,
        modifyChannel varchar(255),
        modifyTimestamp datetime,
        orderValue integer,
        orgRef_relation varchar(157),
        orgRef_targetOid varchar(36),
        orgRef_type integer,
        targetRef_relation varchar(157),
        targetRef_targetOid varchar(36),
        targetRef_type integer,
        tenantRef_relation varchar(157),
        tenantRef_targetOid varchar(36),
        tenantRef_type integer,
        extId integer,
        extOid varchar(36),
        primary key (id, owner_oid)
    ) ENGINE=InnoDB;

    create table m_assignment_ext_boolean (
        eName varchar(157) not null,
        anyContainer_owner_id integer not null,
        anyContainer_owner_owner_oid varchar(36) not null,
        booleanValue bit not null,
        extensionType integer,
        dynamicDef bit,
        eType varchar(157),
        valueType integer,
        primary key (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, booleanValue)
    ) ENGINE=InnoDB;

    create table m_assignment_ext_date (
        eName varchar(157) not null,
        anyContainer_owner_id integer not null,
        anyContainer_owner_owner_oid varchar(36) not null,
        dateValue datetime not null,
        extensionType integer,
        dynamicDef bit,
        eType varchar(157),
        valueType integer,
        primary key (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, dateValue)
    ) ENGINE=InnoDB;

    create table m_assignment_ext_long (
        eName varchar(157) not null,
        anyContainer_owner_id integer not null,
        anyContainer_owner_owner_oid varchar(36) not null,
        longValue bigint not null,
        extensionType integer,
        dynamicDef bit,
        eType varchar(157),
        valueType integer,
        primary key (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, longValue)
    ) ENGINE=InnoDB;

    create table m_assignment_ext_poly (
        eName varchar(157) not null,
        anyContainer_owner_id integer not null,
        anyContainer_owner_owner_oid varchar(36) not null,
        orig varchar(255) not null,
        extensionType integer,
        dynamicDef bit,
        norm varchar(255),
        eType varchar(157),
        valueType integer,
        primary key (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, orig)
    ) ENGINE=InnoDB;

    create table m_assignment_ext_reference (
        eName varchar(157) not null,
        anyContainer_owner_id integer not null,
        anyContainer_owner_owner_oid varchar(36) not null,
        targetoid varchar(36) not null,
        extensionType integer,
        dynamicDef bit,
        relation varchar(157),
        targetType integer,
        eType varchar(157),
        valueType integer,
        primary key (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, targetoid)
    ) ENGINE=InnoDB;

    create table m_assignment_ext_string (
        eName varchar(157) not null,
        anyContainer_owner_id integer not null,
        anyContainer_owner_owner_oid varchar(36) not null,
        stringValue varchar(255) not null,
        extensionType integer,
        dynamicDef bit,
        eType varchar(157),
        valueType integer,
        primary key (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, stringValue)
    ) ENGINE=InnoDB;

    create table m_assignment_extension (
        owner_id integer not null,
        owner_owner_oid varchar(36) not null,
        booleansCount smallint,
        datesCount smallint,
        longsCount smallint,
        polysCount smallint,
        referencesCount smallint,
        stringsCount smallint,
        primary key (owner_id, owner_owner_oid)
    ) ENGINE=InnoDB;

    create table m_assignment_reference (
        owner_id integer not null,
        owner_owner_oid varchar(36) not null,
        reference_type integer not null,
        relation varchar(157) not null,
        targetOid varchar(36) not null,
        containerType integer,
        primary key (owner_id, owner_owner_oid, reference_type, relation, targetOid)
    ) ENGINE=InnoDB;

    create table m_audit_delta (
        checksum varchar(32) not null,
        record_id bigint not null,
        delta longtext,
        deltaOid varchar(36),
        deltaType integer,
        fullResult longtext,
        status integer,
        primary key (checksum, record_id)
    ) ENGINE=InnoDB;

    create table m_audit_event (
        id bigint not null,
        channel varchar(255),
        eventIdentifier varchar(255),
        eventStage integer,
        eventType integer,
        hostIdentifier varchar(255),
        initiatorName varchar(255),
        initiatorOid varchar(36),
        message varchar(1024),
        outcome integer,
        parameter varchar(255),
        result varchar(255),
        sessionIdentifier varchar(255),
        targetName varchar(255),
        targetOid varchar(36),
        targetOwnerName varchar(255),
        targetOwnerOid varchar(36),
        targetType integer,
        taskIdentifier varchar(255),
        taskOID varchar(255),
        timestampValue datetime,
        primary key (id)
    ) ENGINE=InnoDB;

    create table m_connector (
        connectorBundle varchar(255),
        connectorHostRef_relation varchar(157),
        connectorHostRef_targetOid varchar(36),
        connectorHostRef_type integer,
        connectorType varchar(255),
        connectorVersion varchar(255),
        framework varchar(255),
        name_norm varchar(255),
        name_orig varchar(255),
        oid varchar(36) not null,
        primary key (oid)
    ) ENGINE=InnoDB;

    create table m_connector_host (
        hostname varchar(255),
        name_norm varchar(255),
        name_orig varchar(255),
        port varchar(255),
        oid varchar(36) not null,
        primary key (oid)
    ) ENGINE=InnoDB;

    create table m_connector_target_system (
        connector_oid varchar(36) not null,
        targetSystemType varchar(255)
    ) ENGINE=InnoDB;

    create table m_exclusion (
        id integer not null,
        owner_oid varchar(36) not null,
        policy integer,
        targetRef_relation varchar(157),
        targetRef_targetOid varchar(36),
        targetRef_type integer,
        primary key (id, owner_oid)
    ) ENGINE=InnoDB;

    create table m_focus (
        administrativeStatus integer,
        archiveTimestamp datetime,
        disableReason varchar(255),
        disableTimestamp datetime,
        effectiveStatus integer,
        enableTimestamp datetime,
        validFrom datetime,
        validTo datetime,
        validityChangeTimestamp datetime,
        validityStatus integer,
        hasPhoto bit default false not null,
        oid varchar(36) not null,
        primary key (oid)
    ) ENGINE=InnoDB;

    create table m_focus_photo (
        owner_oid varchar(36) not null,
        photo longblob,
        primary key (owner_oid)
    ) ENGINE=InnoDB;

    create table m_generic_object (
        name_norm varchar(255),
        name_orig varchar(255),
        objectType varchar(255),
        oid varchar(36) not null,
        primary key (oid)
    ) ENGINE=InnoDB;

    create table m_lookup_table (
        name_norm varchar(255),
        name_orig varchar(255),
        oid varchar(36) not null,
        primary key (oid)
    ) ENGINE=InnoDB;

    create table m_lookup_table_row (
        id integer not null,
        owner_oid varchar(36) not null,
        row_key varchar(255),
        label_norm varchar(255),
        label_orig varchar(255),
        lastChangeTimestamp datetime,
        row_value varchar(255),
        primary key (id, owner_oid)
    ) ENGINE=InnoDB;

    create table m_node (
        name_norm varchar(255),
        name_orig varchar(255),
        nodeIdentifier varchar(255),
        oid varchar(36) not null,
        primary key (oid)
    ) ENGINE=InnoDB;

    create table m_object (
        oid varchar(36) not null,
        booleansCount smallint,
        createChannel varchar(255),
        createTimestamp datetime,
        creatorRef_relation varchar(157),
        creatorRef_targetOid varchar(36),
        creatorRef_type integer,
        datesCount smallint,
        fullObject longblob,
        longsCount smallint,
        modifierRef_relation varchar(157),
        modifierRef_targetOid varchar(36),
        modifierRef_type integer,
        modifyChannel varchar(255),
        modifyTimestamp datetime,
        name_norm varchar(255),
        name_orig varchar(255),
        objectTypeClass integer,
        polysCount smallint,
        referencesCount smallint,
        stringsCount smallint,
        tenantRef_relation varchar(157),
        tenantRef_targetOid varchar(36),
        tenantRef_type integer,
        version integer not null,
        primary key (oid)
    ) ENGINE=InnoDB;

    create table m_object_ext_boolean (
        eName varchar(157) not null,
        owner_oid varchar(36) not null,
        ownerType integer not null,
        booleanValue bit not null,
        dynamicDef bit,
        eType varchar(157),
        valueType integer,
        primary key (eName, owner_oid, ownerType, booleanValue)
    ) ENGINE=InnoDB;

    create table m_object_ext_date (
        eName varchar(157) not null,
        owner_oid varchar(36) not null,
        ownerType integer not null,
        dateValue datetime not null,
        dynamicDef bit,
        eType varchar(157),
        valueType integer,
        primary key (eName, owner_oid, ownerType, dateValue)
    ) ENGINE=InnoDB;

    create table m_object_ext_long (
        eName varchar(157) not null,
        owner_oid varchar(36) not null,
        ownerType integer not null,
        longValue bigint not null,
        dynamicDef bit,
        eType varchar(157),
        valueType integer,
        primary key (eName, owner_oid, ownerType, longValue)
    ) ENGINE=InnoDB;

    create table m_object_ext_poly (
        eName varchar(157) not null,
        owner_oid varchar(36) not null,
        ownerType integer not null,
        orig varchar(255) not null,
        dynamicDef bit,
        norm varchar(255),
        eType varchar(157),
        valueType integer,
        primary key (eName, owner_oid, ownerType, orig)
    ) ENGINE=InnoDB;

    create table m_object_ext_reference (
        eName varchar(157) not null,
        owner_oid varchar(36) not null,
        ownerType integer not null,
        targetoid varchar(36) not null,
        dynamicDef bit,
        relation varchar(157),
        targetType integer,
        eType varchar(157),
        valueType integer,
        primary key (eName, owner_oid, ownerType, targetoid)
    ) ENGINE=InnoDB;

    create table m_object_ext_string (
        eName varchar(157) not null,
        owner_oid varchar(36) not null,
        ownerType integer not null,
        stringValue varchar(255) not null,
        dynamicDef bit,
        eType varchar(157),
        valueType integer,
        primary key (eName, owner_oid, ownerType, stringValue)
    ) ENGINE=InnoDB;

    create table m_object_template (
        name_norm varchar(255),
        name_orig varchar(255),
        type integer,
        oid varchar(36) not null,
        primary key (oid)
    ) ENGINE=InnoDB;

    create table m_org (
        costCenter varchar(255),
        displayName_norm varchar(255),
        displayName_orig varchar(255),
        displayOrder integer,
        identifier varchar(255),
        locality_norm varchar(255),
        locality_orig varchar(255),
        name_norm varchar(255),
        name_orig varchar(255),
        tenant bit,
        oid varchar(36) not null,
        primary key (oid)
    ) ENGINE=InnoDB;

    create table m_org_closure (
        ancestor_oid varchar(36) not null,
        descendant_oid varchar(36) not null,
        val integer,
        primary key (ancestor_oid, descendant_oid)
    ) ENGINE=InnoDB;

    create table m_org_org_type (
        org_oid varchar(36) not null,
        orgType varchar(255)
    ) ENGINE=InnoDB;

    create table m_reference (
        owner_oid varchar(36) not null,
        reference_type integer not null,
        relation varchar(157) not null,
        targetOid varchar(36) not null,
        containerType integer,
        primary key (owner_oid, reference_type, relation, targetOid)
    ) ENGINE=InnoDB;

    create table m_report (
        export integer,
        name_norm varchar(255),
        name_orig varchar(255),
        orientation integer,
        parent bit,
        useHibernateSession bit,
        oid varchar(36) not null,
        primary key (oid)
    ) ENGINE=InnoDB;

    create table m_report_output (
        name_norm varchar(255),
        name_orig varchar(255),
        reportRef_relation varchar(157),
        reportRef_targetOid varchar(36),
        reportRef_type integer,
        oid varchar(36) not null,
        primary key (oid)
    ) ENGINE=InnoDB;

    create table m_resource (
        administrativeState integer,
        connectorRef_relation varchar(157),
        connectorRef_targetOid varchar(36),
        connectorRef_type integer,
        name_norm varchar(255),
        name_orig varchar(255),
        o16_lastAvailabilityStatus integer,
        oid varchar(36) not null,
        primary key (oid)
    ) ENGINE=InnoDB;

    create table m_role (
        name_norm varchar(255),
        name_orig varchar(255),
        roleType varchar(255),
        oid varchar(36) not null,
        primary key (oid)
    ) ENGINE=InnoDB;

    create table m_security_policy (
        name_norm varchar(255),
        name_orig varchar(255),
        oid varchar(36) not null,
        primary key (oid)
    ) ENGINE=InnoDB;

    create table m_sequence (
        name_norm varchar(255),
        name_orig varchar(255),
        oid varchar(36) not null,
        primary key (oid)
    ) ENGINE=InnoDB;

    create table m_shadow (
        attemptNumber integer,
        dead bit,
        exist bit,
        failedOperationType integer,
        fullSynchronizationTimestamp datetime,
        intent varchar(255),
        kind integer,
        name_norm varchar(255),
        name_orig varchar(255),
        objectClass varchar(157),
        resourceRef_relation varchar(157),
        resourceRef_targetOid varchar(36),
        resourceRef_type integer,
        status integer,
        synchronizationSituation integer,
        synchronizationTimestamp datetime,
        oid varchar(36) not null,
        primary key (oid)
    ) ENGINE=InnoDB;

    create table m_system_configuration (
        name_norm varchar(255),
        name_orig varchar(255),
        oid varchar(36) not null,
        primary key (oid)
    ) ENGINE=InnoDB;

    create table m_task (
        binding integer,
        canRunOnNode varchar(255),
        category varchar(255),
        completionTimestamp datetime,
        executionStatus integer,
        handlerUri varchar(255),
        lastRunFinishTimestamp datetime,
        lastRunStartTimestamp datetime,
        name_norm varchar(255),
        name_orig varchar(255),
        node varchar(255),
        objectRef_relation varchar(157),
        objectRef_targetOid varchar(36),
        objectRef_type integer,
        ownerRef_relation varchar(157),
        ownerRef_targetOid varchar(36),
        ownerRef_type integer,
        parent varchar(255),
        recurrence integer,
        status integer,
        taskIdentifier varchar(255),
        threadStopAction integer,
        waitingReason integer,
        oid varchar(36) not null,
        primary key (oid)
    ) ENGINE=InnoDB;

    create table m_task_dependent (
        task_oid varchar(36) not null,
        dependent varchar(255)
    ) ENGINE=InnoDB;

    create table m_trigger (
        id integer not null,
        owner_oid varchar(36) not null,
        handlerUri varchar(255),
        timestampValue datetime,
        primary key (id, owner_oid)
    ) ENGINE=InnoDB;

    create table m_user (
        additionalName_norm varchar(255),
        additionalName_orig varchar(255),
        costCenter varchar(255),
        emailAddress varchar(255),
        employeeNumber varchar(255),
        familyName_norm varchar(255),
        familyName_orig varchar(255),
        fullName_norm varchar(255),
        fullName_orig varchar(255),
        givenName_norm varchar(255),
        givenName_orig varchar(255),
        honorificPrefix_norm varchar(255),
        honorificPrefix_orig varchar(255),
        honorificSuffix_norm varchar(255),
        honorificSuffix_orig varchar(255),
        locale varchar(255),
        locality_norm varchar(255),
        locality_orig varchar(255),
        name_norm varchar(255),
        name_orig varchar(255),
        nickName_norm varchar(255),
        nickName_orig varchar(255),
        preferredLanguage varchar(255),
        status integer,
        telephoneNumber varchar(255),
        timezone varchar(255),
        title_norm varchar(255),
        title_orig varchar(255),
        oid varchar(36) not null,
        primary key (oid)
    ) ENGINE=InnoDB;

    create table m_user_employee_type (
        user_oid varchar(36) not null,
        employeeType varchar(255)
    ) ENGINE=InnoDB;

    create table m_user_organization (
        user_oid varchar(36) not null,
        norm varchar(255),
        orig varchar(255)
    ) ENGINE=InnoDB;

    create table m_user_organizational_unit (
        user_oid varchar(36) not null,
        norm varchar(255),
        orig varchar(255)
    ) ENGINE=InnoDB;

    create table m_value_policy (
        name_norm varchar(255),
        name_orig varchar(255),
        oid varchar(36) not null,
        primary key (oid)
    ) ENGINE=InnoDB;

    create index iRequestable on m_abstract_role (requestable);

    alter table m_acc_cert_campaign 
        add constraint uc_acc_cert_campaign_name  unique (name_norm);

    alter table m_acc_cert_definition 
        add constraint uc_acc_cert_definition_name  unique (name_norm);

    create index iAssignmentAdministrative on m_assignment (administrativeStatus);

    create index iAssignmentEffective on m_assignment (effectiveStatus);

    create index iTargetRefTargetOid on m_assignment (targetRef_targetOid);

    create index iTenantRefTargetOid on m_assignment (tenantRef_targetOid);

    create index iOrgRefTargetOid on m_assignment (orgRef_targetOid);

    create index iAExtensionBoolean on m_assignment_ext_boolean (extensionType, eName, booleanValue);

    create index iAExtensionDate on m_assignment_ext_date (extensionType, eName, dateValue);

    create index iAExtensionLong on m_assignment_ext_long (extensionType, eName, longValue);

    create index iAExtensionPolyString on m_assignment_ext_poly (extensionType, eName, orig);

    create index iAExtensionReference on m_assignment_ext_reference (extensionType, eName, targetoid);

    create index iAExtensionString on m_assignment_ext_string (extensionType, eName, stringValue);

    create index iAssignmentReferenceTargetOid on m_assignment_reference (targetOid);

    alter table m_connector_host 
        add constraint uc_connector_host_name  unique (name_norm);

    create index iFocusAdministrative on m_focus (administrativeStatus);

    create index iFocusEffective on m_focus (effectiveStatus);

    alter table m_generic_object 
        add constraint uc_generic_object_name  unique (name_norm);

    alter table m_lookup_table 
        add constraint uc_lookup_name  unique (name_norm);

    alter table m_lookup_table_row 
        add constraint uc_row_key  unique (row_key);

    alter table m_node 
        add constraint uc_node_name  unique (name_norm);

    create index iObjectNameOrig on m_object (name_orig);

    create index iObjectNameNorm on m_object (name_norm);

    create index iObjectTypeClass on m_object (objectTypeClass);

    create index iObjectCreateTimestamp on m_object (createTimestamp);

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

    alter table m_object_template 
        add constraint uc_object_template_name  unique (name_norm);

    alter table m_org 
        add constraint uc_org_name  unique (name_norm);

    create index iDisplayOrder on m_org (displayOrder);

    create index iAncestor on m_org_closure (ancestor_oid);

    create index iDescendant on m_org_closure (descendant_oid);

    create index iDescendantAncestor on m_org_closure (descendant_oid, ancestor_oid);

    create index iReferenceTargetOid on m_reference (targetOid);

    alter table m_report 
        add constraint uc_report_name  unique (name_norm);

    create index iReportParent on m_report (parent);

    alter table m_resource 
        add constraint uc_resource_name  unique (name_norm);

    alter table m_role 
        add constraint uc_role_name  unique (name_norm);

    alter table m_security_policy 
        add constraint uc_security_policy_name  unique (name_norm);

    alter table m_sequence 
        add constraint uc_sequence_name  unique (name_norm);

    create index iShadowResourceRef on m_shadow (resourceRef_targetOid);

    create index iShadowDead on m_shadow (dead);

    alter table m_system_configuration 
        add constraint uc_system_configuration_name  unique (name_norm);

    create index iParent on m_task (parent);

    create index iTriggerTimestamp on m_trigger (timestampValue);

    alter table m_user 
        add constraint uc_user_name  unique (name_norm);

    create index iEmployeeNumber on m_user (employeeNumber);

    create index iFullName on m_user (fullName_orig);

    create index iFamilyName on m_user (familyName_orig);

    create index iGivenName on m_user (givenName_orig);

    create index iLocality on m_user (locality_orig);

    alter table m_value_policy 
        add constraint uc_value_policy_name  unique (name_norm);

    alter table m_abstract_role 
        add constraint fk_abstract_role 
        foreign key (oid) 
        references m_focus (oid);

    alter table m_acc_cert_campaign 
        add constraint fk_acc_cert_campaign 
        foreign key (oid) 
        references m_object (oid);

    alter table m_acc_cert_definition 
        add constraint fk_acc_cert_definition 
        foreign key (oid) 
        references m_object (oid);

    alter table m_assignment 
        add constraint fk_assignment_owner 
        foreign key (owner_oid) 
        references m_object (oid);

    alter table m_assignment_ext_boolean 
        add constraint fk_assignment_ext_boolean 
        foreign key (anyContainer_owner_id, anyContainer_owner_owner_oid) 
        references m_assignment_extension (owner_id, owner_owner_oid);

    alter table m_assignment_ext_date 
        add constraint fk_assignment_ext_date 
        foreign key (anyContainer_owner_id, anyContainer_owner_owner_oid) 
        references m_assignment_extension (owner_id, owner_owner_oid);

    alter table m_assignment_ext_long 
        add constraint fk_assignment_ext_long 
        foreign key (anyContainer_owner_id, anyContainer_owner_owner_oid) 
        references m_assignment_extension (owner_id, owner_owner_oid);

    alter table m_assignment_ext_poly 
        add constraint fk_assignment_ext_poly 
        foreign key (anyContainer_owner_id, anyContainer_owner_owner_oid) 
        references m_assignment_extension (owner_id, owner_owner_oid);

    alter table m_assignment_ext_reference 
        add constraint fk_assignment_ext_reference 
        foreign key (anyContainer_owner_id, anyContainer_owner_owner_oid) 
        references m_assignment_extension (owner_id, owner_owner_oid);

    alter table m_assignment_ext_string 
        add constraint fk_assignment_ext_string 
        foreign key (anyContainer_owner_id, anyContainer_owner_owner_oid) 
        references m_assignment_extension (owner_id, owner_owner_oid);

    alter table m_assignment_reference 
        add constraint fk_assignment_reference 
        foreign key (owner_id, owner_owner_oid) 
        references m_assignment (id, owner_oid);

    alter table m_audit_delta 
        add constraint fk_audit_delta 
        foreign key (record_id) 
        references m_audit_event (id);

    alter table m_connector 
        add constraint fk_connector 
        foreign key (oid) 
        references m_object (oid);

    alter table m_connector_host 
        add constraint fk_connector_host 
        foreign key (oid) 
        references m_object (oid);

    alter table m_connector_target_system 
        add constraint fk_connector_target_system 
        foreign key (connector_oid) 
        references m_connector (oid);

    alter table m_exclusion 
        add constraint fk_exclusion_owner 
        foreign key (owner_oid) 
        references m_object (oid);

    alter table m_focus 
        add constraint fk_focus 
        foreign key (oid) 
        references m_object (oid);

    alter table m_focus_photo 
        add constraint fk_focus_photo 
        foreign key (owner_oid) 
        references m_focus (oid);

    alter table m_generic_object 
        add constraint fk_generic_object 
        foreign key (oid) 
        references m_object (oid);

    alter table m_lookup_table 
        add constraint fk_lookup_table 
        foreign key (oid) 
        references m_object (oid);

    alter table m_lookup_table_row 
        add constraint fk_lookup_table_owner 
        foreign key (owner_oid) 
        references m_lookup_table (oid);

    alter table m_node 
        add constraint fk_node 
        foreign key (oid) 
        references m_object (oid);

    alter table m_object_ext_boolean 
        add constraint fk_object_ext_boolean 
        foreign key (owner_oid) 
        references m_object (oid);

    alter table m_object_ext_date 
        add constraint fk_object_ext_date 
        foreign key (owner_oid) 
        references m_object (oid);

    alter table m_object_ext_long 
        add constraint fk_object_ext_long 
        foreign key (owner_oid) 
        references m_object (oid);

    alter table m_object_ext_poly 
        add constraint fk_object_ext_poly 
        foreign key (owner_oid) 
        references m_object (oid);

    alter table m_object_ext_reference 
        add constraint fk_object_ext_reference 
        foreign key (owner_oid) 
        references m_object (oid);

    alter table m_object_ext_string 
        add constraint fk_object_ext_string 
        foreign key (owner_oid) 
        references m_object (oid);

    alter table m_object_template 
        add constraint fk_object_template 
        foreign key (oid) 
        references m_object (oid);

    alter table m_org 
        add constraint fk_org 
        foreign key (oid) 
        references m_abstract_role (oid);

    alter table m_org_closure 
        add constraint fk_ancestor 
        foreign key (ancestor_oid) 
        references m_object (oid);

    alter table m_org_closure 
        add constraint fk_descendant 
        foreign key (descendant_oid) 
        references m_object (oid);

    alter table m_org_org_type 
        add constraint fk_org_org_type 
        foreign key (org_oid) 
        references m_org (oid);

    alter table m_reference 
        add constraint fk_reference_owner 
        foreign key (owner_oid) 
        references m_object (oid);

    alter table m_report 
        add constraint fk_report 
        foreign key (oid) 
        references m_object (oid);

    alter table m_report_output 
        add constraint fk_report_output 
        foreign key (oid) 
        references m_object (oid);

    alter table m_resource 
        add constraint fk_resource 
        foreign key (oid) 
        references m_object (oid);

    alter table m_role 
        add constraint fk_role 
        foreign key (oid) 
        references m_abstract_role (oid);

    alter table m_security_policy 
        add constraint fk_security_policy 
        foreign key (oid) 
        references m_object (oid);

    alter table m_sequence 
        add constraint fk_sequence 
        foreign key (oid) 
        references m_object (oid);

    alter table m_shadow 
        add constraint fk_shadow 
        foreign key (oid) 
        references m_object (oid);

    alter table m_system_configuration 
        add constraint fk_system_configuration 
        foreign key (oid) 
        references m_object (oid);

    alter table m_task 
        add constraint fk_task 
        foreign key (oid) 
        references m_object (oid);

    alter table m_task_dependent 
        add constraint fk_task_dependent 
        foreign key (task_oid) 
        references m_task (oid);

    alter table m_trigger 
        add constraint fk_trigger_owner 
        foreign key (owner_oid) 
        references m_object (oid);

    alter table m_user 
        add constraint fk_user 
        foreign key (oid) 
        references m_focus (oid);

    alter table m_user_employee_type 
        add constraint fk_user_employee_type 
        foreign key (user_oid) 
        references m_user (oid);

    alter table m_user_organization 
        add constraint fk_user_organization 
        foreign key (user_oid) 
        references m_user (oid);

    alter table m_user_organizational_unit 
        add constraint fk_user_org_unit 
        foreign key (user_oid) 
        references m_user (oid);

    alter table m_value_policy 
        add constraint fk_value_policy 
        foreign key (oid) 
        references m_object (oid);

    create table hibernate_sequence (
         next_val bigint 
    );

    insert into hibernate_sequence values ( 1 );
