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
 * QMAssignmentExtLong is a Querydsl query type for QMAssignmentExtLong
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMAssignmentExtLong extends com.querydsl.sql.RelationalPathBase<QMAssignmentExtLong> {

    private static final long serialVersionUID = -882911260;

    public static final QMAssignmentExtLong mAssignmentExtLong = new QMAssignmentExtLong("M_ASSIGNMENT_EXT_LONG");

    public final NumberPath<Integer> anycontainerOwnerId = createNumber("anycontainerOwnerId", Integer.class);

    public final StringPath anycontainerOwnerOwnerOid = createString("anycontainerOwnerOwnerOid");

    public final NumberPath<Integer> itemId = createNumber("itemId", Integer.class);

    public final NumberPath<Long> longvalue = createNumber("longvalue", Long.class);

    public final com.querydsl.sql.PrimaryKey<QMAssignmentExtLong> constraint8b = createPrimaryKey(anycontainerOwnerId, anycontainerOwnerOwnerOid, itemId, longvalue);

    public final com.querydsl.sql.ForeignKey<QMAssignmentExtension> aExtLongOwnerFk = createForeignKey(Arrays.asList(anycontainerOwnerId, anycontainerOwnerOwnerOid), Arrays.asList("OWNER_ID", "OWNER_OWNER_OID"));

    public QMAssignmentExtLong(String variable) {
        super(QMAssignmentExtLong.class, forVariable(variable), "PUBLIC", "M_ASSIGNMENT_EXT_LONG");
        addMetadata();
    }

    public QMAssignmentExtLong(String variable, String schema, String table) {
        super(QMAssignmentExtLong.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMAssignmentExtLong(String variable, String schema) {
        super(QMAssignmentExtLong.class, forVariable(variable), schema, "M_ASSIGNMENT_EXT_LONG");
        addMetadata();
    }

    public QMAssignmentExtLong(Path<? extends QMAssignmentExtLong> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_ASSIGNMENT_EXT_LONG");
        addMetadata();
    }

    public QMAssignmentExtLong(PathMetadata metadata) {
        super(QMAssignmentExtLong.class, metadata, "PUBLIC", "M_ASSIGNMENT_EXT_LONG");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(anycontainerOwnerId, ColumnMetadata.named("ANYCONTAINER_OWNER_ID").withIndex(2).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(anycontainerOwnerOwnerOid, ColumnMetadata.named("ANYCONTAINER_OWNER_OWNER_OID").withIndex(3).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(itemId, ColumnMetadata.named("ITEM_ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(longvalue, ColumnMetadata.named("LONGVALUE").withIndex(4).ofType(Types.BIGINT).withSize(19).notNull());
    }

}

