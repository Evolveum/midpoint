package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMObjectExtString is a Querydsl query type for QMObjectExtString
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMObjectExtString extends com.querydsl.sql.RelationalPathBase<QMObjectExtString> {

    private static final long serialVersionUID = -2008435385;

    public static final QMObjectExtString mObjectExtString = new QMObjectExtString("M_OBJECT_EXT_STRING");

    public final NumberPath<Integer> itemId = createNumber("itemId", Integer.class);

    public final StringPath ownerOid = createString("ownerOid");

    public final NumberPath<Integer> ownertype = createNumber("ownertype", Integer.class);

    public final StringPath stringvalue = createString("stringvalue");

    public final com.querydsl.sql.PrimaryKey<QMObjectExtString> constraint11 = createPrimaryKey(itemId, ownertype, ownerOid, stringvalue);

    public final com.querydsl.sql.ForeignKey<QMObject> objectExtStringFk = createForeignKey(ownerOid, "OID");

    public QMObjectExtString(String variable) {
        super(QMObjectExtString.class, forVariable(variable), "PUBLIC", "M_OBJECT_EXT_STRING");
        addMetadata();
    }

    public QMObjectExtString(String variable, String schema, String table) {
        super(QMObjectExtString.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMObjectExtString(String variable, String schema) {
        super(QMObjectExtString.class, forVariable(variable), schema, "M_OBJECT_EXT_STRING");
        addMetadata();
    }

    public QMObjectExtString(Path<? extends QMObjectExtString> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_OBJECT_EXT_STRING");
        addMetadata();
    }

    public QMObjectExtString(PathMetadata metadata) {
        super(QMObjectExtString.class, metadata, "PUBLIC", "M_OBJECT_EXT_STRING");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(itemId, ColumnMetadata.named("ITEM_ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(ownerOid, ColumnMetadata.named("OWNER_OID").withIndex(2).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(ownertype, ColumnMetadata.named("OWNERTYPE").withIndex(3).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(stringvalue, ColumnMetadata.named("STRINGVALUE").withIndex(4).ofType(Types.VARCHAR).withSize(255).notNull());
    }

}

