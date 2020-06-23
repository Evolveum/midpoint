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
 * QMAssignmentExtension is a Querydsl query type for QMAssignmentExtension
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMAssignmentExtension extends com.querydsl.sql.RelationalPathBase<QMAssignmentExtension> {

    private static final long serialVersionUID = -1654203866;

    public static final QMAssignmentExtension mAssignmentExtension = new QMAssignmentExtension("M_ASSIGNMENT_EXTENSION");

    public final NumberPath<Integer> ownerId = createNumber("ownerId", Integer.class);

    public final StringPath ownerOwnerOid = createString("ownerOwnerOid");

    public final com.querydsl.sql.PrimaryKey<QMAssignmentExtension> constraintE2 = createPrimaryKey(ownerId, ownerOwnerOid);

    public final com.querydsl.sql.ForeignKey<QMAssignmentExtString> _aExtStringOwnerFk = createInvForeignKey(Arrays.asList(ownerId, ownerOwnerOid), Arrays.asList("ANYCONTAINER_OWNER_ID", "ANYCONTAINER_OWNER_OWNER_OID"));

    public final com.querydsl.sql.ForeignKey<QMAssignmentExtDate> _aExtDateOwnerFk = createInvForeignKey(Arrays.asList(ownerId, ownerOwnerOid), Arrays.asList("ANYCONTAINER_OWNER_ID", "ANYCONTAINER_OWNER_OWNER_OID"));

    public final com.querydsl.sql.ForeignKey<QMAssignmentExtPoly> _aExtPolyOwnerFk = createInvForeignKey(Arrays.asList(ownerId, ownerOwnerOid), Arrays.asList("ANYCONTAINER_OWNER_ID", "ANYCONTAINER_OWNER_OWNER_OID"));

    public final com.querydsl.sql.ForeignKey<QMAssignmentExtReference> _aExtReferenceOwnerFk = createInvForeignKey(Arrays.asList(ownerId, ownerOwnerOid), Arrays.asList("ANYCONTAINER_OWNER_ID", "ANYCONTAINER_OWNER_OWNER_OID"));

    public final com.querydsl.sql.ForeignKey<QMAssignmentExtBoolean> _aExtBooleanOwnerFk = createInvForeignKey(Arrays.asList(ownerId, ownerOwnerOid), Arrays.asList("ANYCONTAINER_OWNER_ID", "ANYCONTAINER_OWNER_OWNER_OID"));

    public final com.querydsl.sql.ForeignKey<QMAssignmentExtLong> _aExtLongOwnerFk = createInvForeignKey(Arrays.asList(ownerId, ownerOwnerOid), Arrays.asList("ANYCONTAINER_OWNER_ID", "ANYCONTAINER_OWNER_OWNER_OID"));

    public QMAssignmentExtension(String variable) {
        super(QMAssignmentExtension.class, forVariable(variable), "PUBLIC", "M_ASSIGNMENT_EXTENSION");
        addMetadata();
    }

    public QMAssignmentExtension(String variable, String schema, String table) {
        super(QMAssignmentExtension.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMAssignmentExtension(String variable, String schema) {
        super(QMAssignmentExtension.class, forVariable(variable), schema, "M_ASSIGNMENT_EXTENSION");
        addMetadata();
    }

    public QMAssignmentExtension(Path<? extends QMAssignmentExtension> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_ASSIGNMENT_EXTENSION");
        addMetadata();
    }

    public QMAssignmentExtension(PathMetadata metadata) {
        super(QMAssignmentExtension.class, metadata, "PUBLIC", "M_ASSIGNMENT_EXTENSION");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(ownerId, ColumnMetadata.named("OWNER_ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(ownerOwnerOid, ColumnMetadata.named("OWNER_OWNER_OID").withIndex(2).ofType(Types.VARCHAR).withSize(36).notNull());
    }

}

