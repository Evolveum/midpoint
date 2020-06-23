package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMObjectExtPoly is a Querydsl query type for QMObjectExtPoly
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMObjectExtPoly extends com.querydsl.sql.RelationalPathBase<QMObjectExtPoly> {

    private static final long serialVersionUID = -502742398;

    public static final QMObjectExtPoly mObjectExtPoly = new QMObjectExtPoly("M_OBJECT_EXT_POLY");

    public final NumberPath<Integer> itemId = createNumber("itemId", Integer.class);

    public final StringPath norm = createString("norm");

    public final StringPath orig = createString("orig");

    public final StringPath ownerOid = createString("ownerOid");

    public final NumberPath<Integer> ownertype = createNumber("ownertype", Integer.class);

    public final com.querydsl.sql.PrimaryKey<QMObjectExtPoly> constraintDbc4 = createPrimaryKey(itemId, orig, ownertype, ownerOid);

    public final com.querydsl.sql.ForeignKey<QMObject> oExtPolyOwnerFk = createForeignKey(ownerOid, "OID");

    public QMObjectExtPoly(String variable) {
        super(QMObjectExtPoly.class, forVariable(variable), "PUBLIC", "M_OBJECT_EXT_POLY");
        addMetadata();
    }

    public QMObjectExtPoly(String variable, String schema, String table) {
        super(QMObjectExtPoly.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMObjectExtPoly(String variable, String schema) {
        super(QMObjectExtPoly.class, forVariable(variable), schema, "M_OBJECT_EXT_POLY");
        addMetadata();
    }

    public QMObjectExtPoly(Path<? extends QMObjectExtPoly> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_OBJECT_EXT_POLY");
        addMetadata();
    }

    public QMObjectExtPoly(PathMetadata metadata) {
        super(QMObjectExtPoly.class, metadata, "PUBLIC", "M_OBJECT_EXT_POLY");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(itemId, ColumnMetadata.named("ITEM_ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(norm, ColumnMetadata.named("NORM").withIndex(5).ofType(Types.VARCHAR).withSize(255));
        addMetadata(orig, ColumnMetadata.named("ORIG").withIndex(4).ofType(Types.VARCHAR).withSize(255).notNull());
        addMetadata(ownerOid, ColumnMetadata.named("OWNER_OID").withIndex(2).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(ownertype, ColumnMetadata.named("OWNERTYPE").withIndex(3).ofType(Types.INTEGER).withSize(10).notNull());
    }

}

