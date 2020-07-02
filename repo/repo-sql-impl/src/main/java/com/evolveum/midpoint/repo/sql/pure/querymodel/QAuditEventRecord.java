/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.pure.querymodel;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

import java.sql.Types;

import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sql.pure.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sql.pure.MAuditDelta;
import com.evolveum.midpoint.repo.sql.pure.MAuditEventRecord;

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

    // columns and relations
    public final NumberPath<Long> id = addMetadata(createNumber("id", Long.class), ID);
    public final StringPath attorneyName = addMetadata(createString("attorneyname"), ATTORNEY_NAME);
    public final StringPath attorneyOid = addMetadata(createString("attorneyoid"), ATTORNEY_OID);
    public final StringPath channel = addMetadata(createString("channel"), CHANNEL);
    public final StringPath eventidentifier =
            addMetadata(createString("eventidentifier"), EVENT_IDENTIFIER);
    public final NumberPath<Integer> eventStage =
            addMetadata(createNumber("eventStage", Integer.class), EVENT_STAGE);
    public final NumberPath<Integer> eventType =
            addMetadata(createNumber("eventType", Integer.class), EVENT_TYPE);
    public final StringPath hostidentifier =
            addMetadata(createString("hostidentifier"), HOST_IDENTIFIER);
    public final StringPath initiatorname =
            addMetadata(createString("initiatorname"), INITIATOR_NAME);
    public final StringPath initiatoroid =
            addMetadata(createString("initiatoroid"), INITIATOR_OID);
    public final NumberPath<Integer> initiatortype =
            addMetadata(createNumber("initiatortype", Integer.class), INITIATOR_TYPE);
    public final StringPath message = addMetadata(createString("message"), MESSAGE);
    public final StringPath nodeidentifier =
            addMetadata(createString("nodeidentifier"), NODE_IDENTIFIER);
    public final NumberPath<Integer> outcome =
            addMetadata(createNumber("outcome", Integer.class), OUTCOME);
    public final StringPath parameter = addMetadata(createString("parameter"), PARAMETER);
    public final StringPath remotehostaddress =
            addMetadata(createString("remotehostaddress"), REMOTE_HOST_ADDRESS);
    public final StringPath requestidentifier =
            addMetadata(createString("requestidentifier"), REQUEST_IDENTIFIER);
    public final StringPath result = addMetadata(createString("result"), RESULT);
    public final StringPath sessionidentifier =
            addMetadata(createString("sessionidentifier"), SESSION_IDENTIFIER);
    public final StringPath targetname = addMetadata(createString("targetname"), TARGET_NAME);
    public final StringPath targetoid = addMetadata(createString("targetoid"), TARGET_OID);
    public final StringPath targetownername =
            addMetadata(createString("targetownername"), TARGET_OWNER_NAME);
    public final StringPath targetowneroid =
            addMetadata(createString("targetowneroid"), TARGET_OWNER_OID);
    public final NumberPath<Integer> targetownertype =
            addMetadata(createNumber("targetownertype", Integer.class), TARGET_OWNER_TYPE);
    public final NumberPath<Integer> targettype =
            addMetadata(createNumber("targettype", Integer.class), TARGET_TYPE);
    public final StringPath taskidentifier =
            addMetadata(createString("taskidentifier"), TASK_IDENTIFIER);
    public final StringPath taskoid = addMetadata(createString("taskoid"), TASK_OID);
    public final DateTimePath<java.sql.Timestamp> timestampvalue =
            addMetadata(createDateTime("timestampvalue", java.sql.Timestamp.class), TIMESTAMP_VALUE);

    public final com.querydsl.sql.PrimaryKey<MAuditEventRecord> constraint85c = createPrimaryKey(id);
    public final com.querydsl.sql.ForeignKey<QMAuditItem> _auditItemFk = createInvForeignKey(id, "RECORD_ID");
    public final com.querydsl.sql.ForeignKey<QMAuditPropValue> _auditPropValueFk = createInvForeignKey(id, "RECORD_ID");
    public final com.querydsl.sql.ForeignKey<MAuditDelta> _auditDeltaFk = createInvForeignKey(id, "RECORD_ID");
    public final com.querydsl.sql.ForeignKey<QMAuditRefValue> _auditRefValueFk = createInvForeignKey(id, "RECORD_ID");
    public final com.querydsl.sql.ForeignKey<QMAuditResource> _auditResourceFk = createInvForeignKey(id, "RECORD_ID");

    public QAuditEventRecord(String variable) {
        this(variable, "PUBLIC", "M_AUDIT_EVENT");
    }

    public QAuditEventRecord(String variable, String schema, String table) {
        super(MAuditEventRecord.class, forVariable(variable), schema, table);
    }
}
