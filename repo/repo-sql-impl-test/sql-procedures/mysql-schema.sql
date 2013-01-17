
    create table m_account_shadow (
        accountType varchar(255),
        allowedIdmAdminGuiAccess boolean,
        passwordXml longtext,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    ) ENGINE=InnoDB;

    create table m_any (
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        ownerType integer not null,
        primary key (owner_id, owner_oid, ownerType)
    ) ENGINE=InnoDB;

    create table m_any_clob (
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        ownerType integer not null,
        clobValue longtext,
        dynamicDef boolean,
        name_namespace varchar(255),
        name_localPart varchar(255),
        type_namespace varchar(255),
        type_localPart varchar(255),
        valueType integer
    ) ENGINE=InnoDB;

    create table m_any_date (
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        ownerType integer not null,
        dateValue datetime,
        dynamicDef boolean,
        name_namespace varchar(255),
        name_localPart varchar(255),
        type_namespace varchar(255),
        type_localPart varchar(255),
        valueType integer
    ) ENGINE=InnoDB;

    create table m_any_long (
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        ownerType integer not null,
        longValue bigint,
        dynamicDef boolean,
        name_namespace varchar(255),
        name_localPart varchar(255),
        type_namespace varchar(255),
        type_localPart varchar(255),
        valueType integer
    ) ENGINE=InnoDB;

    create table m_any_reference (
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        ownerType integer not null,
        oidValue varchar(255),
        dynamicDef boolean,
        name_namespace varchar(255),
        name_localPart varchar(255),
        type_namespace varchar(255),
        type_localPart varchar(255),
        valueType integer
    ) ENGINE=InnoDB;

    create table m_any_string (
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        ownerType integer not null,
        stringValue varchar(255),
        dynamicDef boolean,
        name_namespace varchar(255),
        name_localPart varchar(255),
        type_namespace varchar(255),
        type_localPart varchar(255),
        valueType integer
    ) ENGINE=InnoDB;

    create table m_assignment (
        accountConstruction longtext,
        enabled boolean,
        validFrom datetime,
        validTo datetime,
        description longtext,
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        targetRef_description longtext,
        targetRef_filter longtext,
        targetRef_relationLocalPart varchar(255),
        targetRef_relationNamespace varchar(255),
        targetRef_targetOid varchar(36),
        targetRef_type integer,
        id bigint not null,
        oid varchar(36) not null,
        extId bigint,
        extOid varchar(36),
        extType integer,
        primary key (id, oid)
    ) ENGINE=InnoDB;

    create table m_audit_delta (
        RAuditEventRecord_id bigint not null,
        deltas longtext
    ) ENGINE=InnoDB;

    create table m_audit_event (
        id bigint not null,
        channel varchar(255),
        eventIdentifier varchar(255),
        eventStage integer,
        eventType integer,
        hostIdentifier varchar(255),
        initiator longtext,
        outcome integer,
        sessionIdentifier varchar(255),
        target longtext,
        targetOwner longtext,
        taskIdentifier varchar(255),
        taskOID varchar(255),
        timestampValue bigint,
        primary key (id)
    ) ENGINE=InnoDB;

    create table m_connector (
        connectorBundle varchar(255),
        connectorType varchar(255),
        connectorVersion varchar(255),
        framework varchar(255),
        name_norm varchar(255),
        name_orig varchar(255),
        namespace varchar(255),
        xmlSchema longtext,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    ) ENGINE=InnoDB;

    create table m_connector_host (
        hostname varchar(255),
        name_norm varchar(255),
        name_orig varchar(255),
        port varchar(255),
        protectConnection boolean,
        sharedSecret longtext,
        timeout integer,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) ENGINE=InnoDB;

    create table m_connector_target_system (
        connector_id bigint not null,
        connector_oid varchar(36) not null,
        targetSystemType varchar(255)
    ) ENGINE=InnoDB;

    create table m_container (
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    ) ENGINE=InnoDB;

    create table m_exclusion (
        description longtext,
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        policy integer,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    ) ENGINE=InnoDB;

    create table m_generic_object (
        name_norm varchar(255),
        name_orig varchar(255),
        objectType varchar(255),
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) ENGINE=InnoDB;

    create table m_node (
        clusteredNode boolean,
        hostname varchar(255),
        internalNodeIdentifier varchar(255),
        jmxPort integer,
        lastCheckInTime datetime,
        name_norm varchar(255),
        name_orig varchar(255),
        nodeIdentifier varchar(255),
        running boolean,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) ENGINE=InnoDB;

    create table m_object (
        description longtext,
        version bigint not null,
        id bigint not null,
        oid varchar(36) not null,
        extId bigint,
        extOid varchar(36),
        extType integer,
        primary key (id, oid)
    ) ENGINE=InnoDB;

    create table m_object_org_ref (
        object_id bigint not null,
        object_oid varchar(36) not null,
        description longtext,
        filter longtext,
        relationLocalPart varchar(255),
        relationNamespace varchar(255),
        targetOid varchar(36),
        type integer
    ) ENGINE=InnoDB;

    create table m_operation_result (
        owner_oid varchar(36) not null,
        owner_id bigint not null,
        details longtext,
        localizedMessage longtext,
        message longtext,
        messageCode varchar(255),
        operation longtext,
        params longtext,
        partialResults longtext,
        status integer,
        token bigint,
        primary key (owner_oid, owner_id)
    ) ENGINE=InnoDB;

    create table m_org (
        costCenter varchar(255),
        displayName_norm varchar(255),
        displayName_orig varchar(255),
        identifier varchar(255),
        locality_norm varchar(255),
        locality_orig varchar(255),
        name_norm varchar(255),
        name_orig varchar(255),
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) ENGINE=InnoDB;

    create table m_org_closure (
        id bigint not null,
        depthValue integer,
        ancestor_id bigint,
        ancestor_oid varchar(36),
        descendant_id bigint,
        descendant_oid varchar(36),
        primary key (id)
    ) ENGINE=InnoDB;

    create table m_org_org_type (
        org_id bigint not null,
        org_oid varchar(36) not null,
        orgType varchar(255)
    ) ENGINE=InnoDB;

    create table m_org_sys_config (
        org_id bigint not null,
        org_oid varchar(36) not null,
        description longtext,
        filter longtext,
        relationLocalPart varchar(255),
        relationNamespace varchar(255),
        targetOid varchar(36),
        type integer
    ) ENGINE=InnoDB;

    create table m_password_policy (
        lifetime longtext,
        name_norm varchar(255),
        name_orig varchar(255),
        stringPolicy longtext,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) ENGINE=InnoDB;

    create table m_reference (
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        targetOid varchar(36) not null,
        description longtext,
        filter longtext,
        reference_relationLocalPart varchar(255),
        reference_relationNamespace varchar(255),
        type integer,
        primary key (owner_id, owner_oid, targetOid)
    ) ENGINE=InnoDB;

    create table m_resource (
        business_administrativeState integer,
        capabilities_cachingMetadata longtext,
        capabilities_configured longtext,
        capabilities_native longtext,
        configuration longtext,
        connectorRef_description longtext,
        connectorRef_filter longtext,
        connectorRef_relationLocalPart varchar(255),
        connectorRef_relationNamespace varchar(255),
        connectorRef_targetOid varchar(36),
        connectorRef_type integer,
        consistency longtext,
        name_norm varchar(255),
        name_orig varchar(255),
        namespace varchar(255),
        o16_lastAvailabilityStatus integer,
        schemaHandling longtext,
        scripts longtext,
        synchronization longtext,
        xmlSchema longtext,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) ENGINE=InnoDB;

    create table m_resource_approver_ref (
        user_id bigint not null,
        user_oid varchar(36) not null,
        description longtext,
        filter longtext,
        relationLocalPart varchar(255),
        relationNamespace varchar(255),
        targetOid varchar(36),
        type integer
    ) ENGINE=InnoDB;

    create table m_resource_shadow (
        enabled boolean,
        validFrom datetime,
        validTo datetime,
        attemptNumber integer,
        dead boolean,
        failedOperationType integer,
        intent varchar(255),
        name_norm varchar(255),
        name_orig varchar(255),
        objectChange longtext,
        class_namespace varchar(255),
        class_localPart varchar(255),
        synchronizationSituation integer,
        synchronizationTimestamp datetime,
        id bigint not null,
        oid varchar(36) not null,
        attrId bigint,
        attrOid varchar(36),
        attrType integer,
        primary key (id, oid)
    ) ENGINE=InnoDB;

    create table m_role (
        approvalExpression longtext,
        approvalProcess varchar(255),
        approvalSchema longtext,
        automaticallyApproved longtext,
        name_norm varchar(255),
        name_orig varchar(255),
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) ENGINE=InnoDB;

    create table m_sync_situation_description (
        shadow_id bigint not null,
        shadow_oid varchar(36) not null,
        chanel varchar(255),
        situation integer,
        timestamp datetime
    ) ENGINE=InnoDB;

    create table m_system_configuration (
        connectorFramework longtext,
        g36 longtext,
        g23_description longtext,
        globalPasswordPolicyRef_filter longtext,
        g23_relationLocalPart varchar(255),
        g23_relationNamespace varchar(255),
        g23_targetOid varchar(36),
        globalPasswordPolicyRef_type integer,
        logging longtext,
        modelHooks longtext,
        name_norm varchar(255),
        name_orig varchar(255),
        notificationConfiguration longtext,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) ENGINE=InnoDB;

    create table m_task (
        binding integer,
        canRunOnNode varchar(255),
        category varchar(255),
        claimExpirationTimestamp datetime,
        exclusivityStatus integer,
        executionStatus integer,
        handlerUri varchar(255),
        lastRunFinishTimestamp datetime,
        lastRunStartTimestamp datetime,
        modelOperationState longtext,
        name_norm varchar(255),
        name_orig varchar(255),
        nextRunStartTime datetime,
        node varchar(255),
        objectRef_description longtext,
        objectRef_filter longtext,
        objectRef_relationLocalPart varchar(255),
        objectRef_relationNamespace varchar(255),
        objectRef_targetOid varchar(36),
        objectRef_type integer,
        otherHandlersUriStack longtext,
        ownerRef_description longtext,
        ownerRef_filter longtext,
        ownerRef_relationLocalPart varchar(255),
        ownerRef_relationNamespace varchar(255),
        ownerRef_targetOid varchar(36),
        ownerRef_type integer,
        parent varchar(255),
        progress bigint,
        recurrence integer,
        resultStatus integer,
        schedule longtext,
        taskIdentifier varchar(255),
        threadStopAction integer,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    ) ENGINE=InnoDB;

    create table m_user (
        enabled boolean,
        validFrom datetime,
        validTo datetime,
        additionalName_norm varchar(255),
        additionalName_orig varchar(255),
        costCenter varchar(255),
        allowedIdmAdminGuiAccess boolean,
        passwordXml longtext,
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
        telephoneNumber varchar(255),
        timezone varchar(255),
        title_norm varchar(255),
        title_orig varchar(255),
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) ENGINE=InnoDB;

    create table m_user_employee_type (
        user_id bigint not null,
        user_oid varchar(36) not null,
        employeeType varchar(255)
    ) ENGINE=InnoDB;

    create table m_user_organizational_unit (
        user_id bigint not null,
        user_oid varchar(36) not null,
        norm varchar(255),
        orig varchar(255)
    ) ENGINE=InnoDB;

    create table m_user_template (
        accountConstruction longtext,
        name_norm varchar(255),
        name_orig varchar(255),
        propertyConstruction longtext,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) ENGINE=InnoDB;

    alter table m_account_shadow
        add index fk_account_shadow (id, oid),
        add constraint fk_account_shadow
        foreign key (id, oid)
        references m_resource_shadow (id, oid);

    alter table m_any_clob
        add index fk_any_clob (owner_id, owner_oid, ownerType),
        add constraint fk_any_clob
        foreign key (owner_id, owner_oid, ownerType)
        references m_any (owner_id, owner_oid, ownerType);

    create index iDate on m_any_date (dateValue);

    alter table m_any_date
        add index fk_any_date (owner_id, owner_oid, ownerType),
        add constraint fk_any_date
        foreign key (owner_id, owner_oid, ownerType)
        references m_any (owner_id, owner_oid, ownerType);

    create index iLong on m_any_long (longValue);

    alter table m_any_long
        add index fk_any_long (owner_id, owner_oid, ownerType),
        add constraint fk_any_long
        foreign key (owner_id, owner_oid, ownerType)
        references m_any (owner_id, owner_oid, ownerType);

    create index iOid on m_any_reference (oidValue);

    alter table m_any_reference
        add index fk_any_reference (owner_id, owner_oid, ownerType),
        add constraint fk_any_reference
        foreign key (owner_id, owner_oid, ownerType)
        references m_any (owner_id, owner_oid, ownerType);

    create index iString on m_any_string (stringValue);

    alter table m_any_string
        add index fk_any_string (owner_id, owner_oid, ownerType),
        add constraint fk_any_string
        foreign key (owner_id, owner_oid, ownerType)
        references m_any (owner_id, owner_oid, ownerType);

    create index iAssignmentEnabled on m_assignment (enabled);

    alter table m_assignment
        add index fk_assignment (id, oid),
        add constraint fk_assignment
        foreign key (id, oid)
        references m_container (id, oid);

    alter table m_assignment
        add index fk_assignment_owner (owner_id, owner_oid),
        add constraint fk_assignment_owner
        foreign key (owner_id, owner_oid)
        references m_object (id, oid);

    alter table m_audit_delta
        add index fk_audit_delta (RAuditEventRecord_id),
        add constraint fk_audit_delta
        foreign key (RAuditEventRecord_id)
        references m_audit_event (id);

    create index iConnectorName on m_connector (name_norm);

    alter table m_connector
        add index fk_connector (id, oid),
        add constraint fk_connector
        foreign key (id, oid)
        references m_object (id, oid);

    alter table m_connector_host
        add index fk_connector_host (id, oid),
        add constraint fk_connector_host
        foreign key (id, oid)
        references m_object (id, oid);

    alter table m_connector_target_system
        add index fk_connector_target_system (connector_id, connector_oid),
        add constraint fk_connector_target_system
        foreign key (connector_id, connector_oid)
        references m_connector (id, oid);

    alter table m_exclusion
        add index fk_exclusion (id, oid),
        add constraint fk_exclusion
        foreign key (id, oid)
        references m_container (id, oid);

    alter table m_exclusion
        add index fk_exclusion_owner (owner_id, owner_oid),
        add constraint fk_exclusion_owner
        foreign key (owner_id, owner_oid)
        references m_object (id, oid);

    alter table m_generic_object
        add index fk_generic_object (id, oid),
        add constraint fk_generic_object
        foreign key (id, oid)
        references m_object (id, oid);

    alter table m_node
        add index fk_node (id, oid),
        add constraint fk_node
        foreign key (id, oid)
        references m_object (id, oid);

    alter table m_object
        add index fk_container (id, oid),
        add constraint fk_container
        foreign key (id, oid)
        references m_container (id, oid);

    alter table m_object_org_ref
        add index fk_object_org_ref (object_id, object_oid),
        add constraint fk_object_org_ref
        foreign key (object_id, object_oid)
        references m_object (id, oid);

    alter table m_operation_result
        add index fk_result_owner (owner_id, owner_oid),
        add constraint fk_result_owner
        foreign key (owner_id, owner_oid)
        references m_object (id, oid);

    alter table m_org
        add index fk_org (id, oid),
        add constraint fk_org
        foreign key (id, oid)
        references m_object (id, oid);

    create index iDescendant on m_org_closure (descendant_oid, descendant_id);

    create index iAncestor on m_org_closure (ancestor_oid, ancestor_id);

    alter table m_org_closure
        add index fk_descendant (descendant_id, descendant_oid),
        add constraint fk_descendant
        foreign key (descendant_id, descendant_oid)
        references m_object (id, oid);

    alter table m_org_closure
        add index fk_ancestor (ancestor_id, ancestor_oid),
        add constraint fk_ancestor
        foreign key (ancestor_id, ancestor_oid)
        references m_object (id, oid);

    alter table m_org_org_type
        add index fk_org_org_type (org_id, org_oid),
        add constraint fk_org_org_type
        foreign key (org_id, org_oid)
        references m_org (id, oid);

    alter table m_org_sys_config
        add index fk_org_unit (org_id, org_oid),
        add constraint fk_org_unit
        foreign key (org_id, org_oid)
        references m_system_configuration (id, oid);

    alter table m_password_policy
        add index fk_password_policy (id, oid),
        add constraint fk_password_policy
        foreign key (id, oid)
        references m_object (id, oid);

    alter table m_reference
        add index fk_reference_owner (owner_id, owner_oid),
        add constraint fk_reference_owner
        foreign key (owner_id, owner_oid)
        references m_container (id, oid);

    alter table m_resource
        add index fk_resource (id, oid),
        add constraint fk_resource
        foreign key (id, oid)
        references m_object (id, oid);

    alter table m_resource_approver_ref
        add index fk_resource_approver_ref (user_id, user_oid),
        add constraint fk_resource_approver_ref
        foreign key (user_id, user_oid)
        references m_resource (id, oid);

    create index iResourceObjectShadowEnabled on m_resource_shadow (enabled);

    create index iResourceShadowName on m_resource_shadow (name_norm);

    alter table m_resource_shadow
        add index fk_resource_object_shadow (id, oid),
        add constraint fk_resource_object_shadow
        foreign key (id, oid)
        references m_object (id, oid);

    alter table m_role
        add index fk_role (id, oid),
        add constraint fk_role
        foreign key (id, oid)
        references m_object (id, oid);

    alter table m_sync_situation_description
        add index fk_shadow_sync_situation (shadow_id, shadow_oid),
        add constraint fk_shadow_sync_situation
        foreign key (shadow_id, shadow_oid)
        references m_resource_shadow (id, oid);

    alter table m_system_configuration
        add index fk_system_configuration (id, oid),
        add constraint fk_system_configuration
        foreign key (id, oid)
        references m_object (id, oid);

    create index iTaskName on m_task (name_norm);

    alter table m_task
        add index fk_task (id, oid),
        add constraint fk_task
        foreign key (id, oid)
        references m_object (id, oid);

    create index iFullName on m_user (fullName_norm);

    create index iLocality on m_user (locality_norm);

    create index iHonorificSuffix on m_user (honorificSuffix_norm);

    create index iEmployeeNumber on m_user (employeeNumber);

    create index iGivenName on m_user (givenName_norm);

    create index iFamilyName on m_user (familyName_norm);

    create index iAdditionalName on m_user (additionalName_norm);

    create index iHonorificPrefix on m_user (honorificPrefix_norm);

    create index iUserEnabled on m_user (enabled);

    alter table m_user
        add index fk_user (id, oid),
        add constraint fk_user
        foreign key (id, oid)
        references m_object (id, oid);

    alter table m_user_employee_type
        add index fk_user_employee_type (user_id, user_oid),
        add constraint fk_user_employee_type
        foreign key (user_id, user_oid)
        references m_user (id, oid);

    alter table m_user_organizational_unit
        add index fk_user_org_unit (user_id, user_oid),
        add constraint fk_user_org_unit
        foreign key (user_id, user_oid)
        references m_user (id, oid);

    alter table m_user_template
        add index fk_user_template (id, oid),
        add constraint fk_user_template
        foreign key (id, oid)
        references m_object (id, oid);

    create table hibernate_sequence (
         next_val bigint
    );

    insert into hibernate_sequence values ( 1 );