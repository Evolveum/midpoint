package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import java.util.*;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMCaseWi is a Querydsl query type for QMCaseWi
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMCaseWi extends com.querydsl.sql.RelationalPathBase<QMCaseWi> {

    private static final long serialVersionUID = -452417682;

    public static final QMCaseWi mCaseWi = new QMCaseWi("M_CASE_WI");

    public final DateTimePath<java.sql.Timestamp> closetimestamp = createDateTime("closetimestamp", java.sql.Timestamp.class);

    public final DateTimePath<java.sql.Timestamp> createtimestamp = createDateTime("createtimestamp", java.sql.Timestamp.class);

    public final DateTimePath<java.sql.Timestamp> deadline = createDateTime("deadline", java.sql.Timestamp.class);

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final StringPath originalassigneerefRelation = createString("originalassigneerefRelation");

    public final StringPath originalassigneerefTargetoid = createString("originalassigneerefTargetoid");

    public final NumberPath<Integer> originalassigneerefTargettype = createNumber("originalassigneerefTargettype", Integer.class);

    public final NumberPath<Integer> originalassigneerefType = createNumber("originalassigneerefType", Integer.class);

    public final StringPath outcome = createString("outcome");

    public final StringPath ownerOid = createString("ownerOid");

    public final StringPath performerrefRelation = createString("performerrefRelation");

    public final StringPath performerrefTargetoid = createString("performerrefTargetoid");

    public final NumberPath<Integer> performerrefTargettype = createNumber("performerrefTargettype", Integer.class);

    public final NumberPath<Integer> performerrefType = createNumber("performerrefType", Integer.class);

    public final NumberPath<Integer> stagenumber = createNumber("stagenumber", Integer.class);

    public final com.querydsl.sql.PrimaryKey<QMCaseWi> constraint71 = createPrimaryKey(id, ownerOid);

    public final com.querydsl.sql.ForeignKey<QMCase> caseWiOwnerFk = createForeignKey(ownerOid, "OID");

    public final com.querydsl.sql.ForeignKey<QMCaseWiReference> _caseWiReferenceOwnerFk = createInvForeignKey(Arrays.asList(id, ownerOid), Arrays.asList("OWNER_ID", "OWNER_OWNER_OID"));

    public QMCaseWi(String variable) {
        super(QMCaseWi.class, forVariable(variable), "PUBLIC", "M_CASE_WI");
        addMetadata();
    }

    public QMCaseWi(String variable, String schema, String table) {
        super(QMCaseWi.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMCaseWi(String variable, String schema) {
        super(QMCaseWi.class, forVariable(variable), schema, "M_CASE_WI");
        addMetadata();
    }

    public QMCaseWi(Path<? extends QMCaseWi> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_CASE_WI");
        addMetadata();
    }

    public QMCaseWi(PathMetadata metadata) {
        super(QMCaseWi.class, metadata, "PUBLIC", "M_CASE_WI");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(closetimestamp, ColumnMetadata.named("CLOSETIMESTAMP").withIndex(3).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(createtimestamp, ColumnMetadata.named("CREATETIMESTAMP").withIndex(4).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(deadline, ColumnMetadata.named("DEADLINE").withIndex(5).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(id, ColumnMetadata.named("ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(originalassigneerefRelation, ColumnMetadata.named("ORIGINALASSIGNEEREF_RELATION").withIndex(6).ofType(Types.VARCHAR).withSize(157));
        addMetadata(originalassigneerefTargetoid, ColumnMetadata.named("ORIGINALASSIGNEEREF_TARGETOID").withIndex(7).ofType(Types.VARCHAR).withSize(36));
        addMetadata(originalassigneerefTargettype, ColumnMetadata.named("ORIGINALASSIGNEEREF_TARGETTYPE").withIndex(14).ofType(Types.INTEGER).withSize(10));
        addMetadata(originalassigneerefType, ColumnMetadata.named("ORIGINALASSIGNEEREF_TYPE").withIndex(8).ofType(Types.INTEGER).withSize(10));
        addMetadata(outcome, ColumnMetadata.named("OUTCOME").withIndex(9).ofType(Types.VARCHAR).withSize(255));
        addMetadata(ownerOid, ColumnMetadata.named("OWNER_OID").withIndex(2).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(performerrefRelation, ColumnMetadata.named("PERFORMERREF_RELATION").withIndex(10).ofType(Types.VARCHAR).withSize(157));
        addMetadata(performerrefTargetoid, ColumnMetadata.named("PERFORMERREF_TARGETOID").withIndex(11).ofType(Types.VARCHAR).withSize(36));
        addMetadata(performerrefTargettype, ColumnMetadata.named("PERFORMERREF_TARGETTYPE").withIndex(15).ofType(Types.INTEGER).withSize(10));
        addMetadata(performerrefType, ColumnMetadata.named("PERFORMERREF_TYPE").withIndex(12).ofType(Types.INTEGER).withSize(10));
        addMetadata(stagenumber, ColumnMetadata.named("STAGENUMBER").withIndex(13).ofType(Types.INTEGER).withSize(10));
    }

}

