package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMLookupTableRow is a Querydsl query type for QMLookupTableRow
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMLookupTableRow extends com.querydsl.sql.RelationalPathBase<QMLookupTableRow> {

    private static final long serialVersionUID = -1906647022;

    public static final QMLookupTableRow mLookupTableRow = new QMLookupTableRow("M_LOOKUP_TABLE_ROW");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final StringPath labelNorm = createString("labelNorm");

    public final StringPath labelOrig = createString("labelOrig");

    public final DateTimePath<java.sql.Timestamp> lastchangetimestamp = createDateTime("lastchangetimestamp", java.sql.Timestamp.class);

    public final StringPath ownerOid = createString("ownerOid");

    public final StringPath rowKey = createString("rowKey");

    public final StringPath rowValue = createString("rowValue");

    public final com.querydsl.sql.PrimaryKey<QMLookupTableRow> constraint41 = createPrimaryKey(id, ownerOid);

    public final com.querydsl.sql.ForeignKey<QMLookupTable> lookupTableOwnerFk = createForeignKey(ownerOid, "OID");

    public QMLookupTableRow(String variable) {
        super(QMLookupTableRow.class, forVariable(variable), "PUBLIC", "M_LOOKUP_TABLE_ROW");
        addMetadata();
    }

    public QMLookupTableRow(String variable, String schema, String table) {
        super(QMLookupTableRow.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMLookupTableRow(String variable, String schema) {
        super(QMLookupTableRow.class, forVariable(variable), schema, "M_LOOKUP_TABLE_ROW");
        addMetadata();
    }

    public QMLookupTableRow(Path<? extends QMLookupTableRow> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_LOOKUP_TABLE_ROW");
        addMetadata();
    }

    public QMLookupTableRow(PathMetadata metadata) {
        super(QMLookupTableRow.class, metadata, "PUBLIC", "M_LOOKUP_TABLE_ROW");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(id, ColumnMetadata.named("ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(labelNorm, ColumnMetadata.named("LABEL_NORM").withIndex(4).ofType(Types.VARCHAR).withSize(255));
        addMetadata(labelOrig, ColumnMetadata.named("LABEL_ORIG").withIndex(5).ofType(Types.VARCHAR).withSize(255));
        addMetadata(lastchangetimestamp, ColumnMetadata.named("LASTCHANGETIMESTAMP").withIndex(6).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(ownerOid, ColumnMetadata.named("OWNER_OID").withIndex(2).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(rowKey, ColumnMetadata.named("ROW_KEY").withIndex(3).ofType(Types.VARCHAR).withSize(255));
        addMetadata(rowValue, ColumnMetadata.named("ROW_VALUE").withIndex(7).ofType(Types.VARCHAR).withSize(255));
    }

}

