package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMUserOrganizationalUnit is a Querydsl query type for QMUserOrganizationalUnit
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMUserOrganizationalUnit extends com.querydsl.sql.RelationalPathBase<QMUserOrganizationalUnit> {

    private static final long serialVersionUID = -722548807;

    public static final QMUserOrganizationalUnit mUserOrganizationalUnit = new QMUserOrganizationalUnit("M_USER_ORGANIZATIONAL_UNIT");

    public final StringPath norm = createString("norm");

    public final StringPath orig = createString("orig");

    public final StringPath userOid = createString("userOid");

    public final com.querydsl.sql.ForeignKey<QMUser> userOrgUnitFk = createForeignKey(userOid, "OID");

    public QMUserOrganizationalUnit(String variable) {
        super(QMUserOrganizationalUnit.class, forVariable(variable), "PUBLIC", "M_USER_ORGANIZATIONAL_UNIT");
        addMetadata();
    }

    public QMUserOrganizationalUnit(String variable, String schema, String table) {
        super(QMUserOrganizationalUnit.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMUserOrganizationalUnit(String variable, String schema) {
        super(QMUserOrganizationalUnit.class, forVariable(variable), schema, "M_USER_ORGANIZATIONAL_UNIT");
        addMetadata();
    }

    public QMUserOrganizationalUnit(Path<? extends QMUserOrganizationalUnit> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_USER_ORGANIZATIONAL_UNIT");
        addMetadata();
    }

    public QMUserOrganizationalUnit(PathMetadata metadata) {
        super(QMUserOrganizationalUnit.class, metadata, "PUBLIC", "M_USER_ORGANIZATIONAL_UNIT");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(norm, ColumnMetadata.named("NORM").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(orig, ColumnMetadata.named("ORIG").withIndex(3).ofType(Types.VARCHAR).withSize(255));
        addMetadata(userOid, ColumnMetadata.named("USER_OID").withIndex(1).ofType(Types.VARCHAR).withSize(36).notNull());
    }

}

