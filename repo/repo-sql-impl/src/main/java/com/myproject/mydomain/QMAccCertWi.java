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
 * QMAccCertWi is a Querydsl query type for QMAccCertWi
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMAccCertWi extends com.querydsl.sql.RelationalPathBase<QMAccCertWi> {

    private static final long serialVersionUID = 162536235;

    public static final QMAccCertWi mAccCertWi = new QMAccCertWi("M_ACC_CERT_WI");

    public final DateTimePath<java.sql.Timestamp> closetimestamp = createDateTime("closetimestamp", java.sql.Timestamp.class);

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final NumberPath<Integer> iteration = createNumber("iteration", Integer.class);

    public final StringPath outcome = createString("outcome");

    public final DateTimePath<java.sql.Timestamp> outputchangetimestamp = createDateTime("outputchangetimestamp", java.sql.Timestamp.class);

    public final NumberPath<Integer> ownerId = createNumber("ownerId", Integer.class);

    public final StringPath ownerOwnerOid = createString("ownerOwnerOid");

    public final StringPath performerrefRelation = createString("performerrefRelation");

    public final StringPath performerrefTargetoid = createString("performerrefTargetoid");

    public final NumberPath<Integer> performerrefTargettype = createNumber("performerrefTargettype", Integer.class);

    public final NumberPath<Integer> performerrefType = createNumber("performerrefType", Integer.class);

    public final NumberPath<Integer> stagenumber = createNumber("stagenumber", Integer.class);

    public final com.querydsl.sql.PrimaryKey<QMAccCertWi> constraint2c = createPrimaryKey(id, ownerId, ownerOwnerOid);

    public final com.querydsl.sql.ForeignKey<QMAccCertCase> accCertWiOwnerFk = createForeignKey(Arrays.asList(ownerId, ownerOwnerOid), Arrays.asList("ID", "OWNER_OID"));

    public final com.querydsl.sql.ForeignKey<QMAccCertWiReference> _accCertWiRefOwnerFk = createInvForeignKey(Arrays.asList(id, ownerId, ownerOwnerOid), Arrays.asList("OWNER_ID", "OWNER_OWNER_ID", "OWNER_OWNER_OWNER_OID"));

    public QMAccCertWi(String variable) {
        super(QMAccCertWi.class, forVariable(variable), "PUBLIC", "M_ACC_CERT_WI");
        addMetadata();
    }

    public QMAccCertWi(String variable, String schema, String table) {
        super(QMAccCertWi.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMAccCertWi(String variable, String schema) {
        super(QMAccCertWi.class, forVariable(variable), schema, "M_ACC_CERT_WI");
        addMetadata();
    }

    public QMAccCertWi(Path<? extends QMAccCertWi> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_ACC_CERT_WI");
        addMetadata();
    }

    public QMAccCertWi(PathMetadata metadata) {
        super(QMAccCertWi.class, metadata, "PUBLIC", "M_ACC_CERT_WI");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(closetimestamp, ColumnMetadata.named("CLOSETIMESTAMP").withIndex(4).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(id, ColumnMetadata.named("ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(iteration, ColumnMetadata.named("ITERATION").withIndex(5).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(outcome, ColumnMetadata.named("OUTCOME").withIndex(6).ofType(Types.VARCHAR).withSize(255));
        addMetadata(outputchangetimestamp, ColumnMetadata.named("OUTPUTCHANGETIMESTAMP").withIndex(7).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(ownerId, ColumnMetadata.named("OWNER_ID").withIndex(2).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(ownerOwnerOid, ColumnMetadata.named("OWNER_OWNER_OID").withIndex(3).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(performerrefRelation, ColumnMetadata.named("PERFORMERREF_RELATION").withIndex(8).ofType(Types.VARCHAR).withSize(157));
        addMetadata(performerrefTargetoid, ColumnMetadata.named("PERFORMERREF_TARGETOID").withIndex(9).ofType(Types.VARCHAR).withSize(36));
        addMetadata(performerrefTargettype, ColumnMetadata.named("PERFORMERREF_TARGETTYPE").withIndex(12).ofType(Types.INTEGER).withSize(10));
        addMetadata(performerrefType, ColumnMetadata.named("PERFORMERREF_TYPE").withIndex(10).ofType(Types.INTEGER).withSize(10));
        addMetadata(stagenumber, ColumnMetadata.named("STAGENUMBER").withIndex(11).ofType(Types.INTEGER).withSize(10));
    }

}

