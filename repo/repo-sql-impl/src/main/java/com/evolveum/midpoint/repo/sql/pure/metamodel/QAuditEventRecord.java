/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.pure.metamodel;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

import java.sql.Types;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sql.pure.*;

/**
 * Querydsl query type for M_AUDIT_EVENT table.
 */
public class QAuditEventRecord extends FlexibleRelationalPathBase<MAuditEventRecord> {

    private static final long serialVersionUID = -229589301;

    // column metadata constants, we don't care about the indexes, better to remove them
    public static final ColumnMetadata ID =
            ColumnMetadata.named("ID").ofType(Types.BIGINT).withSize(19).notNull();
    public static final ColumnMetadata CHANNEL =
            ColumnMetadata.named("CHANNEL").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata ATTORNEY_NAME =
            ColumnMetadata.named("ATTORNEYNAME").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata ATTORNEY_OID =
            ColumnMetadata.named("ATTORNEYOID").ofType(Types.VARCHAR).withSize(36);
    public static final ColumnMetadata EVENT_IDENTIFIER =
            ColumnMetadata.named("EVENTIDENTIFIER").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata EVENT_STAGE =
            ColumnMetadata.named("EVENTSTAGE").ofType(Types.INTEGER).withSize(10);
    public static final ColumnMetadata EVENT_TYPE =
            ColumnMetadata.named("EVENTTYPE").ofType(Types.INTEGER).withSize(10);
    public static final ColumnMetadata HOST_IDENTIFIER =
            ColumnMetadata.named("HOSTIDENTIFIER").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata INITIATOR_NAME =
            ColumnMetadata.named("INITIATORNAME").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata INITIATOR_OID =
            ColumnMetadata.named("INITIATOROID").ofType(Types.VARCHAR).withSize(36);
    public static final ColumnMetadata INITIATOR_TYPE =
            ColumnMetadata.named("INITIATORTYPE").ofType(Types.INTEGER).withSize(10);
    public static final ColumnMetadata MESSAGE =
            ColumnMetadata.named("MESSAGE").ofType(Types.VARCHAR).withSize(1024);
    public static final ColumnMetadata NODE_IDENTIFIER =
            ColumnMetadata.named("NODEIDENTIFIER").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata OUTCOME =
            ColumnMetadata.named("OUTCOME").ofType(Types.INTEGER).withSize(10);
    public static final ColumnMetadata PARAMETER =
            ColumnMetadata.named("PARAMETER").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata REMOTE_HOST_ADDRESS =
            ColumnMetadata.named("REMOTEHOSTADDRESS").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata REQUEST_IDENTIFIER =
            ColumnMetadata.named("REQUESTIDENTIFIER").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata RESULT =
            ColumnMetadata.named("RESULT").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata SESSION_IDENTIFIER =
            ColumnMetadata.named("SESSIONIDENTIFIER").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata TARGET_NAME =
            ColumnMetadata.named("TARGETNAME").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata TARGET_OID =
            ColumnMetadata.named("TARGETOID").ofType(Types.VARCHAR).withSize(36);
    public static final ColumnMetadata TARGET_OWNER_NAME =
            ColumnMetadata.named("TARGETOWNERNAME").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata TARGET_OWNER_OID =
            ColumnMetadata.named("TARGETOWNEROID").ofType(Types.VARCHAR).withSize(36);
    public static final ColumnMetadata TARGET_OWNER_TYPE =
            ColumnMetadata.named("TARGETOWNERTYPE").ofType(Types.INTEGER).withSize(10);
    public static final ColumnMetadata TARGET_TYPE =
            ColumnMetadata.named("TARGETTYPE").ofType(Types.INTEGER).withSize(10);
    public static final ColumnMetadata TASK_IDENTIFIER =
            ColumnMetadata.named("TASKIDENTIFIER").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata TASK_OID =
            ColumnMetadata.named("TASKOID").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata TIMESTAMP_VALUE =
            ColumnMetadata.named("TIMESTAMPVALUE").ofType(Types.TIMESTAMP).withSize(23).withDigits(10);

    public static final SqlTableMetamodel<QAuditEventRecord> METAMODEL =
            new SqlTableMetamodel<>("M_AUDIT_EVENT", QAuditEventRecord.class,
                    ID, CHANNEL, ATTORNEY_NAME, ATTORNEY_OID,
                    EVENT_IDENTIFIER, EVENT_STAGE, EVENT_TYPE,
                    HOST_IDENTIFIER, INITIATOR_NAME, INITIATOR_OID, INITIATOR_TYPE,
                    MESSAGE, NODE_IDENTIFIER, OUTCOME, PARAMETER,
                    REMOTE_HOST_ADDRESS, REQUEST_IDENTIFIER, RESULT, SESSION_IDENTIFIER,
                    TARGET_NAME, TARGET_OID, TARGET_TYPE,
                    TARGET_OWNER_NAME, TARGET_OWNER_OID, TARGET_OWNER_TYPE,
                    TASK_IDENTIFIER, TASK_OID, TIMESTAMP_VALUE);

