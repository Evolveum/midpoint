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
 * QMAssignmentExtDate is a Querydsl query type for QMAssignmentExtDate
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMAssignmentExtDate extends com.querydsl.sql.RelationalPathBase<QMAssignmentExtDate> {

    private static final long serialVersionUID = -883162858;

    public static final QMAssignmentExtDate mAssignmentExtDate = new QMAssignmentExtDate("M_ASSIGNMENT_EXT_DATE");

    public final NumberPath<Integer> anycontainerOwnerId = createNumber("anycontainerOwnerId", Integer.class);

    public final StringPath anycontainerOwnerOwnerOid = createString("anycontainerOwnerOwnerOid");

    public final DateTimePath<java.sql.Timestamp> datevalue = createDateTime("datevalue", java.sql.Timestamp.class);

    public final NumberPath<Integer> itemId = createNumber("itemId", Integer.class);

    public final com.querydsl.sql.PrimaryKey<QMAssignmentExtDate> constraint8 = createPrimaryKey(anycontainerOwnerId, anycontainerOwnerOwnerOid, datevalue, itemId);

    public final com.querydsl.sql.ForeignKey<QMAssignmentExtension> aExtDateOwnerFk = createForeignKey(Arrays.asList(anycontainerOwnerId, anycontainerOwnerOwnerOid), Arrays.asList("OWNER_ID", "OWNER_OWNER_OID"));

    public QMAssignmentExtDate(String variable) {
        super(QMAssignmentExtDate.class, forVariable(variable), "PUBLIC", "M_ASSIGNMENT_EXT_DATE");
        addMetadata();
    }

    public QMAssignmentExtDate(String variable, String schema, String table) {
        super(QMAssignmentExtDate.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMAssignmentExtDate(String variable, String schema) {
        super(QMAssignmentExtDate.class, forVariable(variable), schema, "M_ASSIGNMENT_EXT_DATE");
        addMetadata();
    }

    public QMAssignmentExtDate(Path<? extends QMAssignmentExtDate> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_ASSIGNMENT_EXT_DATE");
        addMetadata();
    }

    public QMAssignmentExtDate(PathMetadata metadata) {
        super(QMAssignmentExtDate.class, metadata, "PUBLIC", "M_ASSIGNMENT_EXT_DATE");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(anycontainerOwnerId, ColumnMetadata.named("ANYCONTAINER_OWNER_ID").withIndex(2).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(anycontainerOwnerOwnerOid, ColumnMetadata.named("ANYCONTAINER_OWNER_OWNER_OID").withIndex(3).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(datevalue, ColumnMetadata.named("DATEVALUE").withIndex(4).ofType(Types.TIMESTAMP).withSize(23).withDigits(10).notNull());
        addMetadata(itemId, ColumnMetadata.named("ITEM_ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
    }

}

