package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMOrgClosure is a Querydsl query type for QMOrgClosure
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMOrgClosure extends com.querydsl.sql.RelationalPathBase<QMOrgClosure> {

    private static final long serialVersionUID = -475639293;

    public static final QMOrgClosure mOrgClosure = new QMOrgClosure("M_ORG_CLOSURE");

    public final StringPath ancestorOid = createString("ancestorOid");

    public final StringPath descendantOid = createString("descendantOid");

    public final NumberPath<Integer> val = createNumber("val", Integer.class);

    public final com.querydsl.sql.PrimaryKey<QMOrgClosure> constraintD4 = createPrimaryKey(ancestorOid, descendantOid);

    public final com.querydsl.sql.ForeignKey<QMObject> ancestorFk = createForeignKey(ancestorOid, "OID");

    public final com.querydsl.sql.ForeignKey<QMObject> descendantFk = createForeignKey(descendantOid, "OID");

    public QMOrgClosure(String variable) {
        super(QMOrgClosure.class, forVariable(variable), "PUBLIC", "M_ORG_CLOSURE");
        addMetadata();
    }

    public QMOrgClosure(String variable, String schema, String table) {
        super(QMOrgClosure.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMOrgClosure(String variable, String schema) {
        super(QMOrgClosure.class, forVariable(variable), schema, "M_ORG_CLOSURE");
        addMetadata();
    }

    public QMOrgClosure(Path<? extends QMOrgClosure> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_ORG_CLOSURE");
        addMetadata();
    }

    public QMOrgClosure(PathMetadata metadata) {
        super(QMOrgClosure.class, metadata, "PUBLIC", "M_ORG_CLOSURE");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(ancestorOid, ColumnMetadata.named("ANCESTOR_OID").withIndex(1).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(descendantOid, ColumnMetadata.named("DESCENDANT_OID").withIndex(2).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(val, ColumnMetadata.named("VAL").withIndex(3).ofType(Types.INTEGER).withSize(10));
    }

}

