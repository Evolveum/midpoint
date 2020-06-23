package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMOrg is a Querydsl query type for QMOrg
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMOrg extends com.querydsl.sql.RelationalPathBase<QMOrg> {

    private static final long serialVersionUID = 1592354072;

    public static final QMOrg mOrg = new QMOrg("M_ORG");

    public final NumberPath<Integer> displayorder = createNumber("displayorder", Integer.class);

    public final StringPath nameNorm = createString("nameNorm");

    public final StringPath nameOrig = createString("nameOrig");

    public final StringPath oid = createString("oid");

    public final BooleanPath tenant = createBoolean("tenant");

    public final com.querydsl.sql.PrimaryKey<QMOrg> constraint46 = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMAbstractRole> orgFk = createForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMOrgOrgType> _orgOrgTypeFk = createInvForeignKey(oid, "ORG_OID");

    public QMOrg(String variable) {
        super(QMOrg.class, forVariable(variable), "PUBLIC", "M_ORG");
        addMetadata();
    }

    public QMOrg(String variable, String schema, String table) {
        super(QMOrg.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMOrg(String variable, String schema) {
        super(QMOrg.class, forVariable(variable), schema, "M_ORG");
        addMetadata();
    }

    public QMOrg(Path<? extends QMOrg> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_ORG");
        addMetadata();
    }

    public QMOrg(PathMetadata metadata) {
        super(QMOrg.class, metadata, "PUBLIC", "M_ORG");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(displayorder, ColumnMetadata.named("DISPLAYORDER").withIndex(1).ofType(Types.INTEGER).withSize(10));
        addMetadata(nameNorm, ColumnMetadata.named("NAME_NORM").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameOrig, ColumnMetadata.named("NAME_ORIG").withIndex(3).ofType(Types.VARCHAR).withSize(255));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(5).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(tenant, ColumnMetadata.named("TENANT").withIndex(4).ofType(Types.BOOLEAN).withSize(1));
    }

}

