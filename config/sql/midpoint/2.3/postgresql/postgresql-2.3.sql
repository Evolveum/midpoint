
    create table m_abstract_role (
        approvalExpression text,
        approvalProcess varchar(255),
        approvalSchema text,
        automaticallyApproved text,
        requestable boolean,
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid)
    );

    create table m_any (
        owner_id int8 not null,
        owner_oid varchar(36) not null,
        owner_type int4 not null,
        primary key (owner_id, owner_oid, owner_type)
    );

    create table m_any_clob (
        checksum varchar(32) not null,
        name_namespace varchar(255) not null,
        name_localPart varchar(100) not null,
        anyContainer_owner_id int8 not null,
        anyContainer_owner_oid varchar(36) not null,
        anyContainer_owner_type int4 not null,
        type_namespace varchar(255) not null,
        type_localPart varchar(100) not null,
        dynamicDef boolean,
        clobValue text,
        valueType int4,
        primary key (checksum, name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart)
    );

    create table m_any_date (
        name_namespace varchar(255) not null,
        name_localPart varchar(100) not null,
        anyContainer_owner_id int8 not null,
        anyContainer_owner_oid varchar(36) not null,
        anyContainer_owner_type int4 not null,
        type_namespace varchar(255) not null,
        type_localPart varchar(100) not null,
        dateValue timestamp not null,
        dynamicDef boolean,
        valueType int4,
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, dateValue)
    );

    create table m_any_long (
        name_namespace varchar(255) not null,
        name_localPart varchar(100) not null,
        anyContainer_owner_id int8 not null,
        anyContainer_owner_oid varchar(36) not null,
        anyContainer_owner_type int4 not null,
        type_namespace varchar(255) not null,
        type_localPart varchar(100) not null,
        longValue int8 not null,
        dynamicDef boolean,
        valueType int4,
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, longValue)
    );

    create table m_any_poly_string (
        name_namespace varchar(255) not null,
        name_localPart varchar(100) not null,
        anyContainer_owner_id int8 not null,
        anyContainer_owner_oid varchar(36) not null,
        anyContainer_owner_type int4 not null,
        type_namespace varchar(255) not null,
        type_localPart varchar(100) not null,
        orig varchar(255) not null,
        dynamicDef boolean,
        norm varchar(255),
        valueType int4,
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, orig)
    );

    create table m_any_reference (
        name_namespace varchar(255) not null,
        name_localPart varchar(100) not null,
        anyContainer_owner_id int8 not null,
        anyContainer_owner_oid varchar(36) not null,
        anyContainer_owner_type int4 not null,
        type_namespace varchar(255) not null,
        type_localPart varchar(100) not null,
        targetoid varchar(36) not null,
        description text,
        dynamicDef boolean,
        filter text,
        relation_namespace varchar(255),
        relation_localPart varchar(100),
        targetType int4,
        valueType int4,
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, targetoid)
    );

    create table m_any_string (
        name_namespace varchar(255) not null,
        name_localPart varchar(100) not null,
        anyContainer_owner_id int8 not null,
        anyContainer_owner_oid varchar(36) not null,
        anyContainer_owner_type int4 not null,
        type_namespace varchar(255) not null,
        type_localPart varchar(100) not null,
        stringValue varchar(255) not null,
        dynamicDef boolean,
        valueType int4,
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, stringValue)
    );

    create table m_assignment (
        accountConstruction text,
        administrativeStatus int4,
        archiveTimestamp timestamp,
        disableReason varchar(255),
        disableTimestamp timestamp,
        effectiveStatus int4,
        enableTimestamp timestamp,
        validFrom timestamp,
        validTo timestamp,
        validityChangeTimestamp timestamp,
        validityStatus int4,
        assignmentOwner int4,
        construction text,
        description text,
        orderValue int4,
        owner_id int8 not null,
        owner_oid varchar(36) not null,
        targetRef_description text,
        targetRef_filter text,
        targetRef_relationLocalPart varchar(100),
        targetRef_relationNamespace varchar(255),
        targetRef_targetOid varchar(36),
        targetRef_type int4,
        tenantRef_description text,
        tenantRef_filter text,
        tenantRef_relationLocalPart varchar(100),
        tenantRef_relationNamespace varchar(255),
        tenantRef_targetOid varchar(36),
        tenantRef_type int4,
        id int8 not null,
        oid varchar(36) not null,
        extId int8,
        extOid varchar(36),
        extType int4,
        primary key (id, oid)
    );

    create table m_audit_delta (
        checksum varchar(32) not null,
        record_id int8 not null,
        context text,
        delta text,
        deltaOid varchar(36),
        deltaType int4,
        details text,
        localizedMessage text,
        message text,
        messageCode varchar(255),
        operation text,
        params text,
        partialResults text,
        returns text,
        status int4,
        token int8,
        primary key (checksum, record_id)
    );

    create table m_audit_event (
        id int8 not null,
        channel varchar(255),
        eventIdentifier varchar(255),
        eventStage int4,
        eventType int4,
        hostIdentifier varchar(255),
        initiatorName varchar(255),
        initiatorOid varchar(36),
        message varchar(1024),
        outcome int4,
        parameter varchar(255),
        result varchar(255),
        sessionIdentifier varchar(255),
        targetName varchar(255),
        targetOid varchar(36),
        targetOwnerName varchar(255),
        targetOwnerOid varchar(36),
        targetType int4,
        taskIdentifier varchar(255),
        taskOID varchar(255),
        timestampValue timestamp,
        primary key (id)
    );

    create table m_authorization (
        decision int4,
        description text,
        objectSpecification text,
        owner_id int8 not null,
        owner_oid varchar(36) not null,
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid)
    );

    create table m_authorization_action (
        role_id int8 not null,
        role_oid varchar(36) not null,
        action varchar(255)
    );

    create table m_connector (
        connectorBundle varchar(255),
        connectorHostRef_description text,
        connectorHostRef_filter text,
        c16_relationLocalPart varchar(100),
        c16_relationNamespace varchar(255),
        connectorHostRef_targetOid varchar(36),
        connectorHostRef_type int4,
        connectorType varchar(255),
        connectorVersion varchar(255),
        framework varchar(255),
        name_norm varchar(255),
        name_orig varchar(255),
        namespace varchar(255),
        xmlSchema text,
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid)
    );

    create table m_connector_host (
        hostname varchar(255),
        name_norm varchar(255),
        name_orig varchar(255),
        port varchar(255),
        protectConnection boolean,
        sharedSecret text,
        timeout int4,
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_connector_target_system (
        connector_id int8 not null,
        connector_oid varchar(36) not null,
        targetSystemType varchar(255)
    );

    create table m_container (
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid)
    );

    create table m_exclusion (
        description text,
        owner_id int8 not null,
        owner_oid varchar(36) not null,
        policy int4,
        targetRef_description text,
        targetRef_filter text,
        targetRef_relationLocalPart varchar(100),
        targetRef_relationNamespace varchar(255),
        targetRef_targetOid varchar(36),
        targetRef_type int4,
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid)
    );

    create table m_focus (
        administrativeStatus int4,
        archiveTimestamp timestamp,
        disableReason varchar(255),
        disableTimestamp timestamp,
        effectiveStatus int4,
        enableTimestamp timestamp,
        validFrom timestamp,
        validTo timestamp,
        validityChangeTimestamp timestamp,
        validityStatus int4,
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid)
    );

    create table m_generic_object (
        name_norm varchar(255),
        name_orig varchar(255),
        objectType varchar(255),
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_metadata (
        owner_id int8 not null,
        owner_oid varchar(36) not null,
        createChannel varchar(255),
        createTimestamp timestamp,
        creatorRef_description text,
        creatorRef_filter text,
        creatorRef_relationLocalPart varchar(100),
        creatorRef_relationNamespace varchar(255),
        creatorRef_targetOid varchar(36),
        creatorRef_type int4,
        modifierRef_description text,
        modifierRef_filter text,
        modifierRef_relationLocalPart varchar(100),
        modifierRef_relationNamespace varchar(255),
        modifierRef_targetOid varchar(36),
        modifierRef_type int4,
        modifyChannel varchar(255),
        modifyTimestamp timestamp,
        primary key (owner_id, owner_oid)
    );

    create table m_node (
        clusteredNode boolean,
        hostname varchar(255),
        internalNodeIdentifier varchar(255),
        jmxPort int4,
        lastCheckInTime timestamp,
        name_norm varchar(255),
        name_orig varchar(255),
        nodeIdentifier varchar(255),
        running boolean,
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_object (
        description text,
        name_norm varchar(255),
        name_orig varchar(255),
        tenantRef_description text,
        tenantRef_filter text,
        tenantRef_relationLocalPart varchar(100),
        tenantRef_relationNamespace varchar(255),
        tenantRef_targetOid varchar(36),
        tenantRef_type int4,
        version int8 not null,
        id int8 not null,
        oid varchar(36) not null,
        extId int8,
        extOid varchar(36),
        extType int4,
        primary key (id, oid)
    );

    create table m_object_template (
        accountConstruction text,
        mapping text,
        name_norm varchar(255),
        name_orig varchar(255),
        type int4,
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_operation_result (
        owner_oid varchar(36) not null,
        owner_id int8 not null,
        context text,
        details text,
        localizedMessage text,
        message text,
        messageCode varchar(255),
        operation text,
        params text,
        partialResults text,
        returns text,
        status int4,
        token int8,
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
        tenant boolean,
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_org_closure (
        id int8 not null,
        ancestor_id int8,
        ancestor_oid varchar(36),
        depthValue int4,
        descendant_id int8,
        descendant_oid varchar(36),
        primary key (id)
    );

    create table m_org_incorrect (
        descendant_oid varchar(36) not null,
        descendant_id int8 not null,
        ancestor_oid varchar(36) not null,
        primary key (descendant_oid, descendant_id, ancestor_oid)
    );

    create table m_org_org_type (
        org_id int8 not null,
        org_oid varchar(36) not null,
        orgType varchar(255)
    );

    create table m_reference (
        reference_type int4 not null,
        owner_id int8 not null,
        owner_oid varchar(36) not null,
        relLocalPart varchar(100) not null,
        relNamespace varchar(255) not null,
        targetOid varchar(36) not null,
        description text,
        filter text,
        containerType int4,
        primary key (owner_id, owner_oid, relLocalPart, relNamespace, targetOid)
    );

    create table m_report (
        configuration text,
        configurationSchema text,
        dataSource_providerClass varchar(255),
        dataSource_springBean boolean,
        export int4,
        field text,
        name_norm varchar(255),
        name_orig varchar(255),
        orientation int4,
        parent boolean,
        subreport text,
        template text,
        templateStyle text,
        useHibernateSession boolean,
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_report_output (
        name_norm varchar(255),
        name_orig varchar(255),
        reportFilePath varchar(255),
        reportRef_description text,
        reportRef_filter text,
        reportRef_relationLocalPart varchar(100),
        reportRef_relationNamespace varchar(255),
        reportRef_targetOid varchar(36),
        reportRef_type int4,
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_resource (
        administrativeState int4,
        capabilities_cachingMetadata text,
        capabilities_configured text,
        capabilities_native text,
        configuration text,
        connectorRef_description text,
        connectorRef_filter text,
        connectorRef_relationLocalPart varchar(100),
        connectorRef_relationNamespace varchar(255),
        connectorRef_targetOid varchar(36),
        connectorRef_type int4,
        consistency text,
        name_norm varchar(255),
        name_orig varchar(255),
        namespace varchar(255),
        o16_lastAvailabilityStatus int4,
        projection text,
        schemaHandling text,
        scripts text,
        synchronization text,
        xmlSchema text,
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_role (
        name_norm varchar(255),
        name_orig varchar(255),
        roleType varchar(255),
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_shadow (
        administrativeStatus int4,
        archiveTimestamp timestamp,
        disableReason varchar(255),
        disableTimestamp timestamp,
        effectiveStatus int4,
        enableTimestamp timestamp,
        validFrom timestamp,
        validTo timestamp,
        validityChangeTimestamp timestamp,
        validityStatus int4,
        assigned boolean,
        attemptNumber int4,
        dead boolean,
        exist boolean,
        failedOperationType int4,
        fullSynchronizationTimestamp timestamp,
        intent varchar(255),
        iteration int4,
        iterationToken varchar(255),
        kind int4,
        name_norm varchar(255),
        name_orig varchar(255),
        objectChange text,
        class_namespace varchar(255),
        class_localPart varchar(100),
        resourceRef_description text,
        resourceRef_filter text,
        resourceRef_relationLocalPart varchar(100),
        resourceRef_relationNamespace varchar(255),
        resourceRef_targetOid varchar(36),
        resourceRef_type int4,
        synchronizationSituation int4,
        synchronizationTimestamp timestamp,
        id int8 not null,
        oid varchar(36) not null,
        attrId int8,
        attrOid varchar(36),
        attrType int4,
        primary key (id, oid)
    );

    create table m_sync_situation_description (
        checksum varchar(32) not null,
        shadow_id int8 not null,
        shadow_oid varchar(36) not null,
        chanel varchar(255),
        fullFlag boolean,
        situation int4,
        timestampValue timestamp,
        primary key (checksum, shadow_id, shadow_oid)
    );

    create table m_system_configuration (
        cleanupPolicy text,
        connectorFramework text,
        d22_description text,
        defaultUserTemplateRef_filter text,
        d22_relationLocalPart varchar(100),
        d22_relationNamespace varchar(255),
        d22_targetOid varchar(36),
        defaultUserTemplateRef_type int4,
        g36 text,
        g23_description text,
        globalPasswordPolicyRef_filter text,
        g23_relationLocalPart varchar(100),
        g23_relationNamespace varchar(255),
        g23_targetOid varchar(36),
        globalPasswordPolicyRef_type int4,
        logging text,
        modelHooks text,
        name_norm varchar(255),
        name_orig varchar(255),
        notificationConfiguration text,
        objectTemplate text,
        profilingConfiguration text,
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_task (
        binding int4,
        canRunOnNode varchar(255),
        category varchar(255),
        completionTimestamp timestamp,
        executionStatus int4,
        expectedTotal int8,
        handlerUri varchar(255),
        lastRunFinishTimestamp timestamp,
        lastRunStartTimestamp timestamp,
        name_norm varchar(255),
        name_orig varchar(255),
        node varchar(255),
        objectRef_description text,
        objectRef_filter text,
        objectRef_relationLocalPart varchar(100),
        objectRef_relationNamespace varchar(255),
        objectRef_targetOid varchar(36),
        objectRef_type int4,
        otherHandlersUriStack text,
        ownerRef_description text,
        ownerRef_filter text,
        ownerRef_relationLocalPart varchar(100),
        ownerRef_relationNamespace varchar(255),
        ownerRef_targetOid varchar(36),
        ownerRef_type int4,
        parent varchar(255),
        progress int8,
        recurrence int4,
        resultStatus int4,
        schedule text,
        taskIdentifier varchar(255),
        threadStopAction int4,
        waitingReason int4,
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid)
    );

    create table m_task_dependent (
        task_id int8 not null,
        task_oid varchar(36) not null,
        dependent varchar(255)
    );

    create table m_trigger (
        handlerUri varchar(255),
        owner_id int8 not null,
        owner_oid varchar(36) not null,
        timestampValue timestamp,
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid)
    );

    create table m_user (
        additionalName_norm varchar(255),
        additionalName_orig varchar(255),
        costCenter varchar(255),
        allowedIdmAdminGuiAccess boolean,
        passwordXml text,
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
        jpegPhoto oid,
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
        id int8 not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    );

    create table m_user_employee_type (
        user_id int8 not null,
        user_oid varchar(36) not null,
        employeeType varchar(255)
    );

    create table m_user_organization (
        user_id int8 not null,
        user_oid varchar(36) not null,
        norm varchar(255),
        orig varchar(255)
    );

    create table m_user_organizational_unit (
        user_id int8 not null,
        user_oid varchar(36) not null,
        norm varchar(255),
        orig varchar(255)
    );

    create table m_value_policy (
        lifetime text,
        name_norm varchar(255),
        name_orig varchar(255),
        stringPolicy text,
        id int8 not null,
        oid varchar(36) not null,
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

    create index iObjectNameOrig on m_object (name_orig);

    create index iObjectNameNorm on m_object (name_norm);

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

    create sequence hibernate_sequence start 1 increment 1;
