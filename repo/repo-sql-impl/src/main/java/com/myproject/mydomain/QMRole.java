package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMRole is a Querydsl query type for QMRole
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMRole extends com.querydsl.sql.RelationalPathBase<QMRole> {

    private static final long serialVersionUID = 2118422722;

    public static final QMRole mRole = new QMRole("M_ROLE");

    public final StringPath nameNorm = createString("nameNorm");

    public final StringPath nameOrig = createString("nameOrig");

    public final StringPath oid = createString("oid");

    public final StringPath roletype = createString("roletype");

    public final com.querydsl.sql.PrimaryKey<QMRole> constraint88c6 = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMAbstractRole> roleFk = createForeignKey(oid, "OID");

    public QMRole(String variable) {
        super(QMRole.class, forVariable(variable), "PUBLIC", "M_ROLE");
        addMetadata();
    }

    public QMRole(String variable, String schema, String table) {
        super(QMRole.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMRole(String variable, String schema) {
        super(QMRole.class, forVariable(variable), schema, "M_ROLE");
        addMetadata();
    }

    public QMRole(Path<? extends QMRole> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_ROLE");
        addMetadata();
    }

    public QMRole(PathMetadata metadata) {
        super(QMRole.class, metadata, "PUBLIC", "M_ROLE");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(nameNorm, ColumnMetadata.named("NAME_NORM").withIndex(1).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameOrig, ColumnMetadata.named("NAME_ORIG").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(4).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(roletype, ColumnMetadata.named("ROLETYPE").withIndex(3).ofType(Types.VARCHAR).withSize(255));
    }

}

