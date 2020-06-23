package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import java.util.*;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMAssignmentExtReference is a Querydsl query type for QMAssignmentExtReference
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMAssignmentExtReference extends com.querydsl.sql.RelationalPathBase<QMAssignmentExtReference> {

    private static final long serialVersionUID = -1515663645;

    public static final QMAssignmentExtReference mAssignmentExtReference = new QMAssignmentExtReference("M_ASSIGNMENT_EXT_REFERENCE");

    public final NumberPath<Integer> anycontainerOwnerId = createNumber("anycontainerOwnerId", Integer.class);

    public final StringPath anycontainerOwnerOwnerOid = createString("anycontainerOwnerOwnerOid");

    public final NumberPath<Integer> itemId = createNumber("itemId", Integer.class);

    public final StringPath relation = createString("relation");

    public final StringPath targetoid = createString("targetoid");

    public final NumberPath<Integer> targettype = createNumber("targettype", Integer.class);

    public final com.querydsl.sql.PrimaryKey<QMAssignmentExtReference> constraint88 = createPrimaryKey(anycontainerOwnerId, anycontainerOwnerOwnerOid, itemId, targetoid);

    public final com.querydsl.sql.ForeignKey<QMAssignmentExtension> aExtReferenceOwnerFk = createForeignKey(Arrays.asList(anycontainerOwnerId, anycontainerOwnerOwnerOid), Arrays.asList("OWNER_ID", "OWNER_OWNER_OID"));

    public QMAssignmentExtReference(String variable) {
        super(QMAssignmentExtReference.class, forVariable(variable), "PUBLIC", "M_ASSIGNMENT_EXT_REFERENCE");
        addMetadata();
    }

    public QMAssignmentExtReference(String variable, String schema, String table) {
        super(QMAssignmentExtReference.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMAssignmentExtReference(String variable, String schema) {
        super(QMAssignmentExtReference.class, forVariable(variable), schema, "M_ASSIGNMENT_EXT_REFERENCE");
        addMetadata();
    }

    public QMAssignmentExtReference(Path<? extends QMAssignmentExtReference> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_ASSIGNMENT_EXT_REFERENCE");
        addMetadata();
    }

    public QMAssignmentExtReference(PathMetadata metadata) {
        super(QMAssignmentExtReference.class, metadata, "PUBLIC", "M_ASSIGNMENT_EXT_REFERENCE");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(anycontainerOwnerId, ColumnMetadata.named("ANYCONTAINER_OWNER_ID").withIndex(2).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(anycontainerOwnerOwnerOid, ColumnMetadata.named("ANYCONTAINER_OWNER_OWNER_OID").withIndex(3).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(itemId, ColumnMetadata.named("ITEM_ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(relation, ColumnMetadata.named("RELATION").withIndex(5).ofType(Types.VARCHAR).withSize(157));
        addMetadata(targetoid, ColumnMetadata.named("TARGETOID").withIndex(4).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(targettype, ColumnMetadata.named("TARGETTYPE").withIndex(6).ofType(Types.INTEGER).withSize(10));
    }

}

