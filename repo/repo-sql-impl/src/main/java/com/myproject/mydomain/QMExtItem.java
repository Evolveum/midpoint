package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMExtItem is a Querydsl query type for QMExtItem
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMExtItem extends com.querydsl.sql.RelationalPathBase<QMExtItem> {

    private static final long serialVersionUID = 1293548808;

    public static final QMExtItem mExtItem = new QMExtItem("M_EXT_ITEM");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final StringPath itemname = createString("itemname");

    public final StringPath itemtype = createString("itemtype");

    public final NumberPath<Integer> kind = createNumber("kind", Integer.class);

    public final com.querydsl.sql.PrimaryKey<QMExtItem> constraint43 = createPrimaryKey(id);

    public QMExtItem(String variable) {
        super(QMExtItem.class, forVariable(variable), "PUBLIC", "M_EXT_ITEM");
        addMetadata();
    }

    public QMExtItem(String variable, String schema, String table) {
        super(QMExtItem.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMExtItem(String variable, String schema) {
        super(QMExtItem.class, forVariable(variable), schema, "M_EXT_ITEM");
        addMetadata();
    }

    public QMExtItem(Path<? extends QMExtItem> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_EXT_ITEM");
        addMetadata();
    }

    public QMExtItem(PathMetadata metadata) {
        super(QMExtItem.class, metadata, "PUBLIC", "M_EXT_ITEM");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(id, ColumnMetadata.named("ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(itemname, ColumnMetadata.named("ITEMNAME").withIndex(3).ofType(Types.VARCHAR).withSize(157));
        addMetadata(itemtype, ColumnMetadata.named("ITEMTYPE").withIndex(4).ofType(Types.VARCHAR).withSize(157));
        addMetadata(kind, ColumnMetadata.named("KIND").withIndex(2).ofType(Types.INTEGER).withSize(10));
    }

}

