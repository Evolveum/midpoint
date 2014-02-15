# use for db create
# CREATE DATABASE <database name>
#   CHARACTER SET utf8
#   DEFAULT CHARACTER SET utf8
#   COLLATE utf8_bin
#   DEFAULT COLLATE utf8_bin
# ;

# replace "ENGINE=InnoDB" with "DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB"
# replace "DATETIME" with "DATETIME(6)"

    create table m_abstract_role (
        approvalExpression longtext,
        approvalProcess varchar(255),
        approvalSchema longtext,
        automaticallyApproved longtext,
        requestable bit,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_any (
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        owner_type integer not null,
        primary key (owner_id, owner_oid, owner_type)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_any_clob (
        checksum varchar(32) not null,
        name_namespace varchar(255) not null,
        name_localPart varchar(100) not null,
        anyContainer_owner_id bigint not null,
        anyContainer_owner_oid varchar(36) not null,
        anyContainer_owner_type integer not null,
        type_namespace varchar(255) not null,
        type_localPart varchar(100) not null,
        dynamicDef bit,
        clobValue longtext,
        valueType integer,
        primary key (checksum, name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_any_date (
        name_namespace varchar(255) not null,
        name_localPart varchar(100) not null,
        anyContainer_owner_id bigint not null,
        anyContainer_owner_oid varchar(36) not null,
        anyContainer_owner_type integer not null,
        type_namespace varchar(255) not null,
        type_localPart varchar(100) not null,
        dateValue DATETIME(6) not null,
        dynamicDef bit,
        valueType integer,
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, dateValue)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_any_long (
        name_namespace varchar(255) not null,
        name_localPart varchar(100) not null,
        anyContainer_owner_id bigint not null,
        anyContainer_owner_oid varchar(36) not null,
        anyContainer_owner_type integer not null,
        type_namespace varchar(255) not null,
        type_localPart varchar(100) not null,
        longValue bigint not null,
        dynamicDef bit,
        valueType integer,
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, longValue)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_any_poly_string (
        name_namespace varchar(255) not null,
        name_localPart varchar(100) not null,
        anyContainer_owner_id bigint not null,
        anyContainer_owner_oid varchar(36) not null,
        anyContainer_owner_type integer not null,
        type_namespace varchar(255) not null,
        type_localPart varchar(100) not null,
        orig varchar(255) not null,
        dynamicDef bit,
        norm varchar(255),
        valueType integer,
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, orig)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_any_reference (
        name_namespace varchar(255) not null,
        name_localPart varchar(100) not null,
        anyContainer_owner_id bigint not null,
        anyContainer_owner_oid varchar(36) not null,
        anyContainer_owner_type integer not null,
        type_namespace varchar(255) not null,
        type_localPart varchar(100) not null,
        targetoid varchar(36) not null,
        description longtext,
        dynamicDef bit,
        filter longtext,
        relation_namespace varchar(255),
        relation_localPart varchar(100),
        targetType integer,
        valueType integer,
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, targetoid)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_any_string (
        name_namespace varchar(255) not null,
        name_localPart varchar(100) not null,
        anyContainer_owner_id bigint not null,
        anyContainer_owner_oid varchar(36) not null,
        anyContainer_owner_type integer not null,
        type_namespace varchar(255) not null,
        type_localPart varchar(100) not null,
        stringValue varchar(255) not null,
        dynamicDef bit,
        valueType integer,
        primary key (name_namespace, name_localPart, anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type, type_namespace, type_localPart, stringValue)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_assignment (
        accountConstruction longtext,
        administrativeStatus integer,
        archiveTimestamp DATETIME(6),
        disableReason varchar(255),
        disableTimestamp DATETIME(6),
        effectiveStatus integer,
        enableTimestamp DATETIME(6),
        validFrom DATETIME(6),
        validTo DATETIME(6),
        validityChangeTimestamp DATETIME(6),
        validityStatus integer,
        assignmentOwner integer,
        construction longtext,
        description longtext,
        orderValue integer,
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        targetRef_description longtext,
        targetRef_filter longtext,
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
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_audit_delta (
        checksum varchar(32) not null,
        record_id bigint not null,
        context longtext,
        delta longtext,
        deltaOid varchar(36),
        deltaType integer,
        details longtext,
        localizedMessage longtext,
        message longtext,
        messageCode varchar(255),
        operation longtext,
        params longtext,
        partialResults longtext,
        returns longtext,
        status integer,
        token bigint,
        primary key (checksum, record_id)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

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
        timestampValue DATETIME(6),
        primary key (id)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_authorization (
        decision integer,
        description longtext,
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_authorization_action (
        role_id bigint not null,
        role_oid varchar(36) not null,
        action varchar(255)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_connector (
        connectorBundle varchar(255),
        connectorHostRef_description longtext,
        connectorHostRef_filter longtext,
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
        xmlSchema longtext,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_connector_host (
        hostname varchar(255),
        name_norm varchar(255),
        name_orig varchar(255),
        port varchar(255),
        protectConnection bit,
        sharedSecret longtext,
        timeout integer,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_connector_target_system (
        connector_id bigint not null,
        connector_oid varchar(36) not null,
        targetSystemType varchar(255)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_container (
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_exclusion (
        description longtext,
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        policy integer,
        targetRef_description longtext,
        targetRef_filter longtext,
        targetRef_relationLocalPart varchar(100),
        targetRef_relationNamespace varchar(255),
        targetRef_targetOid varchar(36),
        targetRef_type integer,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_focus (
        administrativeStatus integer,
        archiveTimestamp DATETIME(6),
        disableReason varchar(255),
        disableTimestamp DATETIME(6),
        effectiveStatus integer,
        enableTimestamp DATETIME(6),
        validFrom DATETIME(6),
        validTo DATETIME(6),
        validityChangeTimestamp DATETIME(6),
        validityStatus integer,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_generic_object (
        name_norm varchar(255),
        name_orig varchar(255),
        objectType varchar(255),
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_metadata (
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        createChannel varchar(255),
        createTimestamp DATETIME(6),
        creatorRef_description longtext,
        creatorRef_filter longtext,
        creatorRef_relationLocalPart varchar(100),
        creatorRef_relationNamespace varchar(255),
        creatorRef_targetOid varchar(36),
        creatorRef_type integer,
        modifierRef_description longtext,
        modifierRef_filter longtext,
        modifierRef_relationLocalPart varchar(100),
        modifierRef_relationNamespace varchar(255),
        modifierRef_targetOid varchar(36),
        modifierRef_type integer,
        modifyChannel varchar(255),
        modifyTimestamp DATETIME(6),
        primary key (owner_id, owner_oid)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_node (
        clusteredNode bit,
        hostname varchar(255),
        internalNodeIdentifier varchar(255),
        jmxPort integer,
        lastCheckInTime DATETIME(6),
        name_norm varchar(255),
        name_orig varchar(255),
        nodeIdentifier varchar(255),
        running bit,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_object (
        description longtext,
        version bigint not null,
        id bigint not null,
        oid varchar(36) not null,
        extId bigint,
        extOid varchar(36),
        extType integer,
        primary key (id, oid)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_object_template (
        accountConstruction longtext,
        mapping longtext,
        name_norm varchar(255),
        name_orig varchar(255),
        type integer,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_operation_result (
        owner_oid varchar(36) not null,
        owner_id bigint not null,
        context longtext,
        details longtext,
        localizedMessage longtext,
        message longtext,
        messageCode varchar(255),
        operation longtext,
        params longtext,
        partialResults longtext,
        returns longtext,
        status integer,
        token bigint,
        primary key (owner_oid, owner_id)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

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
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_org_closure (
        id bigint not null,
        ancestor_id bigint,
        ancestor_oid varchar(36),
        depthValue integer,
        descendant_id bigint,
        descendant_oid varchar(36),
        primary key (id)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_org_incorrect (
        descendant_oid varchar(36) not null,
        descendant_id bigint not null,
        ancestor_oid varchar(36) not null,
        primary key (descendant_oid, descendant_id, ancestor_oid)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_org_org_type (
        org_id bigint not null,
        org_oid varchar(36) not null,
        orgType varchar(255)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_reference (
        reference_type integer not null,
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        relLocalPart varchar(100) not null,
        relNamespace varchar(255) not null,
        targetOid varchar(36) not null,
        description longtext,
        filter longtext,
        containerType integer,
        primary key (owner_id, owner_oid, relLocalPart, relNamespace, targetOid)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_report (
		configuration longtext,
        configurationSchema longtext,
        dataSource_providerClass varchar(255),
        dataSource_springBean bit,
        export integer,
        field longtext,
        name_norm varchar(255),
        name_orig varchar(255),
        orientation integer,
        parent bit,
        subreport longtext,
        template longtext,
        templateStyle longtext,
        useHibernateSession bit,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_report_output (
        name_norm varchar(255),
        name_orig varchar(255),
        reportFilePath varchar(255),
        reportRef_description longtext,
        reportRef_filter longtext,
        reportRef_relationLocalPart varchar(100),
        reportRef_relationNamespace varchar(255),
        reportRef_targetOid varchar(36),
        reportRef_type integer,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_resource (
        administrativeState integer,
        capabilities_cachingMetadata longtext,
        capabilities_configured longtext,
        capabilities_native longtext,
        configuration longtext,
        connectorRef_description longtext,
        connectorRef_filter longtext,
        connectorRef_relationLocalPart varchar(100),
        connectorRef_relationNamespace varchar(255),
        connectorRef_targetOid varchar(36),
        connectorRef_type integer,
        consistency longtext,
        name_norm varchar(255),
        name_orig varchar(255),
        namespace varchar(255),
        o16_lastAvailabilityStatus integer,
        projection longtext,
        schemaHandling longtext,
        scripts longtext,
        synchronization longtext,
        xmlSchema longtext,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_role (
        name_norm varchar(255),
        name_orig varchar(255),
        roleType varchar(255),
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_shadow (
        administrativeStatus integer,
        archiveTimestamp DATETIME(6),
        disableReason varchar(255),
        disableTimestamp DATETIME(6),
        effectiveStatus integer,
        enableTimestamp DATETIME(6),
        validFrom DATETIME(6),
        validTo DATETIME(6),
        validityChangeTimestamp DATETIME(6),
        validityStatus integer,
        assigned bit,
        attemptNumber integer,
        dead bit,
        exist bit,
        failedOperationType integer,
        fullSynchronizationTimestamp DATETIME(6),
        intent varchar(255),
        iteration integer,
        iterationToken varchar(255),
        kind integer,
        name_norm varchar(255),
        name_orig varchar(255),
        objectChange longtext,
        class_namespace varchar(255),
        class_localPart varchar(100),
        resourceRef_description longtext,
        resourceRef_filter longtext,
        resourceRef_relationLocalPart varchar(100),
        resourceRef_relationNamespace varchar(255),
        resourceRef_targetOid varchar(36),
        resourceRef_type integer,
        synchronizationSituation integer,
        synchronizationTimestamp DATETIME(6),
        id bigint not null,
        oid varchar(36) not null,
        attrId bigint,
        attrOid varchar(36),
        attrType integer,
        primary key (id, oid)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_sync_situation_description (
        checksum varchar(32) not null,
        shadow_id bigint not null,
        shadow_oid varchar(36) not null,
        chanel varchar(255),
        fullFlag bit,
        situation integer,
        timestampValue DATETIME(6),
        primary key (checksum, shadow_id, shadow_oid)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_system_configuration (
        cleanupPolicy longtext,
        connectorFramework longtext,
        d22_description longtext,
        defaultUserTemplateRef_filter longtext,
        d22_relationLocalPart varchar(100),
        d22_relationNamespace varchar(255),
        d22_targetOid varchar(36),
        defaultUserTemplateRef_type integer,
        g36 longtext,
        g23_description longtext,
        globalPasswordPolicyRef_filter longtext,
        g23_relationLocalPart varchar(100),
        g23_relationNamespace varchar(255),
        g23_targetOid varchar(36),
        globalPasswordPolicyRef_type integer,
        logging longtext,
        modelHooks longtext,
        name_norm varchar(255),
        name_orig varchar(255),
        notificationConfiguration longtext,
        objectTemplate longtext,
        profilingConfiguration longtext,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_task (
        binding integer,
        canRunOnNode varchar(255),
        category varchar(255),
        completionTimestamp DATETIME(6),
        executionStatus integer,
        expectedTotal bigint,
        handlerUri varchar(255),
        lastRunFinishTimestamp DATETIME(6),
        lastRunStartTimestamp DATETIME(6),
        name_norm varchar(255),
        name_orig varchar(255),
        node varchar(255),
        objectRef_description longtext,
        objectRef_filter longtext,
        objectRef_relationLocalPart varchar(100),
        objectRef_relationNamespace varchar(255),
        objectRef_targetOid varchar(36),
        objectRef_type integer,
        otherHandlersUriStack longtext,
        ownerRef_description longtext,
        ownerRef_filter longtext,
        ownerRef_relationLocalPart varchar(100),
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
        waitingReason integer,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_task_dependent (
        task_id bigint not null,
        task_oid varchar(36) not null,
        dependent varchar(255)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_trigger (
        handlerUri varchar(255),
        owner_id bigint not null,
        owner_oid varchar(36) not null,
        timestampValue DATETIME(6),
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_user (
        additionalName_norm varchar(255),
        additionalName_orig varchar(255),
        costCenter varchar(255),
        allowedIdmAdminGuiAccess bit,
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
        jpegPhoto longblob,
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
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_user_employee_type (
        user_id bigint not null,
        user_oid varchar(36) not null,
        employeeType varchar(255)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_user_organization (
        user_id bigint not null,
        user_oid varchar(36) not null,
        norm varchar(255),
        orig varchar(255)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_user_organizational_unit (
        user_id bigint not null,
        user_oid varchar(36) not null,
        norm varchar(255),
        orig varchar(255)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create table m_value_policy (
        lifetime longtext,
        name_norm varchar(255),
        name_orig varchar(255),
        stringPolicy longtext,
        id bigint not null,
        oid varchar(36) not null,
        primary key (id, oid),
        unique (name_norm)
    ) DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB;

    create index iRequestable on m_abstract_role (requestable);

    alter table m_abstract_role
        add index fk_abstract_role (id, oid),
        add constraint fk_abstract_role
        foreign key (id, oid)
        references m_focus (id, oid);

    alter table m_any_clob
        add index fk_any_clob (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type),
        add constraint fk_any_clob
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type)
        references m_any (owner_id, owner_oid, owner_type);

    create index iDate on m_any_date (dateValue);

    alter table m_any_date
        add index fk_any_date (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type),
        add constraint fk_any_date
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type)
        references m_any (owner_id, owner_oid, owner_type);

    create index iLong on m_any_long (longValue);

    alter table m_any_long
        add index fk_any_long (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type),
        add constraint fk_any_long
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type)
        references m_any (owner_id, owner_oid, owner_type);

    create index iPolyString on m_any_poly_string (orig);

    alter table m_any_poly_string
        add index fk_any_poly_string (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type),
        add constraint fk_any_poly_string
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type)
        references m_any (owner_id, owner_oid, owner_type);

    create index iTargetOid on m_any_reference (targetoid);

    alter table m_any_reference
        add index fk_any_reference (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type),
        add constraint fk_any_reference
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type)
        references m_any (owner_id, owner_oid, owner_type);

    create index iString on m_any_string (stringValue);

    alter table m_any_string
        add index fk_any_string (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type),
        add constraint fk_any_string
        foreign key (anyContainer_owner_id, anyContainer_owner_oid, anyContainer_owner_type)
        references m_any (owner_id, owner_oid, owner_type);

    create index iAssignmentAdministrative on m_assignment (administrativeStatus);

    create index iAssignmentEffective on m_assignment (effectiveStatus);

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
        add index fk_audit_delta (record_id),
        add constraint fk_audit_delta
        foreign key (record_id)
        references m_audit_event (id);

    alter table m_authorization
        add index fk_authorization (id, oid),
        add constraint fk_authorization
        foreign key (id, oid)
        references m_container (id, oid);

    alter table m_authorization
        add index fk_authorization_owner (owner_id, owner_oid),
        add constraint fk_authorization_owner
        foreign key (owner_id, owner_oid)
        references m_object (id, oid);

    alter table m_authorization_action
        add index fk_authorization_action (role_id, role_oid),
        add constraint fk_authorization_action
        foreign key (role_id, role_oid)
        references m_authorization (id, oid);

    create index iConnectorNameNorm on m_connector (name_norm);

    create index iConnectorNameOrig on m_connector (name_orig);

    alter table m_connector
        add index fk_connector (id, oid),
        add constraint fk_connector
        foreign key (id, oid)
        references m_object (id, oid);

    create index iConnectorHostName on m_connector_host (name_orig);

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

    create index iFocusAdministrative on m_focus (administrativeStatus);

    create index iFocusEffective on m_focus (effectiveStatus);

    alter table m_focus
        add index fk_focus (id, oid),
        add constraint fk_focus
        foreign key (id, oid)
        references m_object (id, oid);

    create index iGenericObjectName on m_generic_object (name_orig);

    alter table m_generic_object
        add index fk_generic_object (id, oid),
        add constraint fk_generic_object
        foreign key (id, oid)
        references m_object (id, oid);

    alter table m_metadata
        add index fk_metadata_owner (owner_id, owner_oid),
        add constraint fk_metadata_owner
        foreign key (owner_id, owner_oid)
        references m_container (id, oid);

    create index iNodeName on m_node (name_orig);

    alter table m_node
        add index fk_node (id, oid),
        add constraint fk_node
        foreign key (id, oid)
        references m_object (id, oid);

    alter table m_object
        add index fk_object (id, oid),
        add constraint fk_object
        foreign key (id, oid)
        references m_container (id, oid);

    create index iObjectTemplate on m_object_template (name_orig);

    alter table m_object_template
        add index fk_object_template (id, oid),
        add constraint fk_object_template
        foreign key (id, oid)
        references m_object (id, oid);

    alter table m_operation_result
        add index fk_result_owner (owner_id, owner_oid),
        add constraint fk_result_owner
        foreign key (owner_id, owner_oid)
        references m_object (id, oid);

    create index iOrgName on m_org (name_orig);

    alter table m_org
        add index fk_org (id, oid),
        add constraint fk_org
        foreign key (id, oid)
        references m_abstract_role (id, oid);

    create index iAncestorDepth on m_org_closure (ancestor_id, ancestor_oid, depthValue);

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

    create index iReferenceTargetOid on m_reference (targetOid);

    alter table m_reference
        add index fk_reference_owner (owner_id, owner_oid),
        add constraint fk_reference_owner
        foreign key (owner_id, owner_oid)
        references m_container (id, oid);

    create index iReportParent on m_report (parent);

    create index iReportName on m_report (name_orig);

    alter table m_report 
        add index fk_report (id, oid), 
        add constraint fk_report 
        foreign key (id, oid) 
        references m_object (id, oid);

    create index iReportOutputName on m_report_output (name_orig);

    alter table m_report_output 
        add index fk_reportoutput (id, oid), 
        add constraint fk_reportoutput 
        foreign key (id, oid) 
        references m_object (id, oid);

    create index iResourceName on m_resource (name_orig);

    alter table m_resource 
        add index fk_resource (id, oid), 
        add constraint fk_resource 
        foreign key (id, oid) 
        references m_object (id, oid);

    create index iRoleName on m_role (name_orig);

    alter table m_role 
        add index fk_role (id, oid), 
        add constraint fk_role 
        foreign key (id, oid) 
        references m_abstract_role (id, oid);

    create index iShadowNameOrig on m_shadow (name_orig);

    create index iShadowDead on m_shadow (dead);

    create index iShadowNameNorm on m_shadow (name_norm);

    create index iShadowResourceRef on m_shadow (resourceRef_targetOid);

    create index iShadowAdministrative on m_shadow (administrativeStatus);

    create index iShadowEffective on m_shadow (effectiveStatus);

    alter table m_shadow 
        add index fk_shadow (id, oid), 
        add constraint fk_shadow 
        foreign key (id, oid) 
        references m_object (id, oid);

    alter table m_sync_situation_description 
        add index fk_shadow_sync_situation (shadow_id, shadow_oid), 
        add constraint fk_shadow_sync_situation 
        foreign key (shadow_id, shadow_oid) 
        references m_shadow (id, oid);

    create index iSystemConfigurationName on m_system_configuration (name_orig);

    alter table m_system_configuration 
        add index fk_system_configuration (id, oid), 
        add constraint fk_system_configuration 
        foreign key (id, oid) 
        references m_object (id, oid);

    create index iTaskNameNameNorm on m_task (name_norm);

    create index iParent on m_task (parent);

    create index iTaskNameOrig on m_task (name_orig);

    alter table m_task 
        add index fk_task (id, oid), 
        add constraint fk_task 
        foreign key (id, oid) 
        references m_object (id, oid);

    alter table m_task_dependent 
        add index fk_task_dependent (task_id, task_oid), 
        add constraint fk_task_dependent 
        foreign key (task_id, task_oid) 
        references m_task (id, oid);

    create index iTriggerTimestamp on m_trigger (timestampValue);

    alter table m_trigger 
        add index fk_trigger (id, oid), 
        add constraint fk_trigger 
        foreign key (id, oid) 
        references m_container (id, oid);

    alter table m_trigger 
        add index fk_trigger_owner (owner_id, owner_oid), 
        add constraint fk_trigger_owner 
        foreign key (owner_id, owner_oid) 
        references m_object (id, oid);

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
        add index fk_user (id, oid), 
        add constraint fk_user 
        foreign key (id, oid) 
        references m_focus (id, oid);

    alter table m_user_employee_type 
        add index fk_user_employee_type (user_id, user_oid), 
        add constraint fk_user_employee_type 
        foreign key (user_id, user_oid) 
        references m_user (id, oid);

    alter table m_user_organization 
        add index fk_user_organization (user_id, user_oid), 
        add constraint fk_user_organization 
        foreign key (user_id, user_oid) 
        references m_user (id, oid);

    alter table m_user_organizational_unit 
        add index fk_user_org_unit (user_id, user_oid), 
        add constraint fk_user_org_unit 
        foreign key (user_id, user_oid) 
        references m_user (id, oid);

    create index iValuePolicy on m_value_policy (name_orig);

    alter table m_value_policy 
        add index fk_value_policy (id, oid), 
        add constraint fk_value_policy 
        foreign key (id, oid) 
        references m_object (id, oid);

    create table hibernate_sequence (
         next_val bigint 
    );

    insert into hibernate_sequence values ( 1 );
