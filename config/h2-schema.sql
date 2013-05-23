
    create table m_abstract_role (
        approvalExpression clob,
        approvalProcess varchar(255),
        approvalSchema clob,
        automaticallyApproved clob,
        requestable boolean,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    );

    create table m_account_shadow (
        accountType varchar(255),
        allowedIdmAdminGuiAccess boolean,
        passwordXml clob,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    );

    create table m_any (
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        owner_type integer not null,
        primary key (owner_id, owner_oid, owner_type)
    );

    create table m_any_clob (
        checksum varchar(32) not null,
        name_namespace varchar(255) not null,
        name_localPart varchar(100) not null,
        anyContainer_owner_id bigint not null,
        anyContainer_owner_oid varchar(36) not null,
        anyContainer_owner_type integer not null,
        type_namespace varchar(255) not null,
        type_localPart varchar(100) not null,
        dynamicDef boolean,
        clobValue clob,
        valueType integer,
        primary key (checksum, name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart)
    );

    create table m_any_date (
        name_namespace varchar(255) not null,
        name_localPart varchar(100) not null,
        anyContainer_owner_id bigint not null,
        anyContainer_owner_oid varchar(36) not null,
        anyContainer_owner_type integer not null,
        type_namespace varchar(255) not null,
        type_localPart varchar(100) not null,
        dateValue timestamp not null,
        dynamicDef boolean,
        valueType integer,
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, dateValue)
    );

    create table m_any_long (
        name_namespace varchar(255) not null,
        name_localPart varchar(100) not null,
        anyContainer_owner_id bigint not null,
        anyContainer_owner_oid varchar(36) not null,
        anyContainer_owner_type integer not null,
        type_namespace varchar(255) not null,
        type_localPart varchar(100) not null,
        longValue bigint not null,
        dynamicDef boolean,
        valueType integer,
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, longValue)
    );

    create table m_any_poly_string (
        name_namespace varchar(255) not null,
        name_localPart varchar(100) not null,
        anyContainer_owner_id bigint not null,
        anyContainer_owner_oid varchar(36) not null,
        anyContainer_owner_type integer not null,
        type_namespace varchar(255) not null,
        type_localPart varchar(100) not null,
        orig varchar(255) not null,
        dynamicDef boolean,
        norm varchar(255),
        valueType integer,
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, orig)
    );

    create table m_any_reference (
        name_namespace varchar(255) not null,
        name_localPart varchar(100) not null,
        anyContainer_owner_id bigint not null,
        anyContainer_owner_oid varchar(36) not null,
        anyContainer_owner_type integer not null,
        type_namespace varchar(255) not null,
        type_localPart varchar(100) not null,
        targetoid varchar(36) not null,
        description clob,
        dynamicDef boolean,
        filter clob,
        relation_namespace varchar(255),
        relation_localPart varchar(100),
        targetType integer,
        valueType integer,
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, targetoid)
    );

    create table m_any_string (
        name_namespace varchar(255) not null,
        name_localPart varchar(100) not null,
        anyContainer_owner_id bigint not null,
        anyContainer_owner_oid varchar(36) not null,
        anyContainer_owner_type integer not null,
        type_namespace varchar(255) not null,
        type_localPart varchar(100) not null,
        stringValue varchar(255) not null,
        dynamicDef boolean,
        valueType integer,
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, stringValue)
    );

    create table m_assignment (
        accountConstruction clob,
        administrativeStatus integer,
        archiveTimestamp timestamp,
        disableTimestamp timestamp,
        effectiveStatus integer,
        enableTimestamp timestamp,
        validFrom timestamp,
        validTo timestamp,
        validityChangeTimestamp timestamp,
        validityStatus integer,
        assignmentOwner integer,
        construction clob,
        description clob,
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        targetRef_description clob,
        targetRef_filter clob,
        targetRef_relationLocalPart varchar(100),
        targetRef_relationNamespace varchar(255),
        targetRef_targetOid varchar(36),
        targetRef_type integer,
        id bigint not null,
        oid varchar(36) not null,
        extId bigint,
        extOid varchar(36),
        extType integer,
        primary key (id, oid)
    );

    create table m_audit_delta (
        checksum varchar(32) not null,
        record_id bigint not null,
        delta clob,
        details clob,
        localizedMessage clob,
        message clob,
        messageCode varchar(255),
        operation clob,
        params clob,
        partialResults clob,
        status integer,
        token bigint,
        primary key (checksum, record_id)
    );

    create table m_audit_event (
        id bigint not null,
        channel varchar(255),
        eventIdentifier varchar(255),
        eventStage integer,
        eventType integer,
        hostIdentifier varchar(255),
        initiatorName varchar(255),
        initiatorOid varchar(36),
        message varchar(255),
        outcome integer,
        parameter varchar(255),
        sessionIdentifier varchar(255),
        targetName varchar(255),
        targetOid varchar(36),
        targetOwnerName varchar(255),
        targetOwnerOid varchar(36),
        targetType integer,
        taskIdentifier varchar(255),
        taskOID varchar(255),
        timestampValue timestamp,
        primary key (id)
    );

    create table m_authorization (
        decision integer,
        description clob,
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    );

    create table m_authorization_action (
        role_id bigint not null,
        role_oid varchar(36) not null,
        action varchar(255)
    );

    create table m_connector (
        connectorBundle varchar(255),
        connectorHostRef_description clob,
        connectorHostRef_filter clob,
        c16_relationLocalPart varchar(100),
        c16_relationNamespace varchar(255),
        connectorHostRef_targetOid varchar(36),
        connectorHostRef_type integer,
        connectorType varchar(255),
        connectorVersion varchar(255),
        framework varchar(255),
        name_norm varchar(255),
        name_orig varchar(255),
        namespace varchar(255),
        xmlSchema clob,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    );

    create table m_connector_host (
        hostname varchar(255),
        name_norm varchar(255),
        name_orig varchar(255),
        port varchar(255),
        protectConnection boolean,
        sharedSecret clob,
        timeout integer,
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
        description clob,
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        policy integer,
        targetRef_description clob,
        targetRef_filter clob,
        targetRef_relationLocalPart varchar(100),
        targetRef_relationNamespace varchar(255),
        targetRef_targetOid varchar(36),
        targetRef_type integer,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    );

    create table m_focus (
        administrativeStatus integer,
        archiveTimestamp timestamp,
        disableTimestamp timestamp,
        effectiveStatus integer,
        enableTimestamp timestamp,
        validFrom timestamp,
        validTo timestamp,
        validityChangeTimestamp timestamp,
        validityStatus integer,
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

    create table m_metadata (
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        createChannel varchar(255),
        createTimestamp timestamp,
        creatorRef_description clob,
        creatorRef_filter clob,
        creatorRef_relationLocalPart varchar(100),
        creatorRef_relationNamespace varchar(255),
        creatorRef_targetOid varchar(36),
        creatorRef_type integer,
        modifierRef_description clob,
        modifierRef_filter clob,
        modifierRef_relationLocalPart varchar(100),
        modifierRef_relationNamespace varchar(255),
        modifierRef_targetOid varchar(36),
        modifierRef_type integer,
        modifyChannel varchar(255),
        modifyTimestamp timestamp,
        primary key (owner_id, owner_oid)
    );

    create table m_node (
        clusteredNode boolean,
        hostname varchar(255),
        internalNodeIdentifier varchar(255),
        jmxPort integer,
        lastCheckInTime timestamp,
        name_norm varchar(255),
        name_orig varchar(255),
        nodeIdentifier varchar(255),
        running boolean,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_object (
        description clob,
        version bigint not null,
        id bigint not null,
        oid varchar(36) not null,
        extId bigint,
        extOid varchar(36),
        extType integer,
        primary key (id, oid)
    );

    create table m_object_template (
        accountConstruction clob,
        mapping clob,
        name_norm varchar(255),
        name_orig varchar(255),
        type integer,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_operation_result (
        owner_oid varchar(36) not null,
        owner_id bigint not null,
        details clob,
        localizedMessage clob,
        message clob,
        messageCode varchar(255),
        operation clob,
        params clob,
        partialResults clob,
        status integer,
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
        ancestor_id bigint,
        ancestor_oid varchar(36),
        depthValue integer,
        descendant_id bigint,
        descendant_oid varchar(36),
        primary key (id)
    );

    create table m_org_incorrect (
        descendant_oid varchar(36) not null,
        descendant_id bigint not null,
        ancestor_oid varchar(36) not null,
        primary key (descendant_oid, descendant_id, ancestor_oid)
    );

    create table m_org_org_type (
        org_id bigint not null,
        org_oid varchar(36) not null,
        orgType varchar(255)
    );

    create table m_password_policy (
        lifetime clob,
        name_norm varchar(255),
        name_orig varchar(255),
        stringPolicy clob,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_reference (
        reference_type integer not null,
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        relLocalPart varchar(100) not null,
        relNamespace varchar(255) not null,
        targetOid varchar(36) not null,
        description clob,
        filter clob,
        containerType integer,
        primary key (owner_id, owner_oid, relLocalPart, relNamespace, targetOid)
    );

    create table m_resource (
        administrativeState integer,
        capabilities_cachingMetadata clob,
        capabilities_configured clob,
        capabilities_native clob,
        configuration clob,
        connectorRef_description clob,
        connectorRef_filter clob,
        connectorRef_relationLocalPart varchar(100),
        connectorRef_relationNamespace varchar(255),
        connectorRef_targetOid varchar(36),
        connectorRef_type integer,
        consistency clob,
        name_norm varchar(255),
        name_orig varchar(255),
        namespace varchar(255),
        o16_lastAvailabilityStatus integer,
        projection clob,
        schemaHandling clob,
        scripts clob,
        synchronization clob,
        xmlSchema clob,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_role (
        name_norm varchar(255),
        name_orig varchar(255),
        roleType varchar(255),
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_shadow (
        administrativeStatus integer,
        archiveTimestamp timestamp,
        disableTimestamp timestamp,
        effectiveStatus integer,
        enableTimestamp timestamp,
        validFrom timestamp,
        validTo timestamp,
        validityChangeTimestamp timestamp,
        validityStatus integer,
        assigned boolean,
        attemptNumber integer,
        dead boolean,
        exist boolean,
        failedOperationType integer,
        intent varchar(255),
        kind integer,
        name_norm varchar(255),
        name_orig varchar(255),
        objectChange clob,
        class_namespace varchar(255),
        class_localPart varchar(100),
        resourceRef_description clob,
        resourceRef_filter clob,
        resourceRef_relationLocalPart varchar(100),
        resourceRef_relationNamespace varchar(255),
        resourceRef_targetOid varchar(36),
        resourceRef_type integer,
        synchronizationSituation integer,
        synchronizationTimestamp timestamp,
        id bigint not null,
        oid varchar(36) not null,
        attrId bigint,
        attrOid varchar(36),
        attrType integer,
        primary key (id, oid)
    );

    create table m_sync_situation_description (
        checksum varchar(32) not null,
        shadow_id bigint not null,
        shadow_oid varchar(36) not null,
        chanel varchar(255),
        situation integer,
        timestampValue timestamp,
        primary key (checksum, shadow_id, shadow_oid)
    );

    create table m_system_configuration (
        cleanupPolicy clob,
        connectorFramework clob,
        d22_description clob,
        defaultUserTemplateRef_filter clob,
        d22_relationLocalPart varchar(100),
        d22_relationNamespace varchar(255),
        d22_targetOid varchar(36),
        defaultUserTemplateRef_type integer,
        g36 clob,
        g23_description clob,
        globalPasswordPolicyRef_filter clob,
        g23_relationLocalPart varchar(100),
        g23_relationNamespace varchar(255),
        g23_targetOid varchar(36),
        globalPasswordPolicyRef_type integer,
        logging clob,
        modelHooks clob,
        name_norm varchar(255),
        name_orig varchar(255),
        notificationConfiguration clob,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_task (
        binding integer,
        canRunOnNode varchar(255),
        category varchar(255),
        completionTimestamp timestamp,
        executionStatus integer,
        handlerUri varchar(255),
        lastRunFinishTimestamp timestamp,
        lastRunStartTimestamp timestamp,
        name_norm varchar(255),
        name_orig varchar(255),
        node varchar(255),
        objectRef_description clob,
        objectRef_filter clob,
        objectRef_relationLocalPart varchar(100),
        objectRef_relationNamespace varchar(255),
        objectRef_targetOid varchar(36),
        objectRef_type integer,
        otherHandlersUriStack clob,
        ownerRef_description clob,
        ownerRef_filter clob,
        ownerRef_relationLocalPart varchar(100),
        ownerRef_relationNamespace varchar(255),
        ownerRef_targetOid varchar(36),
        ownerRef_type integer,
        parent varchar(255),
        progress bigint,
        recurrence integer,
        resultStatus integer,
        schedule clob,
        taskIdentifier varchar(255),
        threadStopAction integer,
        waitingReason integer,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    );

    create table m_task_dependent (
        task_id bigint not null,
        task_oid varchar(36) not null,
        dependent varchar(255)
    );

    create table m_trigger (
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        handlerUri varchar(255),
        timestamp timestamp,
        primary key (owner_id, owner_oid)
    );

    create table m_user (
        additionalName_norm varchar(255),
        additionalName_orig varchar(255),
        costCenter varchar(255),
        allowedIdmAdminGuiAccess boolean,
        passwordXml clob,
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

    create table m_user_organization (
        user_id bigint not null,
        user_oid varchar(36) not null,
        norm varchar(255),
        orig varchar(255)
    );

    create table m_user_organizational_unit (
        user_id bigint not null,
        user_oid varchar(36) not null,
        norm varchar(255),
        orig varchar(255)
    );

    create index iRequestable on m_abstract_role (requestable);

    alter table m_abstract_role 
        add constraint fk_abstract_role 
        foreign key (id, oid) 
        references m_focus;

    alter table m_account_shadow 
        add constraint fk_account_shadow 
        foreign key (id, oid) 
        references m_shadow;

    alter table m_any_clob 
        add constraint fk_any_clob 
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type) 
        references m_any;

    create index iDate on m_any_date (dateValue);

    alter table m_any_date 
        add constraint fk_any_date 
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type) 
        references m_any;

    create index iLong on m_any_long (longValue);

    alter table m_any_long 
        add constraint fk_any_long 
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type) 
        references m_any;

    create index iPolyString on m_any_poly_string (orig);

    alter table m_any_poly_string 
        add constraint fk_any_poly_string 
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type) 
        references m_any;

    create index iTargetOid on m_any_reference (targetoid);

    alter table m_any_reference 
        add constraint fk_any_reference 
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type) 
        references m_any;

    create index iString on m_any_string (stringValue);

    alter table m_any_string 
        add constraint fk_any_string 
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type) 
        references m_any;

    create index iAssignmentAdministrative on m_assignment (administrativeStatus);

    create index iAssignmentEffective on m_assignment (effectiveStatus);

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
        foreign key (record_id) 
        references m_audit_event;

    alter table m_authorization 
        add constraint fk_authorization 
        foreign key (id, oid) 
        references m_container;

    alter table m_authorization 
        add constraint fk_authorization_owner 
        foreign key (owner_id, owner_oid) 
        references m_object;

    alter table m_authorization_action 
        add constraint fk_authorization_action 
        foreign key (role_id, role_oid) 
        references m_authorization;

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

    create index iFocusAdministrative on m_focus (administrativeStatus);

    create index iFocusEffective on m_focus (effectiveStatus);

    alter table m_focus 
        add constraint fk_focus 
        foreign key (id, oid) 
        references m_object;

    alter table m_generic_object 
        add constraint fk_generic_object 
        foreign key (id, oid) 
        references m_object;

    alter table m_metadata 
        add constraint fk_metadata_owner 
        foreign key (owner_id, owner_oid) 
        references m_container;

    alter table m_node 
        add constraint fk_node 
        foreign key (id, oid) 
        references m_object;

    alter table m_object 
        add constraint fk_object 
        foreign key (id, oid) 
        references m_container;

    alter table m_object_template 
        add constraint fk_object_template 
        foreign key (id, oid) 
        references m_object;

    alter table m_operation_result 
        add constraint fk_result_owner 
        foreign key (owner_id, owner_oid) 
        references m_object;

    alter table m_org 
        add constraint fk_org 
        foreign key (id, oid) 
        references m_abstract_role;

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

    alter table m_role 
        add constraint fk_role 
        foreign key (id, oid) 
        references m_abstract_role;

    create index iShadowResourceRef on m_shadow (resourceRef_targetOid);

    create index iShadowAdministrative on m_shadow (administrativeStatus);

    create index iShadowEffective on m_shadow (effectiveStatus);

    create index iShadowName on m_shadow (name_norm);

    alter table m_shadow 
        add constraint fk_shadow 
        foreign key (id, oid) 
        references m_object;

    alter table m_sync_situation_description 
        add constraint fk_shadow_sync_situation 
        foreign key (shadow_id, shadow_oid) 
        references m_shadow;

    alter table m_system_configuration 
        add constraint fk_system_configuration 
        foreign key (id, oid) 
        references m_object;

    create index iTaskName on m_task (name_norm);

    alter table m_task 
        add constraint fk_task 
        foreign key (id, oid) 
        references m_object;

    alter table m_task_dependent 
        add constraint fk_task_dependent 
        foreign key (task_id, task_oid) 
        references m_task;

    create index iTimestamp on m_trigger (timestamp);

    alter table m_trigger 
        add constraint FK6E863FE68FEF355 
        foreign key (owner_id, owner_oid) 
        references m_object;

    alter table m_trigger 
        add constraint fk_trigger_owner 
        foreign key (owner_id, owner_oid) 
        references m_container;

    create index iFullName on m_user (fullName_norm);

    create index iLocality on m_user (locality_norm);

    create index iHonorificSuffix on m_user (honorificSuffix_norm);

    create index iEmployeeNumber on m_user (employeeNumber);

    create index iGivenName on m_user (givenName_norm);

    create index iFamilyName on m_user (familyName_norm);

    create index iAdditionalName on m_user (additionalName_norm);

    create index iHonorificPrefix on m_user (honorificPrefix_norm);

    alter table m_user 
        add constraint fk_user 
        foreign key (id, oid) 
        references m_focus;

    alter table m_user_employee_type 
        add constraint fk_user_employee_type 
        foreign key (user_id, user_oid) 
        references m_user;

    alter table m_user_organization 
        add constraint fk_user_organization 
        foreign key (user_id, user_oid) 
        references m_user;

    alter table m_user_organizational_unit 
        add constraint fk_user_org_unit 
        foreign key (user_id, user_oid) 
        references m_user;

    create sequence hibernate_sequence start with 1 increment by 1;
