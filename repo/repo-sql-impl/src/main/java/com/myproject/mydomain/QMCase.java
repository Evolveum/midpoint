package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMCase is a Querydsl query type for QMCase
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMCase extends com.querydsl.sql.RelationalPathBase<QMCase> {

    private static final long serialVersionUID = 2117962620;

    public static final QMCase mCase = new QMCase("M_CASE");

    public final DateTimePath<java.sql.Timestamp> closetimestamp = createDateTime("closetimestamp", java.sql.Timestamp.class);

    public final StringPath nameNorm = createString("nameNorm");

    public final StringPath nameOrig = createString("nameOrig");

    public final StringPath objectrefRelation = createString("objectrefRelation");

    public final StringPath objectrefTargetoid = createString("objectrefTargetoid");

    public final NumberPath<Integer> objectrefTargettype = createNumber("objectrefTargettype", Integer.class);

    public final NumberPath<Integer> objectrefType = createNumber("objectrefType", Integer.class);

    public final StringPath oid = createString("oid");

    public final StringPath parentrefRelation = createString("parentrefRelation");

    public final StringPath parentrefTargetoid = createString("parentrefTargetoid");

    public final NumberPath<Integer> parentrefTargettype = createNumber("parentrefTargettype", Integer.class);

    public final NumberPath<Integer> parentrefType = createNumber("parentrefType", Integer.class);

    public final StringPath requestorrefRelation = createString("requestorrefRelation");

    public final StringPath requestorrefTargetoid = createString("requestorrefTargetoid");

    public final NumberPath<Integer> requestorrefTargettype = createNumber("requestorrefTargettype", Integer.class);

    public final NumberPath<Integer> requestorrefType = createNumber("requestorrefType", Integer.class);

    public final StringPath state = createString("state");

    public final StringPath targetrefRelation = createString("targetrefRelation");

    public final StringPath targetrefTargetoid = createString("targetrefTargetoid");

    public final NumberPath<Integer> targetrefTargettype = createNumber("targetrefTargettype", Integer.class);

    public final NumberPath<Integer> targetrefType = createNumber("targetrefType", Integer.class);

    public final com.querydsl.sql.PrimaryKey<QMCase> constraint88b = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMObject> caseFk = createForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMCaseWi> _caseWiOwnerFk = createInvForeignKey(oid, "OWNER_OID");

    public QMCase(String variable) {
        super(QMCase.class, forVariable(variable), "PUBLIC", "M_CASE");
        addMetadata();
    }

    public QMCase(String variable, String schema, String table) {
        super(QMCase.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMCase(String variable, String schema) {
        super(QMCase.class, forVariable(variable), schema, "M_CASE");
        addMetadata();
    }

    public QMCase(Path<? extends QMCase> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_CASE");
        addMetadata();
    }

    public QMCase(PathMetadata metadata) {
        super(QMCase.class, metadata, "PUBLIC", "M_CASE");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(closetimestamp, ColumnMetadata.named("CLOSETIMESTAMP").withIndex(1).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(nameNorm, ColumnMetadata.named("NAME_NORM").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameOrig, ColumnMetadata.named("NAME_ORIG").withIndex(3).ofType(Types.VARCHAR).withSize(255));
        addMetadata(objectrefRelation, ColumnMetadata.named("OBJECTREF_RELATION").withIndex(4).ofType(Types.VARCHAR).withSize(157));
        addMetadata(objectrefTargetoid, ColumnMetadata.named("OBJECTREF_TARGETOID").withIndex(5).ofType(Types.VARCHAR).withSize(36));
        addMetadata(objectrefTargettype, ColumnMetadata.named("OBJECTREF_TARGETTYPE").withIndex(18).ofType(Types.INTEGER).withSize(10));
        addMetadata(objectrefType, ColumnMetadata.named("OBJECTREF_TYPE").withIndex(6).ofType(Types.INTEGER).withSize(10));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(17).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(parentrefRelation, ColumnMetadata.named("PARENTREF_RELATION").withIndex(7).ofType(Types.VARCHAR).withSize(157));
        addMetadata(parentrefTargetoid, ColumnMetadata.named("PARENTREF_TARGETOID").withIndex(8).ofType(Types.VARCHAR).withSize(36));
        addMetadata(parentrefTargettype, ColumnMetadata.named("PARENTREF_TARGETTYPE").withIndex(19).ofType(Types.INTEGER).withSize(10));
        addMetadata(parentrefType, ColumnMetadata.named("PARENTREF_TYPE").withIndex(9).ofType(Types.INTEGER).withSize(10));
        addMetadata(requestorrefRelation, ColumnMetadata.named("REQUESTORREF_RELATION").withIndex(10).ofType(Types.VARCHAR).withSize(157));
        addMetadata(requestorrefTargetoid, ColumnMetadata.named("REQUESTORREF_TARGETOID").withIndex(11).ofType(Types.VARCHAR).withSize(36));
        addMetadata(requestorrefTargettype, ColumnMetadata.named("REQUESTORREF_TARGETTYPE").withIndex(20).ofType(Types.INTEGER).withSize(10));
        addMetadata(requestorrefType, ColumnMetadata.named("REQUESTORREF_TYPE").withIndex(12).ofType(Types.INTEGER).withSize(10));
        addMetadata(state, ColumnMetadata.named("STATE").withIndex(13).ofType(Types.VARCHAR).withSize(255));
        addMetadata(targetrefRelation, ColumnMetadata.named("TARGETREF_RELATION").withIndex(14).ofType(Types.VARCHAR).withSize(157));
        addMetadata(targetrefTargetoid, ColumnMetadata.named("TARGETREF_TARGETOID").withIndex(15).ofType(Types.VARCHAR).withSize(36));
        addMetadata(targetrefTargettype, ColumnMetadata.named("TARGETREF_TARGETTYPE").withIndex(21).ofType(Types.INTEGER).withSize(10));
        addMetadata(targetrefType, ColumnMetadata.named("TARGETREF_TYPE").withIndex(16).ofType(Types.INTEGER).withSize(10));
    }

}

