package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMReference is a Querydsl query type for QMReference
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMReference extends com.querydsl.sql.RelationalPathBase<QMReference> {

    private static final long serialVersionUID = -1956217217;

    public static final QMReference mReference = new QMReference("M_REFERENCE");

    public final StringPath ownerOid = createString("ownerOid");

    public final NumberPath<Integer> referenceType = createNumber("referenceType", Integer.class);

    public final StringPath relation = createString("relation");

    public final StringPath targetoid = createString("targetoid");

    public final NumberPath<Integer> targettype = createNumber("targettype", Integer.class);

    public final com.querydsl.sql.PrimaryKey<QMReference> constraintDe = createPrimaryKey(ownerOid, referenceType, relation, targetoid);

    public final com.querydsl.sql.ForeignKey<QMObject> referenceOwnerFk = createForeignKey(ownerOid, "OID");

    public QMReference(String variable) {
        super(QMReference.class, forVariable(variable), "PUBLIC", "M_REFERENCE");
        addMetadata();
    }

    public QMReference(String variable, String schema, String table) {
        super(QMReference.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMReference(String variable, String schema) {
        super(QMReference.class, forVariable(variable), schema, "M_REFERENCE");
        addMetadata();
    }

    public QMReference(Path<? extends QMReference> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_REFERENCE");
        addMetadata();
    }

    public QMReference(PathMetadata metadata) {
        super(QMReference.class, metadata, "PUBLIC", "M_REFERENCE");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(ownerOid, ColumnMetadata.named("OWNER_OID").withIndex(1).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(referenceType, ColumnMetadata.named("REFERENCE_TYPE").withIndex(2).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(relation, ColumnMetadata.named("RELATION").withIndex(3).ofType(Types.VARCHAR).withSize(157).notNull());
        addMetadata(targetoid, ColumnMetadata.named("TARGETOID").withIndex(4).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(targettype, ColumnMetadata.named("TARGETTYPE").withIndex(5).ofType(Types.INTEGER).withSize(10));
    }

}

