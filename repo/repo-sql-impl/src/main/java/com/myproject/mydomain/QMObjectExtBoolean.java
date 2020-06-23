package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMObjectExtBoolean is a Querydsl query type for QMObjectExtBoolean
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMObjectExtBoolean extends com.querydsl.sql.RelationalPathBase<QMObjectExtBoolean> {

    private static final long serialVersionUID = -185483854;

    public static final QMObjectExtBoolean mObjectExtBoolean = new QMObjectExtBoolean("M_OBJECT_EXT_BOOLEAN");

    public final BooleanPath booleanvalue = createBoolean("booleanvalue");

    public final NumberPath<Integer> itemId = createNumber("itemId", Integer.class);

    public final StringPath ownerOid = createString("ownerOid");

    public final NumberPath<Integer> ownertype = createNumber("ownertype", Integer.class);

    public final com.querydsl.sql.PrimaryKey<QMObjectExtBoolean> constraint9 = createPrimaryKey(booleanvalue, itemId, ownertype, ownerOid);

    public final com.querydsl.sql.ForeignKey<QMObject> oExtBooleanOwnerFk = createForeignKey(ownerOid, "OID");

    public QMObjectExtBoolean(String variable) {
        super(QMObjectExtBoolean.class, forVariable(variable), "PUBLIC", "M_OBJECT_EXT_BOOLEAN");
        addMetadata();
    }

    public QMObjectExtBoolean(String variable, String schema, String table) {
        super(QMObjectExtBoolean.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMObjectExtBoolean(String variable, String schema) {
        super(QMObjectExtBoolean.class, forVariable(variable), schema, "M_OBJECT_EXT_BOOLEAN");
        addMetadata();
    }

    public QMObjectExtBoolean(Path<? extends QMObjectExtBoolean> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_OBJECT_EXT_BOOLEAN");
        addMetadata();
    }

    public QMObjectExtBoolean(PathMetadata metadata) {
        super(QMObjectExtBoolean.class, metadata, "PUBLIC", "M_OBJECT_EXT_BOOLEAN");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(booleanvalue, ColumnMetadata.named("BOOLEANVALUE").withIndex(4).ofType(Types.BOOLEAN).withSize(1).notNull());
        addMetadata(itemId, ColumnMetadata.named("ITEM_ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(ownerOid, ColumnMetadata.named("OWNER_OID").withIndex(2).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(ownertype, ColumnMetadata.named("OWNERTYPE").withIndex(3).ofType(Types.INTEGER).withSize(10).notNull());
    }

}

