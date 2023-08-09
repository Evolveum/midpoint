/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.audit.qmodel;

import static com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath.JSONB_TYPE;

import java.io.Serial;
import java.sql.Types;
import java.time.Instant;

import com.evolveum.midpoint.xml.ns._public.common.audit_3.EffectivePrivilegesModificationType;

import com.querydsl.core.types.dsl.*;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

/**
 * Querydsl query type for `MA_AUDIT_EVENT` table.
 */
@SuppressWarnings("unused")
public class QAuditEventRecord extends FlexibleRelationalPathBase<MAuditEventRecord> {

    @Serial private static final long serialVersionUID = 6404919477946782879L;

    public static final String TABLE_NAME = "ma_audit_event";

    public static final ColumnMetadata ID =
            ColumnMetadata.named("id").ofType(Types.BIGINT).notNull();
    public static final ColumnMetadata TIMESTAMP =
            ColumnMetadata.named("timestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE).notNull();
    public static final ColumnMetadata EVENT_IDENTIFIER =
            ColumnMetadata.named("eventIdentifier").ofType(Types.VARCHAR);
    public static final ColumnMetadata EVENT_TYPE =
            ColumnMetadata.named("eventType").ofType(Types.OTHER);
    public static final ColumnMetadata EVENT_STAGE =
            ColumnMetadata.named("eventStage").ofType(Types.OTHER);
    public static final ColumnMetadata SESSION_IDENTIFIER =
            ColumnMetadata.named("sessionIdentifier").ofType(Types.VARCHAR);
    public static final ColumnMetadata REQUEST_IDENTIFIER =
            ColumnMetadata.named("requestIdentifier").ofType(Types.VARCHAR);
    public static final ColumnMetadata TASK_IDENTIFIER =
            ColumnMetadata.named("taskIdentifier").ofType(Types.VARCHAR);
    public static final ColumnMetadata TASK_OID =
            ColumnMetadata.named("taskOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata HOST_IDENTIFIER =
            ColumnMetadata.named("hostIdentifier").ofType(Types.VARCHAR);
    public static final ColumnMetadata NODE_IDENTIFIER =
            ColumnMetadata.named("nodeIdentifier").ofType(Types.VARCHAR);
    public static final ColumnMetadata REMOTE_HOST_ADDRESS =
            ColumnMetadata.named("remoteHostAddress").ofType(Types.VARCHAR);
    public static final ColumnMetadata INITIATOR_OID =
            ColumnMetadata.named("initiatorOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata INITIATOR_TYPE =
            ColumnMetadata.named("initiatorType").ofType(Types.OTHER);
    public static final ColumnMetadata INITIATOR_NAME =
            ColumnMetadata.named("initiatorName").ofType(Types.VARCHAR);
    public static final ColumnMetadata ATTORNEY_OID =
            ColumnMetadata.named("attorneyOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata ATTORNEY_NAME =
            ColumnMetadata.named("attorneyName").ofType(Types.VARCHAR);
    public static final ColumnMetadata EFFECTIVE_PRINCIPAL_OID =
            ColumnMetadata.named("effectivePrincipalOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata EFFECTIVE_PRINCIPAL_TYPE =
            ColumnMetadata.named("effectivePrincipalType").ofType(Types.OTHER);
    public static final ColumnMetadata EFFECTIVE_PRINCIPAL_NAME =
            ColumnMetadata.named("effectivePrincipalName").ofType(Types.VARCHAR);
    public static final ColumnMetadata EFFECTIVE_PRIVILEGES_MODIFICATION =
            ColumnMetadata.named("effectivePrivilegesModification").ofType(Types.OTHER);
    public static final ColumnMetadata TARGET_OID =
            ColumnMetadata.named("targetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata TARGET_TYPE =
            ColumnMetadata.named("targetType").ofType(Types.OTHER);
    public static final ColumnMetadata TARGET_NAME =
            ColumnMetadata.named("targetName").ofType(Types.VARCHAR);
    public static final ColumnMetadata TARGET_OWNER_OID =
            ColumnMetadata.named("targetOwnerOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata TARGET_OWNER_TYPE =
            ColumnMetadata.named("targetOwnerType").ofType(Types.OTHER);
    public static final ColumnMetadata TARGET_OWNER_NAME =
            ColumnMetadata.named("targetOwnerName").ofType(Types.VARCHAR);
    public static final ColumnMetadata CHANNEL =
            ColumnMetadata.named("channel").ofType(Types.VARCHAR);
    public static final ColumnMetadata OUTCOME =
            ColumnMetadata.named("outcome").ofType(Types.OTHER);
    public static final ColumnMetadata PARAMETER =
            ColumnMetadata.named("parameter").ofType(Types.VARCHAR);
    public static final ColumnMetadata RESULT =
            ColumnMetadata.named("result").ofType(Types.VARCHAR);
    public static final ColumnMetadata MESSAGE =
            ColumnMetadata.named("message").ofType(Types.VARCHAR);
    public static final ColumnMetadata CHANGED_ITEM_PATHS =
            ColumnMetadata.named("changedItemPaths").ofType(Types.ARRAY);
    public static final ColumnMetadata RESOURCE_OIDS =
            ColumnMetadata.named("resourceOids").ofType(Types.ARRAY);
    public static final ColumnMetadata PROPERTIES =
            ColumnMetadata.named("properties").ofType(JSONB_TYPE);

    // columns and relations
    public final NumberPath<Long> id = createLong("id", ID);
    public final DateTimePath<Instant> timestamp = createInstant("timestamp", TIMESTAMP);
    public final StringPath eventIdentifier = createString("eventIdentifier", EVENT_IDENTIFIER);
    public final EnumPath<AuditEventTypeType> eventType =
            createEnum("eventType", AuditEventTypeType.class, EVENT_TYPE);
    public final EnumPath<AuditEventStageType> eventStage =
            createEnum("eventStage", AuditEventStageType.class, EVENT_STAGE);
    public final StringPath sessionIdentifier =
            createString("sessionIdentifier", SESSION_IDENTIFIER);
    public final StringPath requestIdentifier =
            createString("requestIdentifier", REQUEST_IDENTIFIER);
    public final StringPath taskIdentifier = createString("taskIdentifier", TASK_IDENTIFIER);
    public final UuidPath taskOid = createUuid("taskOid", TASK_OID);
    public final StringPath hostIdentifier = createString("hostIdentifier", HOST_IDENTIFIER);
    public final StringPath nodeIdentifier = createString("nodeIdentifier", NODE_IDENTIFIER);
    public final StringPath remoteHostAddress =
            createString("remoteHostAddress", REMOTE_HOST_ADDRESS);
    public final UuidPath initiatorOid = createUuid("initiatorOid", INITIATOR_OID);
    public final EnumPath<MObjectType> initiatorType =
            createEnum("initiatorType", MObjectType.class, INITIATOR_TYPE);
    public final StringPath initiatorName = createString("initiatorName", INITIATOR_NAME);
    public final UuidPath attorneyOid = createUuid("attorneyOid", ATTORNEY_OID);
    public final StringPath attorneyName = createString("attorneyName", ATTORNEY_NAME);
    public final UuidPath effectivePrincipalOid = createUuid("effectivePrincipalOid", EFFECTIVE_PRINCIPAL_OID);
    public final EnumPath<MObjectType> effectivePrincipalType =
            createEnum("effectivePrincipalType", MObjectType.class, EFFECTIVE_PRINCIPAL_TYPE);
    public final StringPath effectivePrincipalName = createString("effectivePrincipalName", EFFECTIVE_PRINCIPAL_NAME);
    public final EnumPath<EffectivePrivilegesModificationType> effectivePrivilegesModification =
            createEnum("effectivePrivilegesModification", EffectivePrivilegesModificationType.class, EFFECTIVE_PRIVILEGES_MODIFICATION);
    public final UuidPath targetOid = createUuid("targetOid", TARGET_OID);
    public final EnumPath<MObjectType> targetType =
            createEnum("targetType", MObjectType.class, TARGET_TYPE);
    public final StringPath targetName = createString("targetName", TARGET_NAME);
    public final UuidPath targetOwnerOid = createUuid("targetOwnerOid", TARGET_OWNER_OID);
    public final EnumPath<MObjectType> targetOwnerType =
            createEnum("targetOwnerType", MObjectType.class, TARGET_OWNER_TYPE);
    public final StringPath targetOwnerName = createString("targetOwnerName", TARGET_OWNER_NAME);
    public final StringPath channel = createString("channel", CHANNEL);
    public final EnumPath<OperationResultStatusType> outcome =
            createEnum("outcome", OperationResultStatusType.class, OUTCOME);
    public final StringPath parameter = createString("parameter", PARAMETER);
    public final StringPath result = createString("result", RESULT);
    public final StringPath message = createString("message", MESSAGE);
    public final ArrayPath<String[], String> changedItemPaths =
            createArray("changedItemPaths", String[].class, CHANGED_ITEM_PATHS);
    public final ArrayPath<String[], String> resourceOids =
            createArray("resourceOids", String[].class, RESOURCE_OIDS);
    public final JsonbPath properties =
            addMetadata(add(new JsonbPath(forProperty("properties"))), PROPERTIES);

    public QAuditEventRecord(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QAuditEventRecord(String variable, String schema, String table) {
        super(MAuditEventRecord.class, variable, schema, table);
    }
}
