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
 * QMAccCertCase is a Querydsl query type for QMAccCertCase
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMAccCertCase extends com.querydsl.sql.RelationalPathBase<QMAccCertCase> {

    private static final long serialVersionUID = 1577899337;

    public static final QMAccCertCase mAccCertCase = new QMAccCertCase("M_ACC_CERT_CASE");

    public final NumberPath<Integer> administrativestatus = createNumber("administrativestatus", Integer.class);

    public final DateTimePath<java.sql.Timestamp> archivetimestamp = createDateTime("archivetimestamp", java.sql.Timestamp.class);

    public final StringPath currentstageoutcome = createString("currentstageoutcome");

    public final StringPath disablereason = createString("disablereason");

    public final DateTimePath<java.sql.Timestamp> disabletimestamp = createDateTime("disabletimestamp", java.sql.Timestamp.class);

    public final NumberPath<Integer> effectivestatus = createNumber("effectivestatus", Integer.class);

    public final DateTimePath<java.sql.Timestamp> enabletimestamp = createDateTime("enabletimestamp", java.sql.Timestamp.class);

    public final SimplePath<java.sql.Blob> fullobject = createSimple("fullobject", java.sql.Blob.class);

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final NumberPath<Integer> iteration = createNumber("iteration", Integer.class);

    public final StringPath objectrefRelation = createString("objectrefRelation");

    public final StringPath objectrefTargetoid = createString("objectrefTargetoid");

    public final NumberPath<Integer> objectrefTargettype = createNumber("objectrefTargettype", Integer.class);

    public final NumberPath<Integer> objectrefType = createNumber("objectrefType", Integer.class);

    public final StringPath orgrefRelation = createString("orgrefRelation");

    public final StringPath orgrefTargetoid = createString("orgrefTargetoid");

    public final NumberPath<Integer> orgrefTargettype = createNumber("orgrefTargettype", Integer.class);

    public final NumberPath<Integer> orgrefType = createNumber("orgrefType", Integer.class);

    public final StringPath outcome = createString("outcome");

    public final StringPath ownerOid = createString("ownerOid");

    public final DateTimePath<java.sql.Timestamp> remediedtimestamp = createDateTime("remediedtimestamp", java.sql.Timestamp.class);

    public final DateTimePath<java.sql.Timestamp> reviewdeadline = createDateTime("reviewdeadline", java.sql.Timestamp.class);

    public final DateTimePath<java.sql.Timestamp> reviewrequestedtimestamp = createDateTime("reviewrequestedtimestamp", java.sql.Timestamp.class);

    public final NumberPath<Integer> stagenumber = createNumber("stagenumber", Integer.class);

    public final StringPath targetrefRelation = createString("targetrefRelation");

    public final StringPath targetrefTargetoid = createString("targetrefTargetoid");

    public final NumberPath<Integer> targetrefTargettype = createNumber("targetrefTargettype", Integer.class);

    public final NumberPath<Integer> targetrefType = createNumber("targetrefType", Integer.class);

    public final StringPath tenantrefRelation = createString("tenantrefRelation");

    public final StringPath tenantrefTargetoid = createString("tenantrefTargetoid");

    public final NumberPath<Integer> tenantrefTargettype = createNumber("tenantrefTargettype", Integer.class);

    public final NumberPath<Integer> tenantrefType = createNumber("tenantrefType", Integer.class);

    public final DateTimePath<java.sql.Timestamp> validfrom = createDateTime("validfrom", java.sql.Timestamp.class);

    public final DateTimePath<java.sql.Timestamp> validitychangetimestamp = createDateTime("validitychangetimestamp", java.sql.Timestamp.class);

    public final NumberPath<Integer> validitystatus = createNumber("validitystatus", Integer.class);

    public final DateTimePath<java.sql.Timestamp> validto = createDateTime("validto", java.sql.Timestamp.class);

    public final com.querydsl.sql.PrimaryKey<QMAccCertCase> constraint5 = createPrimaryKey(id, ownerOid);

    public final com.querydsl.sql.ForeignKey<QMAccCertCampaign> accCertCaseOwnerFk = createForeignKey(ownerOid, "OID");

    public final com.querydsl.sql.ForeignKey<QMAccCertWi> _accCertWiOwnerFk = createInvForeignKey(Arrays.asList(id, ownerOid), Arrays.asList("OWNER_ID", "OWNER_OWNER_OID"));

    public QMAccCertCase(String variable) {
        super(QMAccCertCase.class, forVariable(variable), "PUBLIC", "M_ACC_CERT_CASE");
        addMetadata();
    }

    public QMAccCertCase(String variable, String schema, String table) {
        super(QMAccCertCase.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMAccCertCase(String variable, String schema) {
        super(QMAccCertCase.class, forVariable(variable), schema, "M_ACC_CERT_CASE");
        addMetadata();
    }

    public QMAccCertCase(Path<? extends QMAccCertCase> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_ACC_CERT_CASE");
        addMetadata();
    }

    public QMAccCertCase(PathMetadata metadata) {
        super(QMAccCertCase.class, metadata, "PUBLIC", "M_ACC_CERT_CASE");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(administrativestatus, ColumnMetadata.named("ADMINISTRATIVESTATUS").withIndex(3).ofType(Types.INTEGER).withSize(10));
        addMetadata(archivetimestamp, ColumnMetadata.named("ARCHIVETIMESTAMP").withIndex(4).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(currentstageoutcome, ColumnMetadata.named("CURRENTSTAGEOUTCOME").withIndex(13).ofType(Types.VARCHAR).withSize(255));
        addMetadata(disablereason, ColumnMetadata.named("DISABLEREASON").withIndex(5).ofType(Types.VARCHAR).withSize(255));
        addMetadata(disabletimestamp, ColumnMetadata.named("DISABLETIMESTAMP").withIndex(6).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(effectivestatus, ColumnMetadata.named("EFFECTIVESTATUS").withIndex(7).ofType(Types.INTEGER).withSize(10));
        addMetadata(enabletimestamp, ColumnMetadata.named("ENABLETIMESTAMP").withIndex(8).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(fullobject, ColumnMetadata.named("FULLOBJECT").withIndex(14).ofType(Types.BLOB).withSize(2147483647));
        addMetadata(id, ColumnMetadata.named("ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(iteration, ColumnMetadata.named("ITERATION").withIndex(15).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(objectrefRelation, ColumnMetadata.named("OBJECTREF_RELATION").withIndex(16).ofType(Types.VARCHAR).withSize(157));
        addMetadata(objectrefTargetoid, ColumnMetadata.named("OBJECTREF_TARGETOID").withIndex(17).ofType(Types.VARCHAR).withSize(36));
        addMetadata(objectrefTargettype, ColumnMetadata.named("OBJECTREF_TARGETTYPE").withIndex(33).ofType(Types.INTEGER).withSize(10));
        addMetadata(objectrefType, ColumnMetadata.named("OBJECTREF_TYPE").withIndex(18).ofType(Types.INTEGER).withSize(10));
        addMetadata(orgrefRelation, ColumnMetadata.named("ORGREF_RELATION").withIndex(19).ofType(Types.VARCHAR).withSize(157));
        addMetadata(orgrefTargetoid, ColumnMetadata.named("ORGREF_TARGETOID").withIndex(20).ofType(Types.VARCHAR).withSize(36));
        addMetadata(orgrefTargettype, ColumnMetadata.named("ORGREF_TARGETTYPE").withIndex(34).ofType(Types.INTEGER).withSize(10));
        addMetadata(orgrefType, ColumnMetadata.named("ORGREF_TYPE").withIndex(21).ofType(Types.INTEGER).withSize(10));
        addMetadata(outcome, ColumnMetadata.named("OUTCOME").withIndex(22).ofType(Types.VARCHAR).withSize(255));
        addMetadata(ownerOid, ColumnMetadata.named("OWNER_OID").withIndex(2).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(remediedtimestamp, ColumnMetadata.named("REMEDIEDTIMESTAMP").withIndex(23).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(reviewdeadline, ColumnMetadata.named("REVIEWDEADLINE").withIndex(24).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(reviewrequestedtimestamp, ColumnMetadata.named("REVIEWREQUESTEDTIMESTAMP").withIndex(25).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(stagenumber, ColumnMetadata.named("STAGENUMBER").withIndex(26).ofType(Types.INTEGER).withSize(10));
        addMetadata(targetrefRelation, ColumnMetadata.named("TARGETREF_RELATION").withIndex(27).ofType(Types.VARCHAR).withSize(157));
        addMetadata(targetrefTargetoid, ColumnMetadata.named("TARGETREF_TARGETOID").withIndex(28).ofType(Types.VARCHAR).withSize(36));
        addMetadata(targetrefTargettype, ColumnMetadata.named("TARGETREF_TARGETTYPE").withIndex(35).ofType(Types.INTEGER).withSize(10));
        addMetadata(targetrefType, ColumnMetadata.named("TARGETREF_TYPE").withIndex(29).ofType(Types.INTEGER).withSize(10));
        addMetadata(tenantrefRelation, ColumnMetadata.named("TENANTREF_RELATION").withIndex(30).ofType(Types.VARCHAR).withSize(157));
        addMetadata(tenantrefTargetoid, ColumnMetadata.named("TENANTREF_TARGETOID").withIndex(31).ofType(Types.VARCHAR).withSize(36));
        addMetadata(tenantrefTargettype, ColumnMetadata.named("TENANTREF_TARGETTYPE").withIndex(36).ofType(Types.INTEGER).withSize(10));
        addMetadata(tenantrefType, ColumnMetadata.named("TENANTREF_TYPE").withIndex(32).ofType(Types.INTEGER).withSize(10));
        addMetadata(validfrom, ColumnMetadata.named("VALIDFROM").withIndex(9).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(validitychangetimestamp, ColumnMetadata.named("VALIDITYCHANGETIMESTAMP").withIndex(11).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(validitystatus, ColumnMetadata.named("VALIDITYSTATUS").withIndex(12).ofType(Types.INTEGER).withSize(10));
        addMetadata(validto, ColumnMetadata.named("VALIDTO").withIndex(10).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
    }

}

