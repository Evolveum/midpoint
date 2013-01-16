
    create table m_account_shadow (
        accountType varchar(255),
        allowedIdmAdminGuiAccess bit,
        passwordXml varchar(MAX),
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    );

    create table m_any (
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        ownerType int not null,
        primary key (owner_id, owner_oid, ownerType)
    );

    create table m_any_clob (
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        ownerType int not null,
        clobValue varchar(MAX),
        dynamicDef bit,
        name_namespace varchar(255),
        name_localPart varchar(255),
        type_namespace varchar(255),
        type_localPart varchar(255),
        valueType int
    );

    create table m_any_date (
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        ownerType int not null,
        dateValue datetime2,
        dynamicDef bit,
        name_namespace varchar(255),
        name_localPart varchar(255),
        type_namespace varchar(255),
        type_localPart varchar(255),
        valueType int
    );

    create table m_any_long (
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        ownerType int not null,
        longValue bigint,
        dynamicDef bit,
        name_namespace varchar(255),
        name_localPart varchar(255),
        type_namespace varchar(255),
        type_localPart varchar(255),
        valueType int
    );

    create table m_any_reference (
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        ownerType int not null,
        oidValue varchar(255),
        dynamicDef bit,
        name_namespace varchar(255),
        name_localPart varchar(255),
        type_namespace varchar(255),
        type_localPart varchar(255),
        valueType int
    );

    create table m_any_string (
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        ownerType int not null,
        stringValue varchar(255),
        dynamicDef bit,
        name_namespace varchar(255),
        name_localPart varchar(255),
        type_namespace varchar(255),
        type_localPart varchar(255),
        valueType int
    );

    create table m_assignment (
        accountConstruction varchar(MAX),
        enabled bit,
        validFrom datetime2,
        validTo datetime2,
        description varchar(MAX),
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        targetRef_description varchar(MAX),
        targetRef_filter varchar(MAX),
        targetRef_relationLocalPart varchar(255),
        targetRef_relationNamespace varchar(255),
        targetRef_targetOid varchar(36),
        targetRef_type int,
        id bigint not null,
        oid varchar(36) not null,
        extId bigint,
        extOid varchar(36),
        extType int,
        primary key (id, oid)
    );

    create table m_audit_delta (
        RAuditEventRecord_id bigint not null,
        deltas varchar(MAX)
    );

    create table m_audit_event (
        id bigint not null,
        channel varchar(255),
        eventIdentifier varchar(255),
        eventStage int,
        eventType int,
        hostIdentifier varchar(255),
        initiator varchar(MAX),
        outcome int,
        sessionIdentifier varchar(255),
        target varchar(MAX),
        targetOwner varchar(MAX),
        taskIdentifier varchar(255),
        taskOID varchar(255),
        timestampValue bigint,
        primary key (id)
    );

    create table m_connector (
        connectorBundle varchar(255),
        connectorType varchar(255),
        connectorVersion varchar(255),
        framework varchar(255),
        name_norm varchar(255),
        name_orig varchar(255),
        namespace varchar(255),
        xmlSchema varchar(MAX),
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    );

    create table m_connector_host (
        hostname varchar(255),
        name_norm varchar(255),
        name_orig varchar(255),
        port varchar(255),
        protectConnection bit,
        sharedSecret varchar(MAX),
        timeout int,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_connector_target_system (
        connector_id bigint not null,
        connector_oid varchar(36) not null,
        targetSystemType varchar(255)
    );

    create table m_container (
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    );

    create table m_exclusion (
        description varchar(MAX),
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        policy int,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    );

    create table m_generic_object (
        name_norm varchar(255),
        name_orig varchar(255),
        objectType varchar(255),
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_node (
        clusteredNode bit,
        hostname varchar(255),
        internalNodeIdentifier varchar(255),
        jmxPort int,
        lastCheckInTime datetime2,
        name_norm varchar(255),
        name_orig varchar(255),
        nodeIdentifier varchar(255),
        running bit,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_object (
        description varchar(MAX),
        version bigint not null,
        id bigint not null,
        oid varchar(36) not null,
        extId bigint,
        extOid varchar(36),
        extType int,
        primary key (id, oid)
    );

    create table m_object_org_ref (
        object_id bigint not null,
        object_oid varchar(36) not null,
        description varchar(MAX),
        filter varchar(MAX),
        relationLocalPart varchar(255),
        relationNamespace varchar(255),
        targetOid varchar(36),
        type int
    );

    create table m_operation_result (
        owner_oid varchar(36) not null,
        owner_id bigint not null,
        details varchar(MAX),
        localizedMessage varchar(MAX),
        message varchar(MAX),
        messageCode varchar(255),
        operation varchar(MAX),
        params varchar(MAX),
        partialResults varchar(MAX),
        status int,
        token bigint,
        primary key (owner_oid, owner_id)
    );

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
    );

    create table m_org_closure (
        id bigint not null,
        depthValue int,
        ancestor_id bigint,
        ancestor_oid varchar(36),
        descendant_id bigint,
        descendant_oid varchar(36),
        primary key (id)
    );

    create table m_org_org_type (
        org_id bigint not null,
        org_oid varchar(36) not null,
        orgType varchar(255)
    );

    create table m_org_sys_config (
        org_id bigint not null,
        org_oid varchar(36) not null,
        description varchar(MAX),
        filter varchar(MAX),
        relationLocalPart varchar(255),
        relationNamespace varchar(255),
        targetOid varchar(36),
        type int
    );

    create table m_password_policy (
        lifetime varchar(MAX),
        name_norm varchar(255),
        name_orig varchar(255),
        stringPolicy varchar(MAX),
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_reference (
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        targetOid varchar(36) not null,
        description varchar(MAX),
        filter varchar(MAX),
        reference_relationLocalPart varchar(255),
        reference_relationNamespace varchar(255),
        type int,
        primary key (owner_id, owner_oid, targetOid)
    );

    create table m_resource (
        business_administrativeState int,
        capabilities_cachingMetadata varchar(MAX),
        capabilities_configured varchar(MAX),
        capabilities_native varchar(MAX),
        configuration varchar(MAX),
        connectorRef_description varchar(MAX),
        connectorRef_filter varchar(MAX),
        connectorRef_relationLocalPart varchar(255),
        connectorRef_relationNamespace varchar(255),
        connectorRef_targetOid varchar(36),
        connectorRef_type int,
        consistency varchar(MAX),
        name_norm varchar(255),
        name_orig varchar(255),
        namespace varchar(255),
        o16_lastAvailabilityStatus int,
        schemaHandling varchar(MAX),
        scripts varchar(MAX),
        synchronization varchar(MAX),
        xmlSchema varchar(MAX),
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_resource_approver_ref (
        user_id bigint not null,
        user_oid varchar(36) not null,
        description varchar(MAX),
        filter varchar(MAX),
        relationLocalPart varchar(255),
        relationNamespace varchar(255),
        targetOid varchar(36),
        type int
    );

    create table m_resource_shadow (
        enabled bit,
        validFrom datetime2,
        validTo datetime2,
        attemptNumber int,
        dead bit,
        failedOperationType int,
        intent varchar(255),
        name_norm varchar(255),
        name_orig varchar(255),
        objectChange varchar(MAX),
        class_namespace varchar(255),
        class_localPart varchar(255),
        synchronizationSituation int,
        synchronizationTimestamp datetime2,
        id bigint not null,
        oid varchar(36) not null,
        attrId bigint,
        attrOid varchar(36),
        attrType int,
        primary key (id, oid)
    );

    create table m_role (
        approvalExpression varchar(MAX),
        approvalProcess varchar(255),
        approvalSchema varchar(MAX),
        automaticallyApproved varchar(MAX),
        name_norm varchar(255),
        name_orig varchar(255),
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_sync_situation_description (
        shadow_id bigint not null,
        shadow_oid varchar(36) not null,
        chanel varchar(255),
        situation int,
        timestamp datetime2
    );

    create table m_system_configuration (
        connectorFramework varchar(MAX),
        g36 varchar(MAX),
        g23_description varchar(MAX),
        globalPasswordPolicyRef_filter varchar(MAX),
        g23_relationLocalPart varchar(255),
        g23_relationNamespace varchar(255),
        g23_targetOid varchar(36),
        globalPasswordPolicyRef_type int,
        logging varchar(MAX),
        modelHooks varchar(MAX),
        name_norm varchar(255),
        name_orig varchar(255),
        notificationConfiguration varchar(MAX),
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_task (
        binding int,
        canRunOnNode varchar(255),
        category varchar(255),
        claimExpirationTimestamp datetime2,
        exclusivityStatus int,
        executionStatus int,
        handlerUri varchar(255),
        lastRunFinishTimestamp datetime2,
        lastRunStartTimestamp datetime2,
        modelOperationState varchar(MAX),
        name_norm varchar(255),
        name_orig varchar(255),
        nextRunStartTime datetime2,
        node varchar(255),
        objectRef_description varchar(MAX),
        objectRef_filter varchar(MAX),
        objectRef_relationLocalPart varchar(255),
        objectRef_relationNamespace varchar(255),
        objectRef_targetOid varchar(36),
        objectRef_type int,
        otherHandlersUriStack varchar(MAX),
        ownerRef_description varchar(MAX),
        ownerRef_filter varchar(MAX),
        ownerRef_relationLocalPart varchar(255),
        ownerRef_relationNamespace varchar(255),
        ownerRef_targetOid varchar(36),
        ownerRef_type int,
        parent varchar(255),
        progress bigint,
        recurrence int,
        resultStatus int,
        schedule varchar(MAX),
        taskIdentifier varchar(255),
        threadStopAction int,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    );

    create table m_user (
        enabled bit,
        validFrom datetime2,
        validTo datetime2,
        additionalName_norm varchar(255),
        additionalName_orig varchar(255),
        costCenter varchar(255),
        allowedIdmAdminGuiAccess bit,
        passwordXml varchar(MAX),
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
    );

    create table m_user_employee_type (
        user_id bigint not null,
        user_oid varchar(36) not null,
        employeeType varchar(255)
    );

    create table m_user_organizational_unit (
        user_id bigint not null,
        user_oid varchar(36) not null,
        norm varchar(255),
        orig varchar(255)
    );

    create table m_user_template (
        accountConstruction varchar(MAX),
        name_norm varchar(255),
        name_orig varchar(255),
        propertyConstruction varchar(MAX),
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    alter table m_account_shadow 
        add constraint fk_account_shadow 
        foreign key (id, oid) 
        references m_resource_shadow;

    alter table m_any_clob 
        add constraint fk_any_clob 
        foreign key (owner_id, owner_oid, ownerType) 
        references m_any;

    create index iDate on m_any_date (dateValue);

    alter table m_any_date 
        add constraint fk_any_date 
        foreign key (owner_id, owner_oid, ownerType) 
        references m_any;

    create index iLong on m_any_long (longValue);

    alter table m_any_long 
        add constraint fk_any_long 
        foreign key (owner_id, owner_oid, ownerType) 
        references m_any;

    create index iOid on m_any_reference (oidValue);

    alter table m_any_reference 
        add constraint fk_any_reference 
        foreign key (owner_id, owner_oid, ownerType) 
        references m_any;

    create index iString on m_any_string (stringValue);

    alter table m_any_string 
        add constraint fk_any_string 
        foreign key (owner_id, owner_oid, ownerType) 
        references m_any;

    create index iAssignmentEnabled on m_assignment (enabled);

    alter table m_assignment 
        add constraint fk_assignment 
        foreign key (id, oid) 
        references m_container;

    alter table m_assignment 
        add constraint fk_assignment_owner 
        foreign key (owner_id, owner_oid) 
        references m_object;

    alter table m_audit_delta 
        add constraint fk_audit_delta 
        foreign key (RAuditEventRecord_id) 
        references m_audit_event;

    create index iConnectorName on m_connector (name_norm);

    alter table m_connector 
        add constraint fk_connector 
        foreign key (id, oid) 
        references m_object;

    alter table m_connector_host 
        add constraint fk_connector_host 
        foreign key (id, oid) 
        references m_object;

    alter table m_connector_target_system 
        add constraint fk_connector_target_system 
        foreign key (connector_id, connector_oid) 
        references m_connector;

    alter table m_exclusion 
        add constraint fk_exclusion 
        foreign key (id, oid) 
        references m_container;

    alter table m_exclusion 
        add constraint fk_exclusion_owner 
        foreign key (owner_id, owner_oid) 
        references m_object;

    alter table m_generic_object 
        add constraint fk_generic_object 
        foreign key (id, oid) 
        references m_object;

    alter table m_node 
        add constraint fk_node 
        foreign key (id, oid) 
        references m_object;

    alter table m_object 
        add constraint fk_container 
        foreign key (id, oid) 
        references m_container;

    alter table m_object_org_ref 
        add constraint fk_object_org_ref 
        foreign key (object_id, object_oid) 
        references m_object;

    alter table m_operation_result 
        add constraint fk_result_owner 
        foreign key (owner_id, owner_oid) 
        references m_object;

    alter table m_org 
        add constraint fk_org 
        foreign key (id, oid) 
        references m_object;

    create index iDescendant on m_org_closure (descendant_oid, descendant_id);

    create index iAncestor on m_org_closure (ancestor_oid, ancestor_id);

    alter table m_org_closure 
        add constraint fk_descendant 
        foreign key (descendant_id, descendant_oid) 
        references m_object;

    alter table m_org_closure 
        add constraint fk_ancestor 
        foreign key (ancestor_id, ancestor_oid) 
        references m_object;

    alter table m_org_org_type 
        add constraint fk_org_org_type 
        foreign key (org_id, org_oid) 
        references m_org;

    alter table m_org_sys_config 
        add constraint fk_org_unit 
        foreign key (org_id, org_oid) 
        references m_system_configuration;

    alter table m_password_policy 
        add constraint fk_password_policy 
        foreign key (id, oid) 
        references m_object;

    alter table m_reference 
        add constraint fk_reference_owner 
        foreign key (owner_id, owner_oid) 
        references m_container;

    alter table m_resource 
        add constraint fk_resource 
        foreign key (id, oid) 
        references m_object;

    alter table m_resource_approver_ref 
        add constraint fk_resource_approver_ref 
        foreign key (user_id, user_oid) 
        references m_resource;

    create index iResourceObjectShadowEnabled on m_resource_shadow (enabled);

    create index iResourceShadowName on m_resource_shadow (name_norm);

    alter table m_resource_shadow 
        add constraint fk_resource_object_shadow 
        foreign key (id, oid) 
        references m_object;

    alter table m_role 
        add constraint fk_role 
        foreign key (id, oid) 
        references m_object;

    alter table m_sync_situation_description 
        add constraint fk_shadow_sync_situation 
        foreign key (shadow_id, shadow_oid) 
        references m_resource_shadow;

    alter table m_system_configuration 
        add constraint fk_system_configuration 
        foreign key (id, oid) 
        references m_object;

    create index iTaskName on m_task (name_norm);

    alter table m_task 
        add constraint fk_task 
        foreign key (id, oid) 
        references m_object;

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
        add constraint fk_user 
        foreign key (id, oid) 
        references m_object;

    alter table m_user_employee_type 
        add constraint fk_user_employee_type 
        foreign key (user_id, user_oid) 
        references m_user;

    alter table m_user_organizational_unit 
        add constraint fk_user_org_unit 
        foreign key (user_id, user_oid) 
        references m_user;

    alter table m_user_template 
        add constraint fk_user_template 
        foreign key (id, oid) 
        references m_object;

    create table hibernate_sequence (
         next_val bigint 
    );

    insert into hibernate_sequence values ( 1 );
