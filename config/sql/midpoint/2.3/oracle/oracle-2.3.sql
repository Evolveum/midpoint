-- INITRANS added because we use serializable transactions http://docs.oracle.com/cd/B14117_01/appdev.101/b10795/adfns_sq.htm#1025374
-- replace ");" with ") INITRANS 30;"
    create table m_abstract_role (
        approvalExpression clob,
        approvalProcess varchar2(255 char),
        approvalSchema clob,
        automaticallyApproved clob,
        requestable number(1,0),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid)
    ) INITRANS 30;

    create table m_any (
        owner_id number(19,0) not null,
        owner_oid varchar2(36 char) not null,
        owner_type number(10,0) not null,
        primary key (owner_id, owner_oid, owner_type)
    ) INITRANS 30;

    create table m_any_clob (
        checksum varchar2(32 char) not null,
        name_namespace varchar2(255 char) not null,
        name_localPart varchar2(100 char) not null,
        anyContainer_owner_id number(19,0) not null,
        anyContainer_owner_oid varchar2(36 char) not null,
        anyContainer_owner_type number(10,0) not null,
        type_namespace varchar2(255 char) not null,
        type_localPart varchar2(100 char) not null,
        dynamicDef number(1,0),
        clobValue clob,
        valueType number(10,0),
        primary key (checksum, name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart)
    ) INITRANS 30;

    create table m_any_date (
        name_namespace varchar2(255 char) not null,
        name_localPart varchar2(100 char) not null,
        anyContainer_owner_id number(19,0) not null,
        anyContainer_owner_oid varchar2(36 char) not null,
        anyContainer_owner_type number(10,0) not null,
        type_namespace varchar2(255 char) not null,
        type_localPart varchar2(100 char) not null,
        dateValue timestamp not null,
        dynamicDef number(1,0),
        valueType number(10,0),
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, dateValue)
    ) INITRANS 30;

    create table m_any_long (
        name_namespace varchar2(255 char) not null,
        name_localPart varchar2(100 char) not null,
        anyContainer_owner_id number(19,0) not null,
        anyContainer_owner_oid varchar2(36 char) not null,
        anyContainer_owner_type number(10,0) not null,
        type_namespace varchar2(255 char) not null,
        type_localPart varchar2(100 char) not null,
        longValue number(19,0) not null,
        dynamicDef number(1,0),
        valueType number(10,0),
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, longValue)
    ) INITRANS 30;

    create table m_any_poly_string (
        name_namespace varchar2(255 char) not null,
        name_localPart varchar2(100 char) not null,
        anyContainer_owner_id number(19,0) not null,
        anyContainer_owner_oid varchar2(36 char) not null,
        anyContainer_owner_type number(10,0) not null,
        type_namespace varchar2(255 char) not null,
        type_localPart varchar2(100 char) not null,
        orig varchar2(255 char) not null,
        dynamicDef number(1,0),
        norm varchar2(255 char),
        valueType number(10,0),
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, orig)
    ) INITRANS 30;

    create table m_any_reference (
        name_namespace varchar2(255 char) not null,
        name_localPart varchar2(100 char) not null,
        anyContainer_owner_id number(19,0) not null,
        anyContainer_owner_oid varchar2(36 char) not null,
        anyContainer_owner_type number(10,0) not null,
        type_namespace varchar2(255 char) not null,
        type_localPart varchar2(100 char) not null,
        targetoid varchar2(36 char) not null,
        description clob,
        dynamicDef number(1,0),
        filter clob,
        relation_namespace varchar2(255 char),
        relation_localPart varchar2(100 char),
        targetType number(10,0),
        valueType number(10,0),
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, targetoid)
    ) INITRANS 30;

    create table m_any_string (
        name_namespace varchar2(255 char) not null,
        name_localPart varchar2(100 char) not null,
        anyContainer_owner_id number(19,0) not null,
        anyContainer_owner_oid varchar2(36 char) not null,
        anyContainer_owner_type number(10,0) not null,
        type_namespace varchar2(255 char) not null,
        type_localPart varchar2(100 char) not null,
        stringValue varchar2(255 char) not null,
        dynamicDef number(1,0),
        valueType number(10,0),
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, stringValue)
    ) INITRANS 30;

    create table m_assignment (
        accountConstruction clob,
        administrativeStatus number(10,0),
        archiveTimestamp timestamp,
        disableReason varchar2(255 char),
        disableTimestamp timestamp,
        effectiveStatus number(10,0),
        enableTimestamp timestamp,
        validFrom timestamp,
        validTo timestamp,
        validityChangeTimestamp timestamp,
        validityStatus number(10,0),
        assignmentOwner number(10,0),
        construction clob,
        description clob,
        orderValue number(10,0),
        owner_id number(19,0) not null,
        owner_oid varchar2(36 char) not null,
        targetRef_description clob,
        targetRef_filter clob,
        targetRef_relationLocalPart varchar2(100 char),
        targetRef_relationNamespace varchar2(255 char),
        targetRef_targetOid varchar2(36 char),
        targetRef_type number(10,0),
        tenantRef_description clob,
        tenantRef_filter clob,
        tenantRef_relationLocalPart varchar2(100 char),
        tenantRef_relationNamespace varchar2(255 char),
        tenantRef_targetOid varchar2(36 char),
        tenantRef_type number(10,0),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        extId number(19,0),
        extOid varchar2(36 char),
        extType number(10,0),
        primary key (id, oid)
    ) INITRANS 30;

    create table m_audit_delta (
        checksum varchar2(32 char) not null,
        record_id number(19,0) not null,
        context clob,
        delta clob,
        deltaOid varchar2(36 char),
        deltaType number(10,0),
        details clob,
        localizedMessage clob,
        message clob,
        messageCode varchar2(255 char),
        operation clob,
        params clob,
        partialResults clob,
        returns clob,
        status number(10,0),
        token number(19,0),
        primary key (checksum, record_id)
    ) INITRANS 30;

    create table m_audit_event (
        id number(19,0) not null,
        channel varchar2(255 char),
        eventIdentifier varchar2(255 char),
        eventStage number(10,0),
        eventType number(10,0),
        hostIdentifier varchar2(255 char),
        initiatorName varchar2(255 char),
        initiatorOid varchar2(36 char),
        message varchar2(1024 char),
        outcome number(10,0),
        parameter varchar2(255 char),
        result varchar2(255 char),
        sessionIdentifier varchar2(255 char),
        targetName varchar2(255 char),
        targetOid varchar2(36 char),
        targetOwnerName varchar2(255 char),
        targetOwnerOid varchar2(36 char),
        targetType number(10,0),
        taskIdentifier varchar2(255 char),
        taskOID varchar2(255 char),
        timestampValue timestamp,
        primary key (id)
    ) INITRANS 30;

    create table m_authorization (
        decision number(10,0),
        description clob,
        objectSpecification clob,
        owner_id number(19,0) not null,
        owner_oid varchar2(36 char) not null,
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid)
    ) INITRANS 30;

    create table m_authorization_action (
        role_id number(19,0) not null,
        role_oid varchar2(36 char) not null,
        action varchar2(255 char)
    ) INITRANS 30;

    create table m_connector (
        connectorBundle varchar2(255 char),
        connectorHostRef_description clob,
        connectorHostRef_filter clob,
        c16_relationLocalPart varchar2(100 char),
        c16_relationNamespace varchar2(255 char),
        connectorHostRef_targetOid varchar2(36 char),
        connectorHostRef_type number(10,0),
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
    ) INITRANS 30;

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
    ) INITRANS 30;

    create table m_connector_target_system (
        connector_id number(19,0) not null,
        connector_oid varchar2(36 char) not null,
        targetSystemType varchar2(255 char)
    ) INITRANS 30;

    create table m_container (
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid)
    ) INITRANS 30;

    create table m_exclusion (
        description clob,
        owner_id number(19,0) not null,
        owner_oid varchar2(36 char) not null,
        policy number(10,0),
        targetRef_description clob,
        targetRef_filter clob,
        targetRef_relationLocalPart varchar2(100 char),
        targetRef_relationNamespace varchar2(255 char),
        targetRef_targetOid varchar2(36 char),
        targetRef_type number(10,0),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid)
    ) INITRANS 30;

    create table m_focus (
        administrativeStatus number(10,0),
        archiveTimestamp timestamp,
        disableReason varchar2(255 char),
        disableTimestamp timestamp,
        effectiveStatus number(10,0),
        enableTimestamp timestamp,
        validFrom timestamp,
        validTo timestamp,
        validityChangeTimestamp timestamp,
        validityStatus number(10,0),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid)
    ) INITRANS 30;

    create table m_generic_object (
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        objectType varchar2(255 char),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    ) INITRANS 30;

    create table m_metadata (
        owner_id number(19,0) not null,
        owner_oid varchar2(36 char) not null,
        createChannel varchar2(255 char),
        createTimestamp timestamp,
        creatorRef_description clob,
        creatorRef_filter clob,
        creatorRef_relationLocalPart varchar2(100 char),
        creatorRef_relationNamespace varchar2(255 char),
        creatorRef_targetOid varchar2(36 char),
        creatorRef_type number(10,0),
        modifierRef_description clob,
        modifierRef_filter clob,
        modifierRef_relationLocalPart varchar2(100 char),
        modifierRef_relationNamespace varchar2(255 char),
        modifierRef_targetOid varchar2(36 char),
        modifierRef_type number(10,0),
        modifyChannel varchar2(255 char),
        modifyTimestamp timestamp,
        primary key (owner_id, owner_oid)
    ) INITRANS 30;

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
    ) INITRANS 30;

    create table m_object (
        description clob,
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        tenantRef_description clob,
        tenantRef_filter clob,
        tenantRef_relationLocalPart varchar2(100 char),
        tenantRef_relationNamespace varchar2(255 char),
        tenantRef_targetOid varchar2(36 char),
        tenantRef_type number(10,0),
        version number(19,0) not null,
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        extId number(19,0),
        extOid varchar2(36 char),
        extType number(10,0),
        primary key (id, oid)
    ) INITRANS 30;

    create table m_object_template (
        accountConstruction clob,
        mapping clob,
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        type number(10,0),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    ) INITRANS 30;

    create table m_operation_result (
        owner_oid varchar2(36 char) not null,
        owner_id number(19,0) not null,
        context clob,
        details clob,
        localizedMessage clob,
        message clob,
        messageCode varchar2(255 char),
        operation clob,
        params clob,
        partialResults clob,
        returns clob,
        status number(10,0),
        token number(19,0),
        primary key (owner_oid, owner_id)
    ) INITRANS 30;

    create table m_org (
        costCenter varchar2(255 char),
        displayName_norm varchar2(255 char),
        displayName_orig varchar2(255 char),
        identifier varchar2(255 char),
        locality_norm varchar2(255 char),
        locality_orig varchar2(255 char),
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        tenant number(1,0),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    ) INITRANS 30;

    create table m_org_closure (
        id number(19,0) not null,
        ancestor_id number(19,0),
        ancestor_oid varchar2(36 char),
        depthValue number(10,0),
        descendant_id number(19,0),
        descendant_oid varchar2(36 char),
        primary key (id)
    ) INITRANS 30;

    create table m_org_incorrect (
        descendant_oid varchar2(36 char) not null,
        descendant_id number(19,0) not null,
        ancestor_oid varchar2(36 char) not null,
        primary key (descendant_oid, descendant_id, ancestor_oid)
    ) INITRANS 30;

    create table m_org_org_type (
        org_id number(19,0) not null,
        org_oid varchar2(36 char) not null,
        orgType varchar2(255 char)
    ) INITRANS 30;

    create table m_reference (
        reference_type number(10,0) not null,
        owner_id number(19,0) not null,
        owner_oid varchar2(36 char) not null,
        relLocalPart varchar2(100 char) not null,
        relNamespace varchar2(255 char) not null,
        targetOid varchar2(36 char) not null,
        description clob,
        filter clob,
        containerType number(10,0),
        primary key (owner_id, owner_oid, relLocalPart, relNamespace, targetOid)
    ) INITRANS 30;

    create table m_report (
        configuration clob,
        configurationSchema clob,
        dataSource_providerClass varchar2(255 char),
        dataSource_springBean number(1,0),
        export number(10,0),
        field clob,
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        orientation number(10,0),
        parent number(1,0),
        subreport clob,
        template clob,
        templateStyle clob,
        useHibernateSession number(1,0),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    ) INITRANS 30;

    create table m_report_output (
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        reportFilePath varchar2(255 char),
        reportRef_description clob,
        reportRef_filter clob,
        reportRef_relationLocalPart varchar2(100 char),
        reportRef_relationNamespace varchar2(255 char),
        reportRef_targetOid varchar2(36 char),
        reportRef_type number(10,0),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    ) INITRANS 30;

    create table m_resource (
        administrativeState number(10,0),
        capabilities_cachingMetadata clob,
        capabilities_configured clob,
        capabilities_native clob,
        configuration clob,
        connectorRef_description clob,
        connectorRef_filter clob,
        connectorRef_relationLocalPart varchar2(100 char),
        connectorRef_relationNamespace varchar2(255 char),
        connectorRef_targetOid varchar2(36 char),
        connectorRef_type number(10,0),
        consistency clob,
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        namespace varchar2(255 char),
        o16_lastAvailabilityStatus number(10,0),
        projection clob,
        schemaHandling clob,
        scripts clob,
        synchronization clob,
        xmlSchema clob,
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    ) INITRANS 30;

    create table m_role (
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        roleType varchar2(255 char),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    ) INITRANS 30;

    create table m_security_policy (
        authentication clob,
        credentials clob,
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    ) INITRANS 30;

    create table m_shadow (
        administrativeStatus number(10,0),
        archiveTimestamp timestamp,
        disableReason varchar2(255 char),
        disableTimestamp timestamp,
        effectiveStatus number(10,0),
        enableTimestamp timestamp,
        validFrom timestamp,
        validTo timestamp,
        validityChangeTimestamp timestamp,
        validityStatus number(10,0),
        assigned number(1,0),
        attemptNumber number(10,0),
        dead number(1,0),
        exist number(1,0),
        failedOperationType number(10,0),
        fullSynchronizationTimestamp timestamp,
        intent varchar2(255 char),
        iteration number(10,0),
        iterationToken varchar2(255 char),
        kind number(10,0),
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        objectChange clob,
        class_namespace varchar2(255 char),
        class_localPart varchar2(100 char),
        resourceRef_description clob,
        resourceRef_filter clob,
        resourceRef_relationLocalPart varchar2(100 char),
        resourceRef_relationNamespace varchar2(255 char),
        resourceRef_targetOid varchar2(36 char),
        resourceRef_type number(10,0),
        synchronizationSituation number(10,0),
        synchronizationTimestamp timestamp,
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        attrId number(19,0),
        attrOid varchar2(36 char),
        attrType number(10,0),
        primary key (id, oid)
    ) INITRANS 30;

    create table m_sync_situation_description (
        checksum varchar2(32 char) not null,
        shadow_id number(19,0) not null,
        shadow_oid varchar2(36 char) not null,
        chanel varchar2(255 char),
        fullFlag number(1,0),
        situation number(10,0),
        timestampValue timestamp,
        primary key (checksum, shadow_id, shadow_oid)
    ) INITRANS 30;

    create table m_system_configuration (
        cleanupPolicy clob,
        connectorFramework clob,
        d22_description clob,
        defaultUserTemplateRef_filter clob,
        d22_relationLocalPart varchar2(100 char),
        d22_relationNamespace varchar2(255 char),
        d22_targetOid varchar2(36 char),
        defaultUserTemplateRef_type number(10,0),
        g36 clob,
        g23_description clob,
        globalPasswordPolicyRef_filter clob,
        g23_relationLocalPart varchar2(100 char),
        g23_relationNamespace varchar2(255 char),
        g23_targetOid varchar2(36 char),
        globalPasswordPolicyRef_type number(10,0),
        logging clob,
        modelHooks clob,
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        notificationConfiguration clob,
        objectTemplate clob,
        profilingConfiguration clob,
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    ) INITRANS 30;

    create table m_task (
        binding number(10,0),
        canRunOnNode varchar2(255 char),
        category varchar2(255 char),
        completionTimestamp timestamp,
        executionStatus number(10,0),
        expectedTotal number(19,0),
        handlerUri varchar2(255 char),
        lastRunFinishTimestamp timestamp,
        lastRunStartTimestamp timestamp,
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        node varchar2(255 char),
        objectRef_description clob,
        objectRef_filter clob,
        objectRef_relationLocalPart varchar2(100 char),
        objectRef_relationNamespace varchar2(255 char),
        objectRef_targetOid varchar2(36 char),
        objectRef_type number(10,0),
        otherHandlersUriStack clob,
        ownerRef_description clob,
        ownerRef_filter clob,
        ownerRef_relationLocalPart varchar2(100 char),
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
        waitingReason number(10,0),
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid)
    ) INITRANS 30;

    create table m_task_dependent (
        task_id number(19,0) not null,
        task_oid varchar2(36 char) not null,
        dependent varchar2(255 char)
    ) INITRANS 30;

    create table m_trigger (
        handlerUri varchar2(255 char),
        owner_id number(19,0) not null,
        owner_oid varchar2(36 char) not null,
        timestampValue timestamp,
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid)
    ) INITRANS 30;

    create table m_user (
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
        jpegPhoto blob,
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
    ) INITRANS 30;

    create table m_user_employee_type (
        user_id number(19,0) not null,
        user_oid varchar2(36 char) not null,
        employeeType varchar2(255 char)
    ) INITRANS 30;

    create table m_user_organization (
        user_id number(19,0) not null,
        user_oid varchar2(36 char) not null,
        norm varchar2(255 char),
        orig varchar2(255 char)
    ) INITRANS 30;

    create table m_user_organizational_unit (
        user_id number(19,0) not null,
        user_oid varchar2(36 char) not null,
        norm varchar2(255 char),
        orig varchar2(255 char)
    ) INITRANS 30;

    create table m_value_policy (
        lifetime clob,
        name_norm varchar2(255 char),
        name_orig varchar2(255 char),
        stringPolicy clob,
        id number(19,0) not null,
        oid varchar2(36 char) not null,
        primary key (id, oid),
        unique (name_norm)
    ) INITRANS 30;

    create index iRequestable on m_abstract_role (requestable) INITRANS 30;

    alter table m_abstract_role 
        add constraint fk_abstract_role 
        foreign key (id, oid) 
        references m_focus;

    alter table m_any_clob 
        add constraint fk_any_clob 
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type) 
        references m_any;

    create index iDate on m_any_date (dateValue) INITRANS 30;

    alter table m_any_date 
        add constraint fk_any_date 
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type) 
        references m_any;

    create index iLong on m_any_long (longValue) INITRANS 30;

    alter table m_any_long 
        add constraint fk_any_long 
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type) 
        references m_any;

    create index iPolyString on m_any_poly_string (orig) INITRANS 30;

    alter table m_any_poly_string 
        add constraint fk_any_poly_string 
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type) 
        references m_any;

    create index iTargetOid on m_any_reference (targetoid) INITRANS 30;

    alter table m_any_reference 
        add constraint fk_any_reference 
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type) 
        references m_any;

    create index iString on m_any_string (stringValue) INITRANS 30;

    alter table m_any_string 
        add constraint fk_any_string 
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type) 
        references m_any;

    create index iAssignmentAdministrative on m_assignment (administrativeStatus) INITRANS 30;

    create index iAssignmentEffective on m_assignment (effectiveStatus) INITRANS 30;

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

    create index iConnectorNameNorm on m_connector (name_norm) INITRANS 30;

    create index iConnectorNameOrig on m_connector (name_orig) INITRANS 30;

    alter table m_connector 
        add constraint fk_connector 
        foreign key (id, oid) 
        references m_object;

    create index iConnectorHostName on m_connector_host (name_orig) INITRANS 30;

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

    create index iFocusAdministrative on m_focus (administrativeStatus) INITRANS 30;

    create index iFocusEffective on m_focus (effectiveStatus) INITRANS 30;

    alter table m_focus 
        add constraint fk_focus 
        foreign key (id, oid) 
        references m_object;

    create index iGenericObjectName on m_generic_object (name_orig) INITRANS 30;

    alter table m_generic_object 
        add constraint fk_generic_object 
        foreign key (id, oid) 
        references m_object;

    alter table m_metadata 
        add constraint fk_metadata_owner 
        foreign key (owner_id, owner_oid) 
        references m_container;

    create index iNodeName on m_node (name_orig) INITRANS 30;

    alter table m_node 
        add constraint fk_node 
        foreign key (id, oid) 
        references m_object;

    create index iObjectNameOrig on m_object (name_orig) INITRANS 30;

    create index iObjectNameNorm on m_object (name_norm) INITRANS 30;

    alter table m_object 
        add constraint fk_object 
        foreign key (id, oid) 
        references m_container;

    create index iObjectTemplate on m_object_template (name_orig) INITRANS 30;

    alter table m_object_template 
        add constraint fk_object_template 
        foreign key (id, oid) 
        references m_object;

    alter table m_operation_result 
        add constraint fk_result_owner 
        foreign key (owner_id, owner_oid) 
        references m_object;

    create index iOrgName on m_org (name_orig) INITRANS 30;

    alter table m_org 
        add constraint fk_org 
        foreign key (id, oid) 
        references m_abstract_role;

    create index iAncestorDepth on m_org_closure (ancestor_id, ancestor_oid, depthValue) INITRANS 30;

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

    create index iReferenceTargetOid on m_reference (targetOid) INITRANS 30;

    alter table m_reference 
        add constraint fk_reference_owner 
        foreign key (owner_id, owner_oid) 
        references m_container;

    create index iReportParent on m_report (parent) INITRANS 30;

    create index iReportName on m_report (name_orig) INITRANS 30;

    alter table m_report 
        add constraint fk_report 
        foreign key (id, oid) 
        references m_object;

    create index iReportOutputName on m_report_output (name_orig) INITRANS 30;

    alter table m_report_output 
        add constraint fk_reportoutput 
        foreign key (id, oid) 
        references m_object;

    create index iResourceName on m_resource (name_orig) INITRANS 30;

    alter table m_resource 
        add constraint fk_resource 
        foreign key (id, oid) 
        references m_object;

    create index iRoleName on m_role (name_orig) INITRANS 30;

    alter table m_role 
        add constraint fk_role 
        foreign key (id, oid) 
        references m_abstract_role;

    create index iSecurityPolicyName on m_security_policy (name_orig) INITRANS 30;

    alter table m_security_policy 
        add constraint fk_security_policy 
        foreign key (id, oid) 
        references m_object;

    create index iShadowNameOrig on m_shadow (name_orig) INITRANS 30;

    create index iShadowDead on m_shadow (dead) INITRANS 30;

    create index iShadowNameNorm on m_shadow (name_norm) INITRANS 30;

    create index iShadowResourceRef on m_shadow (resourceRef_targetOid) INITRANS 30;

    create index iShadowAdministrative on m_shadow (administrativeStatus) INITRANS 30;

    create index iShadowEffective on m_shadow (effectiveStatus) INITRANS 30;

    alter table m_shadow 
        add constraint fk_shadow 
        foreign key (id, oid) 
        references m_object;

    alter table m_sync_situation_description 
        add constraint fk_shadow_sync_situation 
        foreign key (shadow_id, shadow_oid) 
        references m_shadow;

    create index iSystemConfigurationName on m_system_configuration (name_orig) INITRANS 30;

    alter table m_system_configuration 
        add constraint fk_system_configuration 
        foreign key (id, oid) 
        references m_object;

    create index iTaskNameNameNorm on m_task (name_norm) INITRANS 30;

    create index iParent on m_task (parent) INITRANS 30;

    create index iTaskNameOrig on m_task (name_orig) INITRANS 30;

    alter table m_task 
        add constraint fk_task 
        foreign key (id, oid) 
        references m_object;

    alter table m_task_dependent 
        add constraint fk_task_dependent 
        foreign key (task_id, task_oid) 
        references m_task;

    create index iTriggerTimestamp on m_trigger (timestampValue) INITRANS 30;

    alter table m_trigger 
        add constraint fk_trigger 
        foreign key (id, oid) 
        references m_container;

    alter table m_trigger 
        add constraint fk_trigger_owner 
        foreign key (owner_id, owner_oid) 
        references m_object;

    create index iFullName on m_user (fullName_orig) INITRANS 30;

    create index iLocality on m_user (locality_orig) INITRANS 30;

    create index iHonorificSuffix on m_user (honorificSuffix_orig) INITRANS 30;

    create index iEmployeeNumber on m_user (employeeNumber) INITRANS 30;

    create index iGivenName on m_user (givenName_orig) INITRANS 30;

    create index iFamilyName on m_user (familyName_orig) INITRANS 30;

    create index iAdditionalName on m_user (additionalName_orig) INITRANS 30;

    create index iHonorificPrefix on m_user (honorificPrefix_orig) INITRANS 30;

    create index iUserName on m_user (name_orig) INITRANS 30;

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

    create index iValuePolicy on m_value_policy (name_orig) INITRANS 30;

    alter table m_value_policy 
        add constraint fk_value_policy 
        foreign key (id, oid) 
        references m_object;

    create sequence hibernate_sequence start with 1 increment by 1;
