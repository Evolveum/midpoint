
    create table m_account_shadow (
        accountType varchar2(255 char),
        allowedIdmAdminGuiAccess number(1,0),
        passwordXml clob,
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid)
    );

    create table m_any (
        owner_id number(19,0) not null,
        owner_oid varchar2(36 char) not null,
        ownerType number(10,0) not null,
        primary key (owner_id, owner_oid, ownerType)
    );

    create table m_any_clob (
        owner_id number(19,0) not null,
        owner_oid varchar2(36 char) not null,
        ownerType number(10,0) not null,
        clobValue clob,
        dynamicDef number(1,0),
        name_namespace varchar2(255 char),
        name_localPart varchar2(255 char),
        type_namespace varchar2(255 char),
        type_localPart varchar2(255 char),
        valueType number(10,0)
    );

    create table m_any_date (
        owner_id number(19,0) not null,
        owner_oid varchar2(36 char) not null,
        ownerType number(10,0) not null,
        dateValue timestamp,
        dynamicDef number(1,0),
        name_namespace varchar2(255 char),
        name_localPart varchar2(255 char),
        type_namespace varchar2(255 char),
        type_localPart varchar2(255 char),
        valueType number(10,0)
    );

    create table m_any_long (
        owner_id number(19,0) not null,
        owner_oid varchar2(36 char) not null,
        ownerType number(10,0) not null,
        longValue number(19,0),
        dynamicDef number(1,0),
        name_namespace varchar2(255 char),
        name_localPart varchar2(255 char),
        type_namespace varchar2(255 char),
        type_localPart varchar2(255 char),
        valueType number(10,0)
    );

    create table m_any_reference (
        owner_id number(19,0) not null,
        owner_oid varchar2(36 char) not null,
        ownerType number(10,0) not null,
        oidValue varchar2(255 char),
        dynamicDef number(1,0),
        name_namespace varchar2(255 char),
        name_localPart varchar2(255 char),
        type_namespace varchar2(255 char),
        type_localPart varchar2(255 char),
        valueType number(10,0)
    );

    create table m_any_string (
        owner_id number(19,0) not null,
        owner_oid varchar2(36 char) not null,
        ownerType number(10,0) not null,
        stringValue varchar2(255 char),
        dynamicDef number(1,0),
        name_namespace varchar2(255 char),
        name_localPart varchar2(255 char),
        type_namespace varchar2(255 char),
        type_localPart varchar2(255 char),
        valueType number(10,0)
    );

    create table m_assignment (
        accountConstruction clob,
        enabled number(1,0),
        validFrom timestamp,
        validTo timestamp,
        description clob,
        owner_id number(19,0) not null,
        owner_oid varchar2(36 char) not null,
        targetRef_description clob,
        targetRef_filter clob,
        targetRef_relationLocalPart varchar2(255 char),
        targetRef_relationNamespace varchar2(255 char),
        targetRef_targetOid varchar2(36 char),
        targetRef_type number(10,0),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        extId number(19,0),
        extOid varchar2(36 char),
        extType number(10,0),
        primary key (id, oid)
    );

    create table m_audit_delta (
        RAuditEventRecord_id number(19,0) not null,
        deltas clob
    );

    create table m_audit_event (
        id number(19,0) not null,
        channel varchar2(255 char),
        eventIdentifier varchar2(255 char),
        eventStage number(10,0),
        eventType number(10,0),
        hostIdentifier varchar2(255 char),
        initiator clob,
        outcome number(10,0),
        sessionIdentifier varchar2(255 char),
        target clob,
        targetOwner clob,
        taskIdentifier varchar2(255 char),
        taskOID varchar2(255 char),
        timestampValue number(19,0),
        primary key (id)
    );

    create table m_connector (
        connectorBundle varchar2(255 char),
        connectorType varchar2(255 char),
        connectorVersion varchar2(255 char),
        framework varchar2(255 char),
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        namespace varchar2(255 char),
        xmlSchema clob,
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid)
    );

    create table m_connector_host (
        hostname varchar2(255 char),
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        port varchar2(255 char),
        protectConnection number(1,0),
        sharedSecret clob,
        timeout number(10,0),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_connector_target_system (
        connector_id number(19,0) not null,
        connector_oid varchar2(36 char) not null,
        targetSystemType varchar2(255 char)
    );

    create table m_container (
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid)
    );

    create table m_exclusion (
        description clob,
        owner_id number(19,0) not null,
        owner_oid varchar2(36 char) not null,
        policy number(10,0),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid)
    );

    create table m_generic_object (
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        objectType varchar2(255 char),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_node (
        clusteredNode number(1,0),
        hostname varchar2(255 char),
        internalNodeIdentifier varchar2(255 char),
        jmxPort number(10,0),
        lastCheckInTime timestamp,
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        nodeIdentifier varchar2(255 char),
        running number(1,0),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_object (
        description clob,
        version number(19,0) not null,
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        extId number(19,0),
        extOid varchar2(36 char),
        extType number(10,0),
        primary key (id, oid)
    );

    create table m_object_org_ref (
        object_id number(19,0) not null,
        object_oid varchar2(36 char) not null,
        description clob,
        filter clob,
        relationLocalPart varchar2(255 char),
        relationNamespace varchar2(255 char),
        targetOid varchar2(36 char),
        type number(10,0)
    );

    create table m_operation_result (
        owner_oid varchar2(36 char) not null,
        owner_id number(19,0) not null,
        details clob,
        localizedMessage clob,
        message clob,
        messageCode varchar2(255 char),
        operation clob,
        params clob,
        partialResults clob,
        status number(10,0),
        token number(19,0),
        primary key (owner_oid, owner_id)
    );

    create table m_org (
        costCenter varchar2(255 char),
        displayName_norm varchar2(255 char),
        displayName_orig varchar2(255 char),
        identifier varchar2(255 char),
        locality_norm varchar2(255 char),
        locality_orig varchar2(255 char),
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_org_closure (
        id number(19,0) not null,
        depthValue number(10,0),
        ancestor_id number(19,0),
        ancestor_oid varchar2(36 char),
        descendant_id number(19,0),
        descendant_oid varchar2(36 char),
        primary key (id)
    );

    create table m_org_org_type (
        org_id number(19,0) not null,
        org_oid varchar2(36 char) not null,
        orgType varchar2(255 char)
    );

    create table m_org_sys_config (
        org_id number(19,0) not null,
        org_oid varchar2(36 char) not null,
        description clob,
        filter clob,
        relationLocalPart varchar2(255 char),
        relationNamespace varchar2(255 char),
        targetOid varchar2(36 char),
        type number(10,0)
    );

    create table m_password_policy (
        lifetime clob,
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        stringPolicy clob,
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_reference (
        owner_id number(19,0) not null,
        owner_oid varchar2(36 char) not null,
        targetOid varchar2(36 char) not null,
        description clob,
        filter clob,
        reference_relationLocalPart varchar2(255 char),
        reference_relationNamespace varchar2(255 char),
        type number(10,0),
        primary key (owner_id, owner_oid, targetOid)
    );

    create table m_resource (
        business_administrativeState number(10,0),
        capabilities_cachingMetadata clob,
        capabilities_configured clob,
        capabilities_native clob,
        configuration clob,
        connectorRef_description clob,
        connectorRef_filter clob,
        connectorRef_relationLocalPart varchar2(255 char),
        connectorRef_relationNamespace varchar2(255 char),
        connectorRef_targetOid varchar2(36 char),
        connectorRef_type number(10,0),
        consistency clob,
        lastAvailabilityStatus number(10,0),
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        namespace varchar2(255 char),
        o16_lastAvailabilityStatus number(10,0),
        schemaHandling clob,
        scripts clob,
        synchronization clob,
        xmlSchema clob,
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_resource_approver_ref (
        user_id number(19,0) not null,
        user_oid varchar2(36 char) not null,
        description clob,
        filter clob,
        relationLocalPart varchar2(255 char),
        relationNamespace varchar2(255 char),
        targetOid varchar2(36 char),
        type number(10,0)
    );

    create table m_resource_shadow (
        enabled number(1,0),
        validFrom timestamp,
        validTo timestamp,
        attemptNumber number(10,0),
        dead number(1,0),
        failedOperationType number(10,0),
        intent varchar2(255 char),
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        objectChange clob,
        class_namespace varchar2(255 char),
        class_localPart varchar2(255 char),
        synchronizationSituation number(10,0),
        synchronizationTimestamp timestamp,
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        attrId number(19,0),
        attrOid varchar2(36 char),
        attrType number(10,0),
        primary key (id, oid)
    );

    create table m_role (
        approvalExpression clob,
        approvalProcess varchar2(255 char),
        approvalSchema clob,
        automaticallyApproved clob,
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_sync_situation_description (
        shadow_id number(19,0) not null,
        shadow_oid varchar2(36 char) not null,
        chanel varchar2(255 char),
        situation number(10,0),
        timestamp timestamp
    );

    create table m_system_configuration (
        connectorFramework clob,
        g36 clob,
        g23_description clob,
        globalPasswordPolicyRef_filter clob,
        g23_relationLocalPart varchar2(255 char),
        g23_relationNamespace varchar2(255 char),
        g23_targetOid varchar2(36 char),
        globalPasswordPolicyRef_type number(10,0),
        logging clob,
        modelHooks clob,
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        notificationConfiguration clob,
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_task (
        binding number(10,0),
        canRunOnNode varchar2(255 char),
        category varchar2(255 char),
        claimExpirationTimestamp timestamp,
        exclusivityStatus number(10,0),
        executionStatus number(10,0),
        handlerUri varchar2(255 char),
        lastRunFinishTimestamp timestamp,
        lastRunStartTimestamp timestamp,
        modelOperationState clob,
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        nextRunStartTime timestamp,
        node varchar2(255 char),
        objectRef_description clob,
        objectRef_filter clob,
        objectRef_relationLocalPart varchar2(255 char),
        objectRef_relationNamespace varchar2(255 char),
        objectRef_targetOid varchar2(36 char),
        objectRef_type number(10,0),
        otherHandlersUriStack clob,
        ownerRef_description clob,
        ownerRef_filter clob,
        ownerRef_relationLocalPart varchar2(255 char),
        ownerRef_relationNamespace varchar2(255 char),
        ownerRef_targetOid varchar2(36 char),
        ownerRef_type number(10,0),
        parent varchar2(255 char),
        progress number(19,0),
        recurrence number(10,0),
        resultStatus number(10,0),
        schedule clob,
        taskIdentifier varchar2(255 char),
        threadStopAction number(10,0),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid)
    );

    create table m_user (
        enabled number(1,0),
        validFrom timestamp,
        validTo timestamp,
        additionalName_norm varchar2(255 char),
        additionalName_orig varchar2(255 char),
        costCenter varchar2(255 char),
        allowedIdmAdminGuiAccess number(1,0),
        passwordXml clob,
        emailAddress varchar2(255 char),
        employeeNumber varchar2(255 char),
        familyName_norm varchar2(255 char),
        familyName_orig varchar2(255 char),
        fullName_norm varchar2(255 char),
        fullName_orig varchar2(255 char),
        givenName_norm varchar2(255 char),
        givenName_orig varchar2(255 char),
        honorificPrefix_norm varchar2(255 char),
        honorificPrefix_orig varchar2(255 char),
        honorificSuffix_norm varchar2(255 char),
        honorificSuffix_orig varchar2(255 char),
        locale varchar2(255 char),
        locality_norm varchar2(255 char),
        locality_orig varchar2(255 char),
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        nickName_norm varchar2(255 char),
        nickName_orig varchar2(255 char),
        preferredLanguage varchar2(255 char),
        telephoneNumber varchar2(255 char),
        timezone varchar2(255 char),
        title_norm varchar2(255 char),
        title_orig varchar2(255 char),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_user_employee_type (
        user_id number(19,0) not null,
        user_oid varchar2(36 char) not null,
        employeeType varchar2(255 char)
    );

    create table m_user_organizational_unit (
        user_id number(19,0) not null,
        user_oid varchar2(36 char) not null,
        norm varchar2(255 char),
        orig varchar2(255 char)
    );

    create table m_user_template (
        accountConstruction clob,
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        propertyConstruction clob,
        id number(19,0) not null,
        oid varchar2(36 char) not null,
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

    create sequence hibernate_sequence start with 1 increment by 1;
