package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMUserOrganization is a Querydsl query type for QMUserOrganization
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMUserOrganization extends com.querydsl.sql.RelationalPathBase<QMUserOrganization> {

    private static final long serialVersionUID = -150875414;

    public static final QMUserOrganization mUserOrganization = new QMUserOrganization("M_USER_ORGANIZATION");

    public final StringPath norm = createString("norm");

    public final StringPath orig = createString("orig");

    public final StringPath userOid = createString("userOid");

    public final com.querydsl.sql.ForeignKey<QMUser> userOrganizationFk = createForeignKey(userOid, "OID");

    public QMUserOrganization(String variable) {
        super(QMUserOrganization.class, forVariable(variable), "PUBLIC", "M_USER_ORGANIZATION");
        addMetadata();
    }

    public QMUserOrganization(String variable, String schema, String table) {
        super(QMUserOrganization.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMUserOrganization(String variable, String schema) {
        super(QMUserOrganization.class, forVariable(variable), schema, "M_USER_ORGANIZATION");
        addMetadata();
    }

    public QMUserOrganization(Path<? extends QMUserOrganization> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_USER_ORGANIZATION");
        addMetadata();
    }

    public QMUserOrganization(PathMetadata metadata) {
        super(QMUserOrganization.class, metadata, "PUBLIC", "M_USER_ORGANIZATION");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(norm, ColumnMetadata.named("NORM").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(orig, ColumnMetadata.named("ORIG").withIndex(3).ofType(Types.VARCHAR).withSize(255));
        addMetadata(userOid, ColumnMetadata.named("USER_OID").withIndex(1).ofType(Types.VARCHAR).withSize(36).notNull());
    }

}

