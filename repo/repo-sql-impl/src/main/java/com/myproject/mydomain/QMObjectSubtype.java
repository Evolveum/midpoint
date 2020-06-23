package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMObjectSubtype is a Querydsl query type for QMObjectSubtype
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMObjectSubtype extends com.querydsl.sql.RelationalPathBase<QMObjectSubtype> {

    private static final long serialVersionUID = -1064021393;

    public static final QMObjectSubtype mObjectSubtype = new QMObjectSubtype("M_OBJECT_SUBTYPE");

    public final StringPath objectOid = createString("objectOid");

    public final StringPath subtype = createString("subtype");

    public final com.querydsl.sql.ForeignKey<QMObject> objectSubtypeFk = createForeignKey(objectOid, "OID");

    public QMObjectSubtype(String variable) {
        super(QMObjectSubtype.class, forVariable(variable), "PUBLIC", "M_OBJECT_SUBTYPE");
        addMetadata();
    }

    public QMObjectSubtype(String variable, String schema, String table) {
        super(QMObjectSubtype.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMObjectSubtype(String variable, String schema) {
        super(QMObjectSubtype.class, forVariable(variable), schema, "M_OBJECT_SUBTYPE");
        addMetadata();
    }

    public QMObjectSubtype(Path<? extends QMObjectSubtype> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_OBJECT_SUBTYPE");
        addMetadata();
    }

    public QMObjectSubtype(PathMetadata metadata) {
        super(QMObjectSubtype.class, metadata, "PUBLIC", "M_OBJECT_SUBTYPE");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(objectOid, ColumnMetadata.named("OBJECT_OID").withIndex(1).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(subtype, ColumnMetadata.named("SUBTYPE").withIndex(2).ofType(Types.VARCHAR).withSize(255));
    }

}

