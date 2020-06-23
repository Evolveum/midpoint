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
 * QMAssignmentExtString is a Querydsl query type for QMAssignmentExtString
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMAssignmentExtString extends com.querydsl.sql.RelationalPathBase<QMAssignmentExtString> {

    private static final long serialVersionUID = 2130950009;

    public static final QMAssignmentExtString mAssignmentExtString = new QMAssignmentExtString("M_ASSIGNMENT_EXT_STRING");

    public final NumberPath<Integer> anycontainerOwnerId = createNumber("anycontainerOwnerId", Integer.class);

    public final StringPath anycontainerOwnerOwnerOid = createString("anycontainerOwnerOwnerOid");

    public final NumberPath<Integer> itemId = createNumber("itemId", Integer.class);

    public final StringPath stringvalue = createString("stringvalue");

    public final com.querydsl.sql.PrimaryKey<QMAssignmentExtString> constraintD = createPrimaryKey(anycontainerOwnerId, anycontainerOwnerOwnerOid, itemId, stringvalue);

    public final com.querydsl.sql.ForeignKey<QMAssignmentExtension> aExtStringOwnerFk = createForeignKey(Arrays.asList(anycontainerOwnerId, anycontainerOwnerOwnerOid), Arrays.asList("OWNER_ID", "OWNER_OWNER_OID"));

    public QMAssignmentExtString(String variable) {
        super(QMAssignmentExtString.class, forVariable(variable), "PUBLIC", "M_ASSIGNMENT_EXT_STRING");
        addMetadata();
    }

    public QMAssignmentExtString(String variable, String schema, String table) {
        super(QMAssignmentExtString.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMAssignmentExtString(String variable, String schema) {
        super(QMAssignmentExtString.class, forVariable(variable), schema, "M_ASSIGNMENT_EXT_STRING");
        addMetadata();
    }

    public QMAssignmentExtString(Path<? extends QMAssignmentExtString> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_ASSIGNMENT_EXT_STRING");
        addMetadata();
    }

    public QMAssignmentExtString(PathMetadata metadata) {
        super(QMAssignmentExtString.class, metadata, "PUBLIC", "M_ASSIGNMENT_EXT_STRING");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(anycontainerOwnerId, ColumnMetadata.named("ANYCONTAINER_OWNER_ID").withIndex(2).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(anycontainerOwnerOwnerOid, ColumnMetadata.named("ANYCONTAINER_OWNER_OWNER_OID").withIndex(3).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(itemId, ColumnMetadata.named("ITEM_ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(stringvalue, ColumnMetadata.named("STRINGVALUE").withIndex(4).ofType(Types.VARCHAR).withSize(255).notNull());
    }

}

