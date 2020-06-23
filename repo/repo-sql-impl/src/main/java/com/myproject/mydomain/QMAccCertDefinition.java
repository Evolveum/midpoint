package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMAccCertDefinition is a Querydsl query type for QMAccCertDefinition
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMAccCertDefinition extends com.querydsl.sql.RelationalPathBase<QMAccCertDefinition> {

    private static final long serialVersionUID = -566765396;

    public static final QMAccCertDefinition mAccCertDefinition = new QMAccCertDefinition("M_ACC_CERT_DEFINITION");

    public final StringPath handleruri = createString("handleruri");

    public final DateTimePath<java.sql.Timestamp> lastcampaignclosedtimestamp = createDateTime("lastcampaignclosedtimestamp", java.sql.Timestamp.class);

    public final DateTimePath<java.sql.Timestamp> lastcampaignstartedtimestamp = createDateTime("lastcampaignstartedtimestamp", java.sql.Timestamp.class);

    public final StringPath nameNorm = createString("nameNorm");

    public final StringPath nameOrig = createString("nameOrig");

    public final StringPath oid = createString("oid");

    public final NumberPath<Integer> ownerrefdefinitionTargettype = createNumber("ownerrefdefinitionTargettype", Integer.class);

    public final StringPath ownerrefRelation = createString("ownerrefRelation");

    public final StringPath ownerrefTargetoid = createString("ownerrefTargetoid");

    public final NumberPath<Integer> ownerrefType = createNumber("ownerrefType", Integer.class);

    public final com.querydsl.sql.PrimaryKey<QMAccCertDefinition> constraintA = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMObject> accCertDefinitionFk = createForeignKey(oid, "OID");

    public QMAccCertDefinition(String variable) {
        super(QMAccCertDefinition.class, forVariable(variable), "PUBLIC", "M_ACC_CERT_DEFINITION");
        addMetadata();
    }

    public QMAccCertDefinition(String variable, String schema, String table) {
        super(QMAccCertDefinition.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMAccCertDefinition(String variable, String schema) {
        super(QMAccCertDefinition.class, forVariable(variable), schema, "M_ACC_CERT_DEFINITION");
        addMetadata();
    }

    public QMAccCertDefinition(Path<? extends QMAccCertDefinition> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_ACC_CERT_DEFINITION");
        addMetadata();
    }

    public QMAccCertDefinition(PathMetadata metadata) {
        super(QMAccCertDefinition.class, metadata, "PUBLIC", "M_ACC_CERT_DEFINITION");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(handleruri, ColumnMetadata.named("HANDLERURI").withIndex(1).ofType(Types.VARCHAR).withSize(255));
        addMetadata(lastcampaignclosedtimestamp, ColumnMetadata.named("LASTCAMPAIGNCLOSEDTIMESTAMP").withIndex(2).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(lastcampaignstartedtimestamp, ColumnMetadata.named("LASTCAMPAIGNSTARTEDTIMESTAMP").withIndex(3).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(nameNorm, ColumnMetadata.named("NAME_NORM").withIndex(4).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameOrig, ColumnMetadata.named("NAME_ORIG").withIndex(5).ofType(Types.VARCHAR).withSize(255));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(9).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(ownerrefdefinitionTargettype, ColumnMetadata.named("OWNERREFDEFINITION_TARGETTYPE").withIndex(10).ofType(Types.INTEGER).withSize(10));
        addMetadata(ownerrefRelation, ColumnMetadata.named("OWNERREF_RELATION").withIndex(6).ofType(Types.VARCHAR).withSize(157));
        addMetadata(ownerrefTargetoid, ColumnMetadata.named("OWNERREF_TARGETOID").withIndex(7).ofType(Types.VARCHAR).withSize(36));
        addMetadata(ownerrefType, ColumnMetadata.named("OWNERREF_TYPE").withIndex(8).ofType(Types.INTEGER).withSize(10));
    }

}