    public static final ExtensionColumns EXTENSION_COLUMNS = new ExtensionColumns();

    /**
     * Default base-path variable for this table.
     */
    public static final QAuditEventRecord M_AUDIT_EVENT = new QAuditEventRecord("M_AUDIT_EVENT");

    /**
     * Alias for {@link #M_AUDIT_EVENT} which may be handy to use without static imports.
     */
    public static final QAuditEventRecord $ = M_AUDIT_EVENT;

    // columns and relations
    public final NumberPath<Long> id = addMetadata(createNumber("id", Long.class), ID);
    public final StringPath attorneyName = createString("attorneyname");
    public final StringPath attorneyOid = createString("attorneyoid");
    public final StringPath channel = addMetadata(createString("channel"), CHANNEL);
    public final StringPath eventidentifier = createString("eventidentifier");
    public final NumberPath<Integer> eventstage = createNumber("eventstage", Integer.class);
    public final NumberPath<Integer> eventtype = createNumber("eventtype", Integer.class);
    public final StringPath hostidentifier = createString("hostidentifier");
    public final StringPath initiatorname = createString("initiatorname");
    public final StringPath initiatoroid = createString("initiatoroid");
    public final NumberPath<Integer> initiatortype = createNumber("initiatortype", Integer.class);
    public final StringPath message = createString("message");
    public final StringPath nodeidentifier = createString("nodeidentifier");
    public final NumberPath<Integer> outcome = createNumber("outcome", Integer.class);
    public final StringPath parameter = createString("parameter");
    public final StringPath remotehostaddress = createString("remotehostaddress");
    public final StringPath requestidentifier = createString("requestidentifier");
    public final StringPath result = createString("result");
    public final StringPath sessionidentifier = createString("sessionidentifier");
    public final StringPath targetname = createString("targetname");
    public final StringPath targetoid = createString("targetoid");
    public final StringPath targetownername = createString("targetownername");
    public final StringPath targetowneroid = createString("targetowneroid");
    public final NumberPath<Integer> targetownertype = createNumber("targetownertype", Integer.class);
    public final NumberPath<Integer> targettype = createNumber("targettype", Integer.class);
    public final StringPath taskidentifier = createString("taskidentifier");
    public final StringPath taskoid = createString("taskoid");
    public final DateTimePath<java.sql.Timestamp> timestampvalue = createDateTime("timestampvalue", java.sql.Timestamp.class);

    public final com.querydsl.sql.PrimaryKey<MAuditEventRecord> constraint85c = createPrimaryKey(id);
    public final com.querydsl.sql.ForeignKey<QMAuditItem> _auditItemFk = createInvForeignKey(id, "RECORD_ID");
    public final com.querydsl.sql.ForeignKey<QMAuditPropValue> _auditPropValueFk = createInvForeignKey(id, "RECORD_ID");
    public final com.querydsl.sql.ForeignKey<MAuditDelta> _auditDeltaFk = createInvForeignKey(id, "RECORD_ID");
    public final com.querydsl.sql.ForeignKey<QMAuditRefValue> _auditRefValueFk = createInvForeignKey(id, "RECORD_ID");
    public final com.querydsl.sql.ForeignKey<QMAuditResource> _auditResourceFk = createInvForeignKey(id, "RECORD_ID");

    public QAuditEventRecord(String variable) {
        super(MAuditEventRecord.class, forVariable(variable), "PUBLIC", "M_AUDIT_EVENT");
        applyExtensions(EXTENSION_COLUMNS);
    }

    public QAuditEventRecord(String variable, String schema, String table) {
        super(MAuditEventRecord.class, forVariable(variable), schema, table);
        applyExtensions(EXTENSION_COLUMNS);
    }

    public QAuditEventRecord(String variable, String schema) {
        super(MAuditEventRecord.class, forVariable(variable), schema, "M_AUDIT_EVENT");
        applyExtensions(EXTENSION_COLUMNS);
    }

    public QAuditEventRecord(Path<? extends MAuditEventRecord> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_AUDIT_EVENT");
        applyExtensions(EXTENSION_COLUMNS);
    }

    public QAuditEventRecord(PathMetadata metadata) {
        super(MAuditEventRecord.class, metadata, "PUBLIC", "M_AUDIT_EVENT");
        applyExtensions(EXTENSION_COLUMNS);
    }

    @Override
    protected SqlTableMetamodel<?> getTableMetamodel() {
        return METAMODEL;
    }
}
