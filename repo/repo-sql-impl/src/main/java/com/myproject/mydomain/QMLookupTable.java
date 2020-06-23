package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMLookupTable is a Querydsl query type for QMLookupTable
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMLookupTable extends com.querydsl.sql.RelationalPathBase<QMLookupTable> {

    private static final long serialVersionUID = -1359298392;

    public static final QMLookupTable mLookupTable = new QMLookupTable("M_LOOKUP_TABLE");

    public final StringPath nameNorm = createString("nameNorm");

    public final StringPath nameOrig = createString("nameOrig");

    public final StringPath oid = createString("oid");

    public final com.querydsl.sql.PrimaryKey<QMLookupTable> constraint2ce = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMObject> lookupTableFk = createForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMLookupTableRow> _lookupTableOwnerFk = createInvForeignKey(oid, "OWNER_OID");

    public QMLookupTable(String variable) {
        super(QMLookupTable.class, forVariable(variable), "PUBLIC", "M_LOOKUP_TABLE");
        addMetadata();
    }

    public QMLookupTable(String variable, String schema, String table) {
        super(QMLookupTable.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMLookupTable(String variable, String schema) {
        super(QMLookupTable.class, forVariable(variable), schema, "M_LOOKUP_TABLE");
        addMetadata();
    }

    public QMLookupTable(Path<? extends QMLookupTable> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_LOOKUP_TABLE");
        addMetadata();
    }

    public QMLookupTable(PathMetadata metadata) {
        super(QMLookupTable.class, metadata, "PUBLIC", "M_LOOKUP_TABLE");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(nameNorm, ColumnMetadata.named("NAME_NORM").withIndex(1).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameOrig, ColumnMetadata.named("NAME_ORIG").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(3).ofType(Types.VARCHAR).withSize(36).notNull());
    }

}

