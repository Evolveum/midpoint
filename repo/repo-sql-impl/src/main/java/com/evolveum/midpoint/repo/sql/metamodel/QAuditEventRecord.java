/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.metamodel;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

import java.sql.Types;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
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

    public static final QAuditEventRecord M_AUDIT_EVENT = new QAuditEventRecord("M_AUDIT_EVENT");

    public final StringPath attorneyName = createString("attorneyname");

    public final StringPath attorneyOid = createString("attorneyoid");

    public final StringPath channel = createString("channel");

    public final StringPath eventidentifier = createString("eventidentifier");

    public final NumberPath<Integer> eventstage = createNumber("eventstage", Integer.class);

    public final NumberPath<Integer> eventtype = createNumber("eventtype", Integer.class);

    public final StringPath hostidentifier = createString("hostidentifier");

    public final NumberPath<Long> id = createNumber("id", Long.class);

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
        addMetadata();
    }

    public QAuditEventRecord(String variable, String schema, String table) {
        super(MAuditEventRecord.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QAuditEventRecord(String variable, String schema) {
        super(MAuditEventRecord.class, forVariable(variable), schema, "M_AUDIT_EVENT");
        addMetadata();
    }

    public QAuditEventRecord(Path<? extends MAuditEventRecord> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_AUDIT_EVENT");
        addMetadata();
    }

    public QAuditEventRecord(PathMetadata metadata) {
        super(MAuditEventRecord.class, metadata, "PUBLIC", "M_AUDIT_EVENT");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(attorneyName, ColumnMetadata.named("ATTORNEYNAME").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(attorneyOid, ColumnMetadata.named("ATTORNEYOID").withIndex(3).ofType(Types.VARCHAR).withSize(36));
        addMetadata(channel, ColumnMetadata.named("CHANNEL").withIndex(4).ofType(Types.VARCHAR).withSize(255));
        addMetadata(eventidentifier, ColumnMetadata.named("EVENTIDENTIFIER").withIndex(5).ofType(Types.VARCHAR).withSize(255));
        addMetadata(eventstage, ColumnMetadata.named("EVENTSTAGE").withIndex(6).ofType(Types.INTEGER).withSize(10));
        addMetadata(eventtype, ColumnMetadata.named("EVENTTYPE").withIndex(7).ofType(Types.INTEGER).withSize(10));
        addMetadata(hostidentifier, ColumnMetadata.named("HOSTIDENTIFIER").withIndex(8).ofType(Types.VARCHAR).withSize(255));
        addMetadata(id, ColumnMetadata.named("ID").withIndex(1).ofType(Types.BIGINT).withSize(19).notNull());
        addMetadata(initiatorname, ColumnMetadata.named("INITIATORNAME").withIndex(9).ofType(Types.VARCHAR).withSize(255));
        addMetadata(initiatoroid, ColumnMetadata.named("INITIATOROID").withIndex(10).ofType(Types.VARCHAR).withSize(36));
        addMetadata(initiatortype, ColumnMetadata.named("INITIATORTYPE").withIndex(11).ofType(Types.INTEGER).withSize(10));
        addMetadata(message, ColumnMetadata.named("MESSAGE").withIndex(12).ofType(Types.VARCHAR).withSize(1024));
        addMetadata(nodeidentifier, ColumnMetadata.named("NODEIDENTIFIER").withIndex(13).ofType(Types.VARCHAR).withSize(255));
        addMetadata(outcome, ColumnMetadata.named("OUTCOME").withIndex(14).ofType(Types.INTEGER).withSize(10));
        addMetadata(parameter, ColumnMetadata.named("PARAMETER").withIndex(15).ofType(Types.VARCHAR).withSize(255));
        addMetadata(remotehostaddress, ColumnMetadata.named("REMOTEHOSTADDRESS").withIndex(16).ofType(Types.VARCHAR).withSize(255));
        addMetadata(requestidentifier, ColumnMetadata.named("REQUESTIDENTIFIER").withIndex(17).ofType(Types.VARCHAR).withSize(255));
        addMetadata(result, ColumnMetadata.named("RESULT").withIndex(18).ofType(Types.VARCHAR).withSize(255));
        addMetadata(sessionidentifier, ColumnMetadata.named("SESSIONIDENTIFIER").withIndex(19).ofType(Types.VARCHAR).withSize(255));
        addMetadata(targetname, ColumnMetadata.named("TARGETNAME").withIndex(20).ofType(Types.VARCHAR).withSize(255));
        addMetadata(targetoid, ColumnMetadata.named("TARGETOID").withIndex(21).ofType(Types.VARCHAR).withSize(36));
        addMetadata(targetownername, ColumnMetadata.named("TARGETOWNERNAME").withIndex(22).ofType(Types.VARCHAR).withSize(255));
        addMetadata(targetowneroid, ColumnMetadata.named("TARGETOWNEROID").withIndex(23).ofType(Types.VARCHAR).withSize(36));
        addMetadata(targetownertype, ColumnMetadata.named("TARGETOWNERTYPE").withIndex(24).ofType(Types.INTEGER).withSize(10));
        addMetadata(targettype, ColumnMetadata.named("TARGETTYPE").withIndex(25).ofType(Types.INTEGER).withSize(10));
        addMetadata(taskidentifier, ColumnMetadata.named("TASKIDENTIFIER").withIndex(26).ofType(Types.VARCHAR).withSize(255));
        addMetadata(taskoid, ColumnMetadata.named("TASKOID").withIndex(27).ofType(Types.VARCHAR).withSize(255));
        addMetadata(timestampvalue, ColumnMetadata.named("TIMESTAMPVALUE").withIndex(28).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
    }
}

