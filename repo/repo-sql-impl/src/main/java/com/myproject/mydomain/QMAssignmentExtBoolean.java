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
 * QMAssignmentExtBoolean is a Querydsl query type for QMAssignmentExtBoolean
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMAssignmentExtBoolean extends com.querydsl.sql.RelationalPathBase<QMAssignmentExtBoolean> {

    private static final long serialVersionUID = -713555520;

    public static final QMAssignmentExtBoolean mAssignmentExtBoolean = new QMAssignmentExtBoolean("M_ASSIGNMENT_EXT_BOOLEAN");

    public final NumberPath<Integer> anycontainerOwnerId = createNumber("anycontainerOwnerId", Integer.class);

    public final StringPath anycontainerOwnerOwnerOid = createString("anycontainerOwnerOwnerOid");

    public final BooleanPath booleanvalue = createBoolean("booleanvalue");

    public final NumberPath<Integer> itemId = createNumber("itemId", Integer.class);

    public final com.querydsl.sql.PrimaryKey<QMAssignmentExtBoolean> constraintE6 = createPrimaryKey(anycontainerOwnerId, anycontainerOwnerOwnerOid, booleanvalue, itemId);

    public final com.querydsl.sql.ForeignKey<QMAssignmentExtension> aExtBooleanOwnerFk = createForeignKey(Arrays.asList(anycontainerOwnerId, anycontainerOwnerOwnerOid), Arrays.asList("OWNER_ID", "OWNER_OWNER_OID"));

    public QMAssignmentExtBoolean(String variable) {
        super(QMAssignmentExtBoolean.class, forVariable(variable), "PUBLIC", "M_ASSIGNMENT_EXT_BOOLEAN");
        addMetadata();
    }

    public QMAssignmentExtBoolean(String variable, String schema, String table) {
        super(QMAssignmentExtBoolean.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMAssignmentExtBoolean(String variable, String schema) {
        super(QMAssignmentExtBoolean.class, forVariable(variable), schema, "M_ASSIGNMENT_EXT_BOOLEAN");
        addMetadata();
    }

    public QMAssignmentExtBoolean(Path<? extends QMAssignmentExtBoolean> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_ASSIGNMENT_EXT_BOOLEAN");
        addMetadata();
    }

    public QMAssignmentExtBoolean(PathMetadata metadata) {
        super(QMAssignmentExtBoolean.class, metadata, "PUBLIC", "M_ASSIGNMENT_EXT_BOOLEAN");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(anycontainerOwnerId, ColumnMetadata.named("ANYCONTAINER_OWNER_ID").withIndex(2).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(anycontainerOwnerOwnerOid, ColumnMetadata.named("ANYCONTAINER_OWNER_OWNER_OID").withIndex(3).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(booleanvalue, ColumnMetadata.named("BOOLEANVALUE").withIndex(4).ofType(Types.BOOLEAN).withSize(1).notNull());
        addMetadata(itemId, ColumnMetadata.named("ITEM_ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
    }

}

