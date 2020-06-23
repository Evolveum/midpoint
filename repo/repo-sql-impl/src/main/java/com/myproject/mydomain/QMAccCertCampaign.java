package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMAccCertCampaign is a Querydsl query type for QMAccCertCampaign
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMAccCertCampaign extends com.querydsl.sql.RelationalPathBase<QMAccCertCampaign> {

    private static final long serialVersionUID = 740992681;

    public static final QMAccCertCampaign mAccCertCampaign = new QMAccCertCampaign("M_ACC_CERT_CAMPAIGN");

    public final StringPath definitionrefRelation = createString("definitionrefRelation");

    public final StringPath definitionrefTargetoid = createString("definitionrefTargetoid");

    public final NumberPath<Integer> definitionrefTargettype = createNumber("definitionrefTargettype", Integer.class);

    public final NumberPath<Integer> definitionrefType = createNumber("definitionrefType", Integer.class);

    public final DateTimePath<java.sql.Timestamp> endtimestamp = createDateTime("endtimestamp", java.sql.Timestamp.class);

    public final StringPath handleruri = createString("handleruri");

    public final NumberPath<Integer> iteration = createNumber("iteration", Integer.class);

    public final StringPath nameNorm = createString("nameNorm");

    public final StringPath nameOrig = createString("nameOrig");

    public final StringPath oid = createString("oid");

    public final NumberPath<Integer> ownerrefcampaignTargettype = createNumber("ownerrefcampaignTargettype", Integer.class);

    public final StringPath ownerrefRelation = createString("ownerrefRelation");

    public final StringPath ownerrefTargetoid = createString("ownerrefTargetoid");

    public final NumberPath<Integer> ownerrefType = createNumber("ownerrefType", Integer.class);

    public final NumberPath<Integer> stagenumber = createNumber("stagenumber", Integer.class);

    public final DateTimePath<java.sql.Timestamp> starttimestamp = createDateTime("starttimestamp", java.sql.Timestamp.class);

    public final NumberPath<Integer> state = createNumber("state", Integer.class);

    public final com.querydsl.sql.PrimaryKey<QMAccCertCampaign> constraint2 = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMObject> accCertCampaignFk = createForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMAccCertCase> _accCertCaseOwnerFk = createInvForeignKey(oid, "OWNER_OID");

    public QMAccCertCampaign(String variable) {
        super(QMAccCertCampaign.class, forVariable(variable), "PUBLIC", "M_ACC_CERT_CAMPAIGN");
        addMetadata();
    }

    public QMAccCertCampaign(String variable, String schema, String table) {
        super(QMAccCertCampaign.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMAccCertCampaign(String variable, String schema) {
        super(QMAccCertCampaign.class, forVariable(variable), schema, "M_ACC_CERT_CAMPAIGN");
        addMetadata();
    }

    public QMAccCertCampaign(Path<? extends QMAccCertCampaign> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_ACC_CERT_CAMPAIGN");
        addMetadata();
    }

    public QMAccCertCampaign(PathMetadata metadata) {
        super(QMAccCertCampaign.class, metadata, "PUBLIC", "M_ACC_CERT_CAMPAIGN");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(definitionrefRelation, ColumnMetadata.named("DEFINITIONREF_RELATION").withIndex(1).ofType(Types.VARCHAR).withSize(157));
        addMetadata(definitionrefTargetoid, ColumnMetadata.named("DEFINITIONREF_TARGETOID").withIndex(2).ofType(Types.VARCHAR).withSize(36));
        addMetadata(definitionrefTargettype, ColumnMetadata.named("DEFINITIONREF_TARGETTYPE").withIndex(16).ofType(Types.INTEGER).withSize(10));
        addMetadata(definitionrefType, ColumnMetadata.named("DEFINITIONREF_TYPE").withIndex(3).ofType(Types.INTEGER).withSize(10));
        addMetadata(endtimestamp, ColumnMetadata.named("ENDTIMESTAMP").withIndex(4).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(handleruri, ColumnMetadata.named("HANDLERURI").withIndex(5).ofType(Types.VARCHAR).withSize(255));
        addMetadata(iteration, ColumnMetadata.named("ITERATION").withIndex(6).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(nameNorm, ColumnMetadata.named("NAME_NORM").withIndex(7).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameOrig, ColumnMetadata.named("NAME_ORIG").withIndex(8).ofType(Types.VARCHAR).withSize(255));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(15).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(ownerrefcampaignTargettype, ColumnMetadata.named("OWNERREFCAMPAIGN_TARGETTYPE").withIndex(17).ofType(Types.INTEGER).withSize(10));
        addMetadata(ownerrefRelation, ColumnMetadata.named("OWNERREF_RELATION").withIndex(9).ofType(Types.VARCHAR).withSize(157));
        addMetadata(ownerrefTargetoid, ColumnMetadata.named("OWNERREF_TARGETOID").withIndex(10).ofType(Types.VARCHAR).withSize(36));
        addMetadata(ownerrefType, ColumnMetadata.named("OWNERREF_TYPE").withIndex(11).ofType(Types.INTEGER).withSize(10));
        addMetadata(stagenumber, ColumnMetadata.named("STAGENUMBER").withIndex(12).ofType(Types.INTEGER).withSize(10));
        addMetadata(starttimestamp, ColumnMetadata.named("STARTTIMESTAMP").withIndex(13).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(state, ColumnMetadata.named("STATE").withIndex(14).ofType(Types.INTEGER).withSize(10));
    }

}

