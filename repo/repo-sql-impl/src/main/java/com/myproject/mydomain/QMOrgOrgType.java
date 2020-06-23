package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMOrgOrgType is a Querydsl query type for QMOrgOrgType
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMOrgOrgType extends com.querydsl.sql.RelationalPathBase<QMOrgOrgType> {

    private static final long serialVersionUID = 1747937286;

    public static final QMOrgOrgType mOrgOrgType = new QMOrgOrgType("M_ORG_ORG_TYPE");

    public final StringPath orgOid = createString("orgOid");

    public final StringPath orgtype = createString("orgtype");

    public final com.querydsl.sql.ForeignKey<QMOrg> orgOrgTypeFk = createForeignKey(orgOid, "OID");

    public QMOrgOrgType(String variable) {
        super(QMOrgOrgType.class, forVariable(variable), "PUBLIC", "M_ORG_ORG_TYPE");
        addMetadata();
    }

    public QMOrgOrgType(String variable, String schema, String table) {
        super(QMOrgOrgType.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMOrgOrgType(String variable, String schema) {
        super(QMOrgOrgType.class, forVariable(variable), schema, "M_ORG_ORG_TYPE");
        addMetadata();
    }

    public QMOrgOrgType(Path<? extends QMOrgOrgType> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_ORG_ORG_TYPE");
        addMetadata();
    }

    public QMOrgOrgType(PathMetadata metadata) {
        super(QMOrgOrgType.class, metadata, "PUBLIC", "M_ORG_ORG_TYPE");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(orgOid, ColumnMetadata.named("ORG_OID").withIndex(1).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(orgtype, ColumnMetadata.named("ORGTYPE").withIndex(2).ofType(Types.VARCHAR).withSize(255));
    }

}

