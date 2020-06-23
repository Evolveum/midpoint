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
 * QMAssignmentExtPoly is a Querydsl query type for QMAssignmentExtPoly
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMAssignmentExtPoly extends com.querydsl.sql.RelationalPathBase<QMAssignmentExtPoly> {

    private static final long serialVersionUID = -882792140;

    public static final QMAssignmentExtPoly mAssignmentExtPoly = new QMAssignmentExtPoly("M_ASSIGNMENT_EXT_POLY");

    public final NumberPath<Integer> anycontainerOwnerId = createNumber("anycontainerOwnerId", Integer.class);

    public final StringPath anycontainerOwnerOwnerOid = createString("anycontainerOwnerOwnerOid");

    public final NumberPath<Integer> itemId = createNumber("itemId", Integer.class);

    public final StringPath norm = createString("norm");

    public final StringPath orig = createString("orig");

    public final com.querydsl.sql.PrimaryKey<QMAssignmentExtPoly> constraint8bc = createPrimaryKey(anycontainerOwnerId, anycontainerOwnerOwnerOid, itemId, orig);

    public final com.querydsl.sql.ForeignKey<QMAssignmentExtension> aExtPolyOwnerFk = createForeignKey(Arrays.asList(anycontainerOwnerId, anycontainerOwnerOwnerOid), Arrays.asList("OWNER_ID", "OWNER_OWNER_OID"));

    public QMAssignmentExtPoly(String variable) {
        super(QMAssignmentExtPoly.class, forVariable(variable), "PUBLIC", "M_ASSIGNMENT_EXT_POLY");
        addMetadata();
    }

    public QMAssignmentExtPoly(String variable, String schema, String table) {
        super(QMAssignmentExtPoly.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMAssignmentExtPoly(String variable, String schema) {
        super(QMAssignmentExtPoly.class, forVariable(variable), schema, "M_ASSIGNMENT_EXT_POLY");
        addMetadata();
    }

    public QMAssignmentExtPoly(Path<? extends QMAssignmentExtPoly> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_ASSIGNMENT_EXT_POLY");
        addMetadata();
    }

    public QMAssignmentExtPoly(PathMetadata metadata) {
        super(QMAssignmentExtPoly.class, metadata, "PUBLIC", "M_ASSIGNMENT_EXT_POLY");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(anycontainerOwnerId, ColumnMetadata.named("ANYCONTAINER_OWNER_ID").withIndex(2).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(anycontainerOwnerOwnerOid, ColumnMetadata.named("ANYCONTAINER_OWNER_OWNER_OID").withIndex(3).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(itemId, ColumnMetadata.named("ITEM_ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(norm, ColumnMetadata.named("NORM").withIndex(5).ofType(Types.VARCHAR).withSize(255));
        addMetadata(orig, ColumnMetadata.named("ORIG").withIndex(4).ofType(Types.VARCHAR).withSize(255).notNull());
    }

}

