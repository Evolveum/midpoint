
    create table m_abstract_role (
        approvalExpression nvarchar(MAX),
        approvalProcess nvarchar(255),
        approvalSchema nvarchar(MAX),
        automaticallyApproved nvarchar(MAX),
        requestable bit,
        id bigint not null,
        oid nvarchar(36) not null,
        primary key (id, oid)
    );

    create table m_any (
        owner_id bigint not null,
        owner_oid nvarchar(36) not null,
        owner_type int not null,
        primary key (owner_id, owner_oid, owner_type)
    );

    create table m_any_clob (
        checksum nvarchar(32) not null,
        name_namespace nvarchar(255) not null,
        name_localPart nvarchar(100) not null,
        anyContainer_owner_id bigint not null,
        anyContainer_owner_oid nvarchar(36) not null,
        anyContainer_owner_type int not null,
        type_namespace nvarchar(255) not null,
        type_localPart nvarchar(100) not null,
        dynamicDef bit,
        clobValue nvarchar(MAX),
        valueType int,
        primary key (checksum, name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart)
    );

    create table m_any_date (
        name_namespace nvarchar(255) not null,
        name_localPart nvarchar(100) not null,
        anyContainer_owner_id bigint not null,
        anyContainer_owner_oid nvarchar(36) not null,
        anyContainer_owner_type int not null,
        type_namespace nvarchar(255) not null,
        type_localPart nvarchar(100) not null,
        dateValue datetime2 not null,
        dynamicDef bit,
        valueType int,
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, dateValue)
    );

    create table m_any_long (
        name_namespace nvarchar(255) not null,
        name_localPart nvarchar(100) not null,
        anyContainer_owner_id bigint not null,
        anyContainer_owner_oid nvarchar(36) not null,
        anyContainer_owner_type int not null,
        type_namespace nvarchar(255) not null,
        type_localPart nvarchar(100) not null,
        longValue bigint not null,
        dynamicDef bit,
        valueType int,
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, longValue)
    );

    create table m_any_poly_string (
        name_namespace nvarchar(255) not null,
        name_localPart nvarchar(100) not null,
        anyContainer_owner_id bigint not null,
        anyContainer_owner_oid nvarchar(36) not null,
        anyContainer_owner_type int not null,
        type_namespace nvarchar(255) not null,
        type_localPart nvarchar(100) not null,
        orig nvarchar(255) not null,
        dynamicDef bit,
        norm nvarchar(255),
        valueType int,
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, orig)
    );

    create table m_any_reference (
        name_namespace nvarchar(255) not null,
        name_localPart nvarchar(100) not null,
        anyContainer_owner_id bigint not null,
        anyContainer_owner_oid nvarchar(36) not null,
        anyContainer_owner_type int not null,
        type_namespace nvarchar(255) not null,
        type_localPart nvarchar(100) not null,
        targetoid nvarchar(36) not null,
        description nvarchar(MAX),
        dynamicDef bit,
        filter nvarchar(MAX),
        relation_namespace nvarchar(255),
        relation_localPart nvarchar(100),
        targetType int,
        valueType int,
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, targetoid)
    );

    create table m_any_string (
        name_namespace nvarchar(255) not null,
        name_localPart nvarchar(100) not null,
        anyContainer_owner_id bigint not null,
        anyContainer_owner_oid nvarchar(36) not null,
        anyContainer_owner_type int not null,
        type_namespace nvarchar(255) not null,
        type_localPart nvarchar(100) not null,
        stringValue nvarchar(255) not null,
        dynamicDef bit,
        valueType int,
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, stringValue)
    );

    create table m_assignment (
        accountConstruction nvarchar(MAX),
        administrativeStatus int,
        archiveTimestamp datetime2,
        disableReason nvarchar(255),
        disableTimestamp datetime2,
        effectiveStatus int,
        enableTimestamp datetime2,
        validFrom datetime2,
        validTo datetime2,
        validityChangeTimestamp datetime2,
        validityStatus int,
        assignmentOwner int,
        construction nvarchar(MAX),
        description nvarchar(MAX),
        orderValue int,
        owner_id bigint not null,
        owner_oid nvarchar(36) not null,
        targetRef_description nvarchar(MAX),
        targetRef_filter nvarchar(MAX),
        targetRef_relationLocalPart nvarchar(100),
        targetRef_relationNamespace nvarchar(255),
        targetRef_targetOid nvarchar(36),
        targetRef_type int,
        tenantRef_description nvarchar(MAX),
        tenantRef_filter nvarchar(MAX),
        tenantRef_relationLocalPart nvarchar(100),
        tenantRef_relationNamespace nvarchar(255),
        tenantRef_targetOid nvarchar(36),
        tenantRef_type int,
        id bigint not null,
        oid nvarchar(36) not null,
        extId bigint,
        extOid nvarchar(36),
        extType int,
        primary key (id, oid)
    );

    create table m_audit_delta (
        checksum nvarchar(32) not null,
        record_id bigint not null,
        context nvarchar(MAX),
        delta nvarchar(MAX),
        deltaOid nvarchar(36),
        deltaType int,
        details nvarchar(MAX),
        localizedMessage nvarchar(MAX),
        message nvarchar(MAX),
        messageCode nvarchar(255),
        operation nvarchar(MAX),
        params nvarchar(MAX),
        partialResults nvarchar(MAX),
        returns nvarchar(MAX),
        status int,
        token bigint,
        primary key (checksum, record_id)
    );

    create table m_audit_event (
        id bigint not null,
        channel nvarchar(255),
        eventIdentifier nvarchar(255),
        eventStage int,
        eventType int,
        hostIdentifier nvarchar(255),
        initiatorName nvarchar(255),
        initiatorOid nvarchar(36),
        message nvarchar(1024),
        outcome int,
        parameter nvarchar(255),
        result nvarchar(255),
        sessionIdentifier nvarchar(255),
        targetName nvarchar(255),
        targetOid nvarchar(36),
        targetOwnerName nvarchar(255),
        targetOwnerOid nvarchar(36),
        targetType int,
        taskIdentifier nvarchar(255),
        taskOID nvarchar(255),
        timestampValue datetime2,
        primary key (id)
    );

    create table m_authorization (
        decision int,
        description nvarchar(MAX),
        owner_id bigint not null,
        owner_oid nvarchar(36) not null,
        id bigint not null,
        oid nvarchar(36) not null,
        primary key (id, oid)
    );

    create table m_authorization_action (
        role_id bigint not null,
        role_oid nvarchar(36) not null,
        action nvarchar(255)
    );

    create table m_connector (
        connectorBundle nvarchar(255),
        connectorHostRef_description nvarchar(MAX),
        connectorHostRef_filter nvarchar(MAX),
        c16_relationLocalPart nvarchar(100),
        c16_relationNamespace nvarchar(255),
        connectorHostRef_targetOid nvarchar(36),
        connectorHostRef_type int,
        connectorType nvarchar(255),
        connectorVersion nvarchar(255),
        framework nvarchar(255),
        name_norm nvarchar(255),
        name_orig nvarchar(255),
        namespace nvarchar(255),
        xmlSchema nvarchar(MAX),
        id bigint not null,
        oid nvarchar(36) not null,
        primary key (id, oid)
    );

    create table m_connector_host (
        hostname nvarchar(255),
        name_norm nvarchar(255),
        name_orig nvarchar(255),
        port nvarchar(255),
        protectConnection bit,
        sharedSecret nvarchar(MAX),
        timeout int,
        id bigint not null,
        oid nvarchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_connector_target_system (
        connector_id bigint not null,
        connector_oid nvarchar(36) not null,
        targetSystemType nvarchar(255)
    );

    create table m_container (
        id bigint not null,
        oid nvarchar(36) not null,
        primary key (id, oid)
    );

    create table m_exclusion (
        description nvarchar(MAX),
        owner_id bigint not null,
        owner_oid nvarchar(36) not null,
        policy int,
        targetRef_description nvarchar(MAX),
        targetRef_filter nvarchar(MAX),
        targetRef_relationLocalPart nvarchar(100),
        targetRef_relationNamespace nvarchar(255),
        targetRef_targetOid nvarchar(36),
        targetRef_type int,
        id bigint not null,
        oid nvarchar(36) not null,
        primary key (id, oid)
    );

    create table m_focus (
        administrativeStatus int,
        archiveTimestamp datetime2,
        disableReason nvarchar(255),
        disableTimestamp datetime2,
        effectiveStatus int,
        enableTimestamp datetime2,
        validFrom datetime2,
        validTo datetime2,
        validityChangeTimestamp datetime2,
        validityStatus int,
        id bigint not null,
        oid nvarchar(36) not null,
        primary key (id, oid)
    );

    create table m_generic_object (
        name_norm nvarchar(255),
        name_orig nvarchar(255),
        objectType nvarchar(255),
        id bigint not null,
        oid nvarchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_metadata (
        owner_id bigint not null,
        owner_oid nvarchar(36) not null,
        createChannel nvarchar(255),
        createTimestamp datetime2,
        creatorRef_description nvarchar(MAX),
        creatorRef_filter nvarchar(MAX),
        creatorRef_relationLocalPart nvarchar(100),
        creatorRef_relationNamespace nvarchar(255),
        creatorRef_targetOid nvarchar(36),
        creatorRef_type int,
        modifierRef_description nvarchar(MAX),
        modifierRef_filter nvarchar(MAX),
        modifierRef_relationLocalPart nvarchar(100),
        modifierRef_relationNamespace nvarchar(255),
        modifierRef_targetOid nvarchar(36),
        modifierRef_type int,
        modifyChannel nvarchar(255),
        modifyTimestamp datetime2,
        primary key (owner_id, owner_oid)
    );

    create table m_node (
        clusteredNode bit,
        hostname nvarchar(255),
        internalNodeIdentifier nvarchar(255),
        jmxPort int,
        lastCheckInTime datetime2,
        name_norm nvarchar(255),
        name_orig nvarchar(255),
        nodeIdentifier nvarchar(255),
        running bit,
        id bigint not null,
        oid nvarchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_object (
        description nvarchar(MAX),
        name_norm nvarchar(255),
        name_orig nvarchar(255),
        tenantRef_description nvarchar(MAX),
        tenantRef_filter nvarchar(MAX),
        tenantRef_relationLocalPart nvarchar(100),
        tenantRef_relationNamespace nvarchar(255),
        tenantRef_targetOid nvarchar(36),
        tenantRef_type int,
        version bigint not null,
        id bigint not null,
        oid nvarchar(36) not null,
        extId bigint,
        extOid nvarchar(36),
        extType int,
        primary key (id, oid)
    );

    create table m_object_template (
        accountConstruction nvarchar(MAX),
        mapping nvarchar(MAX),
        name_norm nvarchar(255),
        name_orig nvarchar(255),
        type int,
        id bigint not null,
        oid nvarchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_operation_result (
        owner_oid nvarchar(36) not null,
        owner_id bigint not null,
        context nvarchar(MAX),
        details nvarchar(MAX),
        localizedMessage nvarchar(MAX),
        message nvarchar(MAX),
        messageCode nvarchar(255),
        operation nvarchar(MAX),
        params nvarchar(MAX),
        partialResults nvarchar(MAX),
        returns nvarchar(MAX),
        status int,
        token bigint,
        primary key (owner_oid, owner_id)
    );

    create table m_org (
        costCenter nvarchar(255),
        displayName_norm nvarchar(255),
        displayName_orig nvarchar(255),
        identifier nvarchar(255),
        locality_norm nvarchar(255),
        locality_orig nvarchar(255),
        name_norm nvarchar(255),
        name_orig nvarchar(255),
        tenant bit,
        id bigint not null,
        oid nvarchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_org_closure (
        id bigint not null,
        ancestor_id bigint,
        ancestor_oid nvarchar(36),
        depthValue int,
        descendant_id bigint,
        descendant_oid nvarchar(36),
        primary key (id)
    );

    create table m_org_incorrect (
        descendant_oid nvarchar(36) not null,
        descendant_id bigint not null,
        ancestor_oid nvarchar(36) not null,
        primary key (descendant_oid, descendant_id, ancestor_oid)
    );

    create table m_org_org_type (
        org_id bigint not null,
        org_oid nvarchar(36) not null,
        orgType nvarchar(255)
    );

    create table m_reference (
        reference_type int not null,
        owner_id bigint not null,
        owner_oid nvarchar(36) not null,
        relLocalPart nvarchar(100) not null,
        relNamespace nvarchar(255) not null,
        targetOid nvarchar(36) not null,
        description nvarchar(MAX),
        filter nvarchar(MAX),
        containerType int,
        primary key (owner_id, owner_oid, relLocalPart, relNamespace, targetOid)
    );

    create table m_report (
        configuration nvarchar(MAX),
        configurationSchema nvarchar(MAX),
        dataSource_providerClass nvarchar(255),
        dataSource_springBean bit,
        export int,
        field nvarchar(MAX),
        name_norm nvarchar(255),
        name_orig nvarchar(255),
        orientation int,
        parent bit,
        subreport nvarchar(MAX),
        template nvarchar(MAX),
        templateStyle nvarchar(MAX),
        useHibernateSession bit,
        id bigint not null,
        oid nvarchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_report_output (
        name_norm nvarchar(255),
        name_orig nvarchar(255),
        reportFilePath nvarchar(255),
        reportRef_description nvarchar(MAX),
        reportRef_filter nvarchar(MAX),
        reportRef_relationLocalPart nvarchar(100),
        reportRef_relationNamespace nvarchar(255),
        reportRef_targetOid nvarchar(36),
        reportRef_type int,
        id bigint not null,
        oid nvarchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_resource (
        administrativeState int,
        capabilities_cachingMetadata nvarchar(MAX),
        capabilities_configured nvarchar(MAX),
        capabilities_native nvarchar(MAX),
        configuration nvarchar(MAX),
        connectorRef_description nvarchar(MAX),
        connectorRef_filter nvarchar(MAX),
        connectorRef_relationLocalPart nvarchar(100),
        connectorRef_relationNamespace nvarchar(255),
        connectorRef_targetOid nvarchar(36),
        connectorRef_type int,
        consistency nvarchar(MAX),
        name_norm nvarchar(255),
        name_orig nvarchar(255),
        namespace nvarchar(255),
        o16_lastAvailabilityStatus int,
        projection nvarchar(MAX),
        schemaHandling nvarchar(MAX),
        scripts nvarchar(MAX),
        synchronization nvarchar(MAX),
        xmlSchema nvarchar(MAX),
        id bigint not null,
        oid nvarchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_role (
        name_norm nvarchar(255),
        name_orig nvarchar(255),
        roleType nvarchar(255),
        id bigint not null,
        oid nvarchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_shadow (
        administrativeStatus int,
        archiveTimestamp datetime2,
        disableReason nvarchar(255),
        disableTimestamp datetime2,
        effectiveStatus int,
        enableTimestamp datetime2,
        validFrom datetime2,
        validTo datetime2,
        validityChangeTimestamp datetime2,
        validityStatus int,
        assigned bit,
        attemptNumber int,
        dead bit,
        exist bit,
        failedOperationType int,
        fullSynchronizationTimestamp datetime2,
        intent nvarchar(255),
        iteration int,
        iterationToken nvarchar(255),
        kind int,
        name_norm nvarchar(255),
        name_orig nvarchar(255),
        objectChange nvarchar(MAX),
        class_namespace nvarchar(255),
        class_localPart nvarchar(100),
        resourceRef_description nvarchar(MAX),
        resourceRef_filter nvarchar(MAX),
        resourceRef_relationLocalPart nvarchar(100),
        resourceRef_relationNamespace nvarchar(255),
        resourceRef_targetOid nvarchar(36),
        resourceRef_type int,
        synchronizationSituation int,
        synchronizationTimestamp datetime2,
        id bigint not null,
        oid nvarchar(36) not null,
        attrId bigint,
        attrOid nvarchar(36),
        attrType int,
        primary key (id, oid)
    );

    create table m_sync_situation_description (
        checksum nvarchar(32) not null,
        shadow_id bigint not null,
        shadow_oid nvarchar(36) not null,
        chanel nvarchar(255),
        fullFlag bit,
        situation int,
        timestampValue datetime2,
        primary key (checksum, shadow_id, shadow_oid)
    );

    create table m_system_configuration (
        cleanupPolicy nvarchar(MAX),
        connectorFramework nvarchar(MAX),
        d22_description nvarchar(MAX),
        defaultUserTemplateRef_filter nvarchar(MAX),
        d22_relationLocalPart nvarchar(100),
        d22_relationNamespace nvarchar(255),
        d22_targetOid nvarchar(36),
        defaultUserTemplateRef_type int,
        g36 nvarchar(MAX),
        g23_description nvarchar(MAX),
        globalPasswordPolicyRef_filter nvarchar(MAX),
        g23_relationLocalPart nvarchar(100),
        g23_relationNamespace nvarchar(255),
        g23_targetOid nvarchar(36),
        globalPasswordPolicyRef_type int,
        logging nvarchar(MAX),
        modelHooks nvarchar(MAX),
        name_norm nvarchar(255),
        name_orig nvarchar(255),
        notificationConfiguration nvarchar(MAX),
        objectTemplate nvarchar(MAX),
        profilingConfiguration nvarchar(MAX),
        id bigint not null,
        oid nvarchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_task (
        binding int,
        canRunOnNode nvarchar(255),
        category nvarchar(255),
        completionTimestamp datetime2,
        executionStatus int,
        expectedTotal bigint,
        handlerUri nvarchar(255),
        lastRunFinishTimestamp datetime2,
        lastRunStartTimestamp datetime2,
        name_norm nvarchar(255),
        name_orig nvarchar(255),
        node nvarchar(255),
        objectRef_description nvarchar(MAX),
        objectRef_filter nvarchar(MAX),
        objectRef_relationLocalPart nvarchar(100),
        objectRef_relationNamespace nvarchar(255),
        objectRef_targetOid nvarchar(36),
        objectRef_type int,
        otherHandlersUriStack nvarchar(MAX),
        ownerRef_description nvarchar(MAX),
        ownerRef_filter nvarchar(MAX),
        ownerRef_relationLocalPart nvarchar(100),
        ownerRef_relationNamespace nvarchar(255),
        ownerRef_targetOid nvarchar(36),
        ownerRef_type int,
        parent nvarchar(255),
        progress bigint,
        recurrence int,
        resultStatus int,
        schedule nvarchar(MAX),
        taskIdentifier nvarchar(255),
        threadStopAction int,
        waitingReason int,
        id bigint not null,
        oid nvarchar(36) not null,
        primary key (id, oid)
    );

    create table m_task_dependent (
        task_id bigint not null,
        task_oid nvarchar(36) not null,
        dependent nvarchar(255)
    );

    create table m_trigger (
        handlerUri nvarchar(255),
        owner_id bigint not null,
        owner_oid nvarchar(36) not null,
        timestampValue datetime2,
        id bigint not null,
        oid nvarchar(36) not null,
        primary key (id, oid)
    );

    create table m_user (
        additionalName_norm nvarchar(255),
        additionalName_orig nvarchar(255),
        costCenter nvarchar(255),
        allowedIdmAdminGuiAccess bit,
        passwordXml nvarchar(MAX),
        emailAddress nvarchar(255),
        employeeNumber nvarchar(255),
        familyName_norm nvarchar(255),
        familyName_orig nvarchar(255),
        fullName_norm nvarchar(255),
        fullName_orig nvarchar(255),
        givenName_norm nvarchar(255),
        givenName_orig nvarchar(255),
        honorificPrefix_norm nvarchar(255),
        honorificPrefix_orig nvarchar(255),
        honorificSuffix_norm nvarchar(255),
        honorificSuffix_orig nvarchar(255),
        jpegPhoto varbinary(MAX),
        locale nvarchar(255),
        locality_norm nvarchar(255),
        locality_orig nvarchar(255),
        name_norm nvarchar(255),
        name_orig nvarchar(255),
        nickName_norm nvarchar(255),
        nickName_orig nvarchar(255),
        preferredLanguage nvarchar(255),
        telephoneNumber nvarchar(255),
        timezone nvarchar(255),
        title_norm nvarchar(255),
        title_orig nvarchar(255),
        id bigint not null,
        oid nvarchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_user_employee_type (
        user_id bigint not null,
        user_oid nvarchar(36) not null,
        employeeType nvarchar(255)
    );

    create table m_user_organization (
        user_id bigint not null,
        user_oid nvarchar(36) not null,
        norm nvarchar(255),
        orig nvarchar(255)
    );

    create table m_user_organizational_unit (
        user_id bigint not null,
        user_oid nvarchar(36) not null,
        norm nvarchar(255),
        orig nvarchar(255)
    );

    create table m_value_policy (
        lifetime nvarchar(MAX),
        name_norm nvarchar(255),
        name_orig nvarchar(255),
        stringPolicy nvarchar(MAX),
        id bigint not null,
        oid nvarchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create index iRequestable on m_abstract_role (requestable);

    alter table m_abstract_role 
        add constraint fk_abstract_role 
        foreign key (id, oid) 
        references m_focus;

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

    create index iConnectorNameNorm on m_connector (name_norm);

    create index iConnectorNameOrig on m_connector (name_orig);

    alter table m_connector 
        add constraint fk_connector 
        foreign key (id, oid) 
        references m_object;

    create index iConnectorHostName on m_connector_host (name_orig);

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

    create index iGenericObjectName on m_generic_object (name_orig);

    alter table m_generic_object 
        add constraint fk_generic_object 
        foreign key (id, oid) 
        references m_object;

    alter table m_metadata 
        add constraint fk_metadata_owner 
        foreign key (owner_id, owner_oid) 
        references m_container;

    create index iNodeName on m_node (name_orig);

    alter table m_node 
        add constraint fk_node 
        foreign key (id, oid) 
        references m_object;

    create index iObject on m_object (name_orig);

    alter table m_object 
        add constraint fk_object 
        foreign key (id, oid) 
        references m_container;

    create index iObjectTemplate on m_object_template (name_orig);

    alter table m_object_template 
        add constraint fk_object_template 
        foreign key (id, oid) 
        references m_object;

    alter table m_operation_result 
        add constraint fk_result_owner 
        foreign key (owner_id, owner_oid) 
        references m_object;

    create index iOrgName on m_org (name_orig);

    alter table m_org 
        add constraint fk_org 
        foreign key (id, oid) 
        references m_abstract_role;

    create index iAncestorDepth on m_org_closure (ancestor_id, ancestor_oid, depthValue);

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

    create index iReferenceTargetOid on m_reference (targetOid);

    alter table m_reference 
        add constraint fk_reference_owner 
        foreign key (owner_id, owner_oid) 
        references m_container;

    create index iReportParent on m_report (parent);

    create index iReportName on m_report (name_orig);

    alter table m_report 
        add constraint fk_report 
        foreign key (id, oid) 
        references m_object;

    create index iReportOutputName on m_report_output (name_orig);

    alter table m_report_output 
        add constraint fk_reportoutput 
        foreign key (id, oid) 
        references m_object;

    create index iResourceName on m_resource (name_orig);

    alter table m_resource 
        add constraint fk_resource 
        foreign key (id, oid) 
        references m_object;

    create index iRoleName on m_role (name_orig);

    alter table m_role 
        add constraint fk_role 
        foreign key (id, oid) 
        references m_abstract_role;

    create index iShadowNameOrig on m_shadow (name_orig);

    create index iShadowDead on m_shadow (dead);

    create index iShadowNameNorm on m_shadow (name_norm);

    create index iShadowResourceRef on m_shadow (resourceRef_targetOid);

    create index iShadowAdministrative on m_shadow (administrativeStatus);

    create index iShadowEffective on m_shadow (effectiveStatus);

    alter table m_shadow 
        add constraint fk_shadow 
        foreign key (id, oid) 
        references m_object;

    alter table m_sync_situation_description 
        add constraint fk_shadow_sync_situation 
        foreign key (shadow_id, shadow_oid) 
        references m_shadow;

    create index iSystemConfigurationName on m_system_configuration (name_orig);

    alter table m_system_configuration 
        add constraint fk_system_configuration 
        foreign key (id, oid) 
        references m_object;

    create index iTaskNameNameNorm on m_task (name_norm);

    create index iParent on m_task (parent);

    create index iTaskNameOrig on m_task (name_orig);

    alter table m_task 
        add constraint fk_task 
        foreign key (id, oid) 
        references m_object;

    alter table m_task_dependent 
        add constraint fk_task_dependent 
        foreign key (task_id, task_oid) 
        references m_task;

    create index iTriggerTimestamp on m_trigger (timestampValue);

    alter table m_trigger 
        add constraint fk_trigger 
        foreign key (id, oid) 
        references m_container;

    alter table m_trigger 
        add constraint fk_trigger_owner 
        foreign key (owner_id, owner_oid) 
        references m_object;

    create index iFullName on m_user (fullName_orig);

    create index iLocality on m_user (locality_orig);

    create index iHonorificSuffix on m_user (honorificSuffix_orig);

    create index iEmployeeNumber on m_user (employeeNumber);

    create index iGivenName on m_user (givenName_orig);

    create index iFamilyName on m_user (familyName_orig);

    create index iAdditionalName on m_user (additionalName_orig);

    create index iHonorificPrefix on m_user (honorificPrefix_orig);

    create index iUserName on m_user (name_orig);

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

    create index iValuePolicy on m_value_policy (name_orig);

    alter table m_value_policy 
        add constraint fk_value_policy 
        foreign key (id, oid) 
        references m_object;

    create table hibernate_sequence (
         next_val bigint 
    );

    insert into hibernate_sequence values ( 1 );
