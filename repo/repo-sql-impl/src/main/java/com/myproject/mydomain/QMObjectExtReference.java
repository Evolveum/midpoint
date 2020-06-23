package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMObjectExtReference is a Querydsl query type for QMObjectExtReference
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMObjectExtReference extends com.querydsl.sql.RelationalPathBase<QMObjectExtReference> {

    private static final long serialVersionUID = -844933547;

    public static final QMObjectExtReference mObjectExtReference = new QMObjectExtReference("M_OBJECT_EXT_REFERENCE");

    public final NumberPath<Integer> itemId = createNumber("itemId", Integer.class);

    public final StringPath ownerOid = createString("ownerOid");

    public final NumberPath<Integer> ownertype = createNumber("ownertype", Integer.class);

    public final StringPath relation = createString("relation");

    public final StringPath targetoid = createString("targetoid");

    public final NumberPath<Integer> targettype = createNumber("targettype", Integer.class);

    public final com.querydsl.sql.PrimaryKey<QMObjectExtReference> constraint69 = createPrimaryKey(itemId, ownertype, ownerOid, targetoid);

    public final com.querydsl.sql.ForeignKey<QMObject> oExtReferenceOwnerFk = createForeignKey(ownerOid, "OID");

    public QMObjectExtReference(String variable) {
        super(QMObjectExtReference.class, forVariable(variable), "PUBLIC", "M_OBJECT_EXT_REFERENCE");
        addMetadata();
    }

    public QMObjectExtReference(String variable, String schema, String table) {
        super(QMObjectExtReference.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMObjectExtReference(String variable, String schema) {
        super(QMObjectExtReference.class, forVariable(variable), schema, "M_OBJECT_EXT_REFERENCE");
        addMetadata();
    }

    public QMObjectExtReference(Path<? extends QMObjectExtReference> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_OBJECT_EXT_REFERENCE");
        addMetadata();
    }

    public QMObjectExtReference(PathMetadata metadata) {
        super(QMObjectExtReference.class, metadata, "PUBLIC", "M_OBJECT_EXT_REFERENCE");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(itemId, ColumnMetadata.named("ITEM_ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(ownerOid, ColumnMetadata.named("OWNER_OID").withIndex(2).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(ownertype, ColumnMetadata.named("OWNERTYPE").withIndex(3).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(relation, ColumnMetadata.named("RELATION").withIndex(5).ofType(Types.VARCHAR).withSize(157));
        addMetadata(targetoid, ColumnMetadata.named("TARGETOID").withIndex(4).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(targettype, ColumnMetadata.named("TARGETTYPE").withIndex(6).ofType(Types.INTEGER).withSize(10));
    }

}

