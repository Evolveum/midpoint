package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMAuditDelta is a Querydsl query type for QMAuditDelta
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMAuditDelta extends com.querydsl.sql.RelationalPathBase<QMAuditDelta> {

    private static final long serialVersionUID = -231012375;

    public static final QMAuditDelta mAuditDelta = new QMAuditDelta("M_AUDIT_DELTA");

    public final StringPath checksum = createString("checksum");

    public final SimplePath<java.sql.Blob> delta = createSimple("delta", java.sql.Blob.class);

    public final StringPath deltaoid = createString("deltaoid");

    public final NumberPath<Integer> deltatype = createNumber("deltatype", Integer.class);

    public final SimplePath<java.sql.Blob> fullresult = createSimple("fullresult", java.sql.Blob.class);

    public final StringPath objectnameNorm = createString("objectnameNorm");

    public final StringPath objectnameOrig = createString("objectnameOrig");

    public final NumberPath<Long> recordId = createNumber("recordId", Long.class);

    public final StringPath resourcenameNorm = createString("resourcenameNorm");

    public final StringPath resourcenameOrig = createString("resourcenameOrig");

    public final StringPath resourceoid = createString("resourceoid");

    public final NumberPath<Integer> status = createNumber("status", Integer.class);

    public final com.querydsl.sql.PrimaryKey<QMAuditDelta> constraint85 = createPrimaryKey(checksum, recordId);

    public final com.querydsl.sql.ForeignKey<QMAuditEvent> auditDeltaFk = createForeignKey(recordId, "ID");

    public QMAuditDelta(String variable) {
        super(QMAuditDelta.class, forVariable(variable), "PUBLIC", "M_AUDIT_DELTA");
        addMetadata();
    }

    public QMAuditDelta(String variable, String schema, String table) {
        super(QMAuditDelta.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMAuditDelta(String variable, String schema) {
        super(QMAuditDelta.class, forVariable(variable), schema, "M_AUDIT_DELTA");
        addMetadata();
    }

    public QMAuditDelta(Path<? extends QMAuditDelta> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_AUDIT_DELTA");
        addMetadata();
    }

    public QMAuditDelta(PathMetadata metadata) {
        super(QMAuditDelta.class, metadata, "PUBLIC", "M_AUDIT_DELTA");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(checksum, ColumnMetadata.named("CHECKSUM").withIndex(1).ofType(Types.VARCHAR).withSize(32).notNull());
        addMetadata(delta, ColumnMetadata.named("DELTA").withIndex(3).ofType(Types.BLOB).withSize(2147483647));
        addMetadata(deltaoid, ColumnMetadata.named("DELTAOID").withIndex(4).ofType(Types.VARCHAR).withSize(36));
        addMetadata(deltatype, ColumnMetadata.named("DELTATYPE").withIndex(5).ofType(Types.INTEGER).withSize(10));
        addMetadata(fullresult, ColumnMetadata.named("FULLRESULT").withIndex(6).ofType(Types.BLOB).withSize(2147483647));
        addMetadata(objectnameNorm, ColumnMetadata.named("OBJECTNAME_NORM").withIndex(7).ofType(Types.VARCHAR).withSize(255));
        addMetadata(objectnameOrig, ColumnMetadata.named("OBJECTNAME_ORIG").withIndex(8).ofType(Types.VARCHAR).withSize(255));
        addMetadata(recordId, ColumnMetadata.named("RECORD_ID").withIndex(2).ofType(Types.BIGINT).withSize(19).notNull());
        addMetadata(resourcenameNorm, ColumnMetadata.named("RESOURCENAME_NORM").withIndex(9).ofType(Types.VARCHAR).withSize(255));
        addMetadata(resourcenameOrig, ColumnMetadata.named("RESOURCENAME_ORIG").withIndex(10).ofType(Types.VARCHAR).withSize(255));
        addMetadata(resourceoid, ColumnMetadata.named("RESOURCEOID").withIndex(11).ofType(Types.VARCHAR).withSize(36));
        addMetadata(status, ColumnMetadata.named("STATUS").withIndex(12).ofType(Types.INTEGER).withSize(10));
    }

}

