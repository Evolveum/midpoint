package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMObjectExtDate is a Querydsl query type for QMObjectExtDate
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMObjectExtDate extends com.querydsl.sql.RelationalPathBase<QMObjectExtDate> {

    private static final long serialVersionUID = -503113116;

    public static final QMObjectExtDate mObjectExtDate = new QMObjectExtDate("M_OBJECT_EXT_DATE");

    public final DateTimePath<java.sql.Timestamp> datevalue = createDateTime("datevalue", java.sql.Timestamp.class);

    public final NumberPath<Integer> itemId = createNumber("itemId", Integer.class);

    public final StringPath ownerOid = createString("ownerOid");

    public final NumberPath<Integer> ownertype = createNumber("ownertype", Integer.class);

    public final com.querydsl.sql.PrimaryKey<QMObjectExtDate> constraintDb = createPrimaryKey(datevalue, itemId, ownertype, ownerOid);

    public final com.querydsl.sql.ForeignKey<QMObject> oExtDateOwnerFk = createForeignKey(ownerOid, "OID");

    public QMObjectExtDate(String variable) {
        super(QMObjectExtDate.class, forVariable(variable), "PUBLIC", "M_OBJECT_EXT_DATE");
        addMetadata();
    }

    public QMObjectExtDate(String variable, String schema, String table) {
        super(QMObjectExtDate.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMObjectExtDate(String variable, String schema) {
        super(QMObjectExtDate.class, forVariable(variable), schema, "M_OBJECT_EXT_DATE");
        addMetadata();
    }

    public QMObjectExtDate(Path<? extends QMObjectExtDate> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_OBJECT_EXT_DATE");
        addMetadata();
    }

    public QMObjectExtDate(PathMetadata metadata) {
        super(QMObjectExtDate.class, metadata, "PUBLIC", "M_OBJECT_EXT_DATE");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(datevalue, ColumnMetadata.named("DATEVALUE").withIndex(4).ofType(Types.TIMESTAMP).withSize(23).withDigits(10).notNull());
        addMetadata(itemId, ColumnMetadata.named("ITEM_ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(ownerOid, ColumnMetadata.named("OWNER_OID").withIndex(2).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(ownertype, ColumnMetadata.named("OWNERTYPE").withIndex(3).ofType(Types.INTEGER).withSize(10).notNull());
    }

}

