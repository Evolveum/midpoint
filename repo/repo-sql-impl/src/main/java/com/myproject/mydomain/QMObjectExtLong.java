package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMObjectExtLong is a Querydsl query type for QMObjectExtLong
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMObjectExtLong extends com.querydsl.sql.RelationalPathBase<QMObjectExtLong> {

    private static final long serialVersionUID = -502861518;

    public static final QMObjectExtLong mObjectExtLong = new QMObjectExtLong("M_OBJECT_EXT_LONG");

    public final NumberPath<Integer> itemId = createNumber("itemId", Integer.class);

    public final NumberPath<Long> longvalue = createNumber("longvalue", Long.class);

    public final StringPath ownerOid = createString("ownerOid");

    public final NumberPath<Integer> ownertype = createNumber("ownertype", Integer.class);

    public final com.querydsl.sql.PrimaryKey<QMObjectExtLong> constraintDbc = createPrimaryKey(itemId, longvalue, ownertype, ownerOid);

    public final com.querydsl.sql.ForeignKey<QMObject> objectExtLongFk = createForeignKey(ownerOid, "OID");

    public QMObjectExtLong(String variable) {
        super(QMObjectExtLong.class, forVariable(variable), "PUBLIC", "M_OBJECT_EXT_LONG");
        addMetadata();
    }

    public QMObjectExtLong(String variable, String schema, String table) {
        super(QMObjectExtLong.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMObjectExtLong(String variable, String schema) {
        super(QMObjectExtLong.class, forVariable(variable), schema, "M_OBJECT_EXT_LONG");
        addMetadata();
    }

    public QMObjectExtLong(Path<? extends QMObjectExtLong> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_OBJECT_EXT_LONG");
        addMetadata();
    }

    public QMObjectExtLong(PathMetadata metadata) {
        super(QMObjectExtLong.class, metadata, "PUBLIC", "M_OBJECT_EXT_LONG");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(itemId, ColumnMetadata.named("ITEM_ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(longvalue, ColumnMetadata.named("LONGVALUE").withIndex(4).ofType(Types.BIGINT).withSize(19).notNull());
        addMetadata(ownerOid, ColumnMetadata.named("OWNER_OID").withIndex(2).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(ownertype, ColumnMetadata.named("OWNERTYPE").withIndex(3).ofType(Types.INTEGER).withSize(10).notNull());
    }

}

