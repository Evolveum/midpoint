package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMShadow is a Querydsl query type for QMShadow
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMShadow extends com.querydsl.sql.RelationalPathBase<QMShadow> {

    private static final long serialVersionUID = 11576940;

    public static final QMShadow mShadow = new QMShadow("M_SHADOW");

    public final NumberPath<Integer> attemptnumber = createNumber("attemptnumber", Integer.class);

    public final BooleanPath dead = createBoolean("dead");

    public final BooleanPath exist = createBoolean("exist");

    public final NumberPath<Integer> failedoperationtype = createNumber("failedoperationtype", Integer.class);

    public final DateTimePath<java.sql.Timestamp> fullsynchronizationtimestamp = createDateTime("fullsynchronizationtimestamp", java.sql.Timestamp.class);

    public final StringPath intent = createString("intent");

    public final NumberPath<Integer> kind = createNumber("kind", Integer.class);

    public final StringPath nameNorm = createString("nameNorm");

    public final StringPath nameOrig = createString("nameOrig");

    public final StringPath objectclass = createString("objectclass");

    public final StringPath oid = createString("oid");

    public final NumberPath<Integer> pendingoperationcount = createNumber("pendingoperationcount", Integer.class);

    public final StringPath primaryidentifiervalue = createString("primaryidentifiervalue");

    public final StringPath resourcerefRelation = createString("resourcerefRelation");

    public final StringPath resourcerefTargetoid = createString("resourcerefTargetoid");

    public final NumberPath<Integer> resourcerefTargettype = createNumber("resourcerefTargettype", Integer.class);

    public final NumberPath<Integer> resourcerefType = createNumber("resourcerefType", Integer.class);

    public final NumberPath<Integer> status = createNumber("status", Integer.class);

    public final NumberPath<Integer> synchronizationsituation = createNumber("synchronizationsituation", Integer.class);

    public final DateTimePath<java.sql.Timestamp> synchronizationtimestamp = createDateTime("synchronizationtimestamp", java.sql.Timestamp.class);

    public final com.querydsl.sql.PrimaryKey<QMShadow> constraint71e = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMObject> shadowFk = createForeignKey(oid, "OID");

    public QMShadow(String variable) {
        super(QMShadow.class, forVariable(variable), "PUBLIC", "M_SHADOW");
        addMetadata();
    }

    public QMShadow(String variable, String schema, String table) {
        super(QMShadow.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMShadow(String variable, String schema) {
        super(QMShadow.class, forVariable(variable), schema, "M_SHADOW");
        addMetadata();
    }

    public QMShadow(Path<? extends QMShadow> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_SHADOW");
        addMetadata();
    }

    public QMShadow(PathMetadata metadata) {
        super(QMShadow.class, metadata, "PUBLIC", "M_SHADOW");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(attemptnumber, ColumnMetadata.named("ATTEMPTNUMBER").withIndex(1).ofType(Types.INTEGER).withSize(10));
        addMetadata(dead, ColumnMetadata.named("DEAD").withIndex(2).ofType(Types.BOOLEAN).withSize(1));
        addMetadata(exist, ColumnMetadata.named("EXIST").withIndex(3).ofType(Types.BOOLEAN).withSize(1));
        addMetadata(failedoperationtype, ColumnMetadata.named("FAILEDOPERATIONTYPE").withIndex(4).ofType(Types.INTEGER).withSize(10));
        addMetadata(fullsynchronizationtimestamp, ColumnMetadata.named("FULLSYNCHRONIZATIONTIMESTAMP").withIndex(5).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(intent, ColumnMetadata.named("INTENT").withIndex(6).ofType(Types.VARCHAR).withSize(255));
        addMetadata(kind, ColumnMetadata.named("KIND").withIndex(7).ofType(Types.INTEGER).withSize(10));
        addMetadata(nameNorm, ColumnMetadata.named("NAME_NORM").withIndex(8).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameOrig, ColumnMetadata.named("NAME_ORIG").withIndex(9).ofType(Types.VARCHAR).withSize(255));
        addMetadata(objectclass, ColumnMetadata.named("OBJECTCLASS").withIndex(10).ofType(Types.VARCHAR).withSize(157));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(19).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(pendingoperationcount, ColumnMetadata.named("PENDINGOPERATIONCOUNT").withIndex(11).ofType(Types.INTEGER).withSize(10));
        addMetadata(primaryidentifiervalue, ColumnMetadata.named("PRIMARYIDENTIFIERVALUE").withIndex(12).ofType(Types.VARCHAR).withSize(255));
        addMetadata(resourcerefRelation, ColumnMetadata.named("RESOURCEREF_RELATION").withIndex(13).ofType(Types.VARCHAR).withSize(157));
        addMetadata(resourcerefTargetoid, ColumnMetadata.named("RESOURCEREF_TARGETOID").withIndex(14).ofType(Types.VARCHAR).withSize(36));
        addMetadata(resourcerefTargettype, ColumnMetadata.named("RESOURCEREF_TARGETTYPE").withIndex(20).ofType(Types.INTEGER).withSize(10));
        addMetadata(resourcerefType, ColumnMetadata.named("RESOURCEREF_TYPE").withIndex(15).ofType(Types.INTEGER).withSize(10));
        addMetadata(status, ColumnMetadata.named("STATUS").withIndex(16).ofType(Types.INTEGER).withSize(10));
        addMetadata(synchronizationsituation, ColumnMetadata.named("SYNCHRONIZATIONSITUATION").withIndex(17).ofType(Types.INTEGER).withSize(10));
        addMetadata(synchronizationtimestamp, ColumnMetadata.named("SYNCHRONIZATIONTIMESTAMP").withIndex(18).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
    }

}

