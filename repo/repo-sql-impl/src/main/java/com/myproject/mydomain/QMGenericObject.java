package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMGenericObject is a Querydsl query type for QMGenericObject
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMGenericObject extends com.querydsl.sql.RelationalPathBase<QMGenericObject> {

    private static final long serialVersionUID = -33545942;

    public static final QMGenericObject mGenericObject = new QMGenericObject("M_GENERIC_OBJECT");

    public final StringPath nameNorm = createString("nameNorm");

    public final StringPath nameOrig = createString("nameOrig");

    public final StringPath objecttype = createString("objecttype");

    public final StringPath oid = createString("oid");

    public final com.querydsl.sql.PrimaryKey<QMGenericObject> constraint7b = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMFocus> genericObjectFk = createForeignKey(oid, "OID");

    public QMGenericObject(String variable) {
        super(QMGenericObject.class, forVariable(variable), "PUBLIC", "M_GENERIC_OBJECT");
        addMetadata();
    }

    public QMGenericObject(String variable, String schema, String table) {
        super(QMGenericObject.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMGenericObject(String variable, String schema) {
        super(QMGenericObject.class, forVariable(variable), schema, "M_GENERIC_OBJECT");
        addMetadata();
    }

    public QMGenericObject(Path<? extends QMGenericObject> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_GENERIC_OBJECT");
        addMetadata();
    }

    public QMGenericObject(PathMetadata metadata) {
        super(QMGenericObject.class, metadata, "PUBLIC", "M_GENERIC_OBJECT");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(nameNorm, ColumnMetadata.named("NAME_NORM").withIndex(1).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameOrig, ColumnMetadata.named("NAME_ORIG").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(objecttype, ColumnMetadata.named("OBJECTTYPE").withIndex(3).ofType(Types.VARCHAR).withSize(255));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(4).ofType(Types.VARCHAR).withSize(36).notNull());
    }

}

