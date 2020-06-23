package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMObjectCollection is a Querydsl query type for QMObjectCollection
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMObjectCollection extends com.querydsl.sql.RelationalPathBase<QMObjectCollection> {

    private static final long serialVersionUID = 855468425;

    public static final QMObjectCollection mObjectCollection = new QMObjectCollection("M_OBJECT_COLLECTION");

    public final StringPath nameNorm = createString("nameNorm");

    public final StringPath nameOrig = createString("nameOrig");

    public final StringPath oid = createString("oid");

    public final com.querydsl.sql.PrimaryKey<QMObjectCollection> constraint9d = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMObject> objectCollectionFk = createForeignKey(oid, "OID");

    public QMObjectCollection(String variable) {
        super(QMObjectCollection.class, forVariable(variable), "PUBLIC", "M_OBJECT_COLLECTION");
        addMetadata();
    }

    public QMObjectCollection(String variable, String schema, String table) {
        super(QMObjectCollection.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMObjectCollection(String variable, String schema) {
        super(QMObjectCollection.class, forVariable(variable), schema, "M_OBJECT_COLLECTION");
        addMetadata();
    }

    public QMObjectCollection(Path<? extends QMObjectCollection> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_OBJECT_COLLECTION");
        addMetadata();
    }

    public QMObjectCollection(PathMetadata metadata) {
        super(QMObjectCollection.class, metadata, "PUBLIC", "M_OBJECT_COLLECTION");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(nameNorm, ColumnMetadata.named("NAME_NORM").withIndex(1).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameOrig, ColumnMetadata.named("NAME_ORIG").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(3).ofType(Types.VARCHAR).withSize(36).notNull());
    }

}

