package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMAuditEvent is a Querydsl query type for QMAuditEvent
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMAuditEvent extends com.querydsl.sql.RelationalPathBase<QMAuditEvent> {

    private static final long serialVersionUID = -229589301;

    public static final QMAuditEvent mAuditEvent = new QMAuditEvent("M_AUDIT_EVENT");

    public final StringPath attorneyname = createString("attorneyname");

    public final StringPath attorneyoid = createString("attorneyoid");

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

    public final com.querydsl.sql.PrimaryKey<QMAuditEvent> constraint85c = createPrimaryKey(id);

    public final com.querydsl.sql.ForeignKey<QMAuditItem> _auditItemFk = createInvForeignKey(id, "RECORD_ID");

    public final com.querydsl.sql.ForeignKey<QMAuditPropValue> _auditPropValueFk = createInvForeignKey(id, "RECORD_ID");

    public final com.querydsl.sql.ForeignKey<QMAuditDelta> _auditDeltaFk = createInvForeignKey(id, "RECORD_ID");

    public final com.querydsl.sql.ForeignKey<QMAuditRefValue> _auditRefValueFk = createInvForeignKey(id, "RECORD_ID");

    public final com.querydsl.sql.ForeignKey<QMAuditResource> _auditResourceFk = createInvForeignKey(id, "RECORD_ID");

    public QMAuditEvent(String variable) {
        super(QMAuditEvent.class, forVariable(variable), "PUBLIC", "M_AUDIT_EVENT");
        addMetadata();
    }

    public QMAuditEvent(String variable, String schema, String table) {
        super(QMAuditEvent.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMAuditEvent(String variable, String schema) {
        super(QMAuditEvent.class, forVariable(variable), schema, "M_AUDIT_EVENT");
        addMetadata();
    }

    public QMAuditEvent(Path<? extends QMAuditEvent> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_AUDIT_EVENT");
        addMetadata();
    }

    public QMAuditEvent(PathMetadata metadata) {
        super(QMAuditEvent.class, metadata, "PUBLIC", "M_AUDIT_EVENT");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(attorneyname, ColumnMetadata.named("ATTORNEYNAME").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(attorneyoid, ColumnMetadata.named("ATTORNEYOID").withIndex(3).ofType(Types.VARCHAR).withSize(36));
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

